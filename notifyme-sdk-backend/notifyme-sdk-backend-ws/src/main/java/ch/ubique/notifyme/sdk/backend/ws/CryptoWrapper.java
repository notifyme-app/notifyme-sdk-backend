package ch.ubique.notifyme.sdk.backend.ws;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Arrays;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.crypto.tink.subtle.Hkdf;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.goterl.lazycode.lazysodium.SodiumJava;
import com.goterl.lazycode.lazysodium.interfaces.Box;
import com.goterl.lazycode.lazysodium.utils.LibraryLoadingException;
import com.herumi.mcl.Fr;
import com.herumi.mcl.G1;
import com.herumi.mcl.G2;
import com.herumi.mcl.GT;
import com.herumi.mcl.Mcl;
import com.sun.jna.Native;
import com.sun.jna.Platform;

import ch.ubique.notifyme.sdk.backend.model.PreTraceWithProofOuterClass.PreTrace;
import ch.ubique.notifyme.sdk.backend.model.PreTraceWithProofOuterClass.PreTraceWithProof;
import ch.ubique.notifyme.sdk.backend.model.PreTraceWithProofOuterClass.TraceProof;
import ch.ubique.notifyme.sdk.backend.model.QrCodeContentOuterClass.QrCodeContent;
import ch.ubique.notifyme.sdk.backend.model.tracekey.v2.TraceKey;
import ch.ubique.notifyme.sdk.backend.model.v3.AssociatedDataOuterClass.AssociatedData;
import ch.ubique.notifyme.sdk.backend.model.v3.QrCodePayload.QRCodePayload;

public class CryptoWrapper {

    private static final Logger logger = LoggerFactory.getLogger(CryptoWrapper.class);

    private static final int HASH_BYTES = 32;
    private static final int NONCE_BYTES = 32;
    private static final int QR_CODE_VERSION_3 = 3;

    public final byte[] sk;
    public final byte[] pk;
    private final SodiumJava sodium;

    public CryptoWrapper(String skHex, String pkHex) {
        try {
            this.sk = Hex.decodeHex(skHex);
        } catch (DecoderException e) {
            logger.error("unable to parse sk hexstring", e);
            throw new RuntimeException(e);
        }
        try {
            this.pk = Hex.decodeHex(pkHex);
        } catch (DecoderException e) {
            logger.error("unable to parse pk hexstring", e);
            throw new RuntimeException(e);
        }
        // Do custom loading for the libsodium lib, as it does not work out of the box
        // with spring boot bundled jars. To get a path to the full file, we copy
        // libsodium to a tmpfile and give that absolute path to lazysodium
        try {
            InputStream in = getClass().getClassLoader().getResourceAsStream("libsodium/" + getSodiumPathInResources());
            File libTmpFile = File.createTempFile("libsodium", null);
            Files.copy(in, libTmpFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            in.close();
            this.sodium = new SodiumJava(libTmpFile.getAbsolutePath());
        } catch (Exception e) {
            logger.error("unable to load libsodium", e);
            throw new RuntimeException(e);
        }

        // Do custom loading for the mcl lib, as it does not work out of the box
        // with spring boot bundled jars. To get a path to the full file, we copy
        // libmcl to a tmpfile and load that path
        try {
            InputStream in = getClass().getClassLoader().getResourceAsStream("libmcl/" + getMclPathInResources());
            File libTmpFile = File.createTempFile("libmcl", null);
            Files.copy(in, libTmpFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            in.close();
            System.load(libTmpFile.getAbsolutePath());
            Mcl.SystemInit(Mcl.BLS12_381);
        } catch (Exception e) {
            logger.error("unable to load libmcl", e);
            throw new RuntimeException(e);
        }
    }

    public SodiumJava getSodium() {
        return sodium;
    }

    public TraceKey calculateSecretKeyForIdentityAndIdentity(PreTraceWithProof preTraceWithProof, int affectedHour,
                    TraceKey traceKey) throws InvalidProtocolBufferException {
        PreTrace preTrace = preTraceWithProof.getPreTrace();
        TraceProof proof = preTraceWithProof.getProof();

        byte[] ctxha = preTrace.getCipherTextHealthAuthority().toByteArray();
        byte[] mskhRaw = new byte[ctxha.length - Box.SEALBYTES];
        int result = sodium.crypto_box_seal_open(mskhRaw, ctxha, ctxha.length, pk, sk);
        if (result != 0) {
            throw new RuntimeException("crypto_box_seal_open returned a value != 0");
        }
        Fr mskh = new Fr();
        mskh.deserialize(mskhRaw);

        G1 partialSecretKeyForIdentityOfHealthAuthority = keyDer(mskh, preTrace.getIdentity().toByteArray());
        G1 partialSecretKeyForIdentityOfLocation = new G1();
        partialSecretKeyForIdentityOfLocation
                        .deserialize(preTrace.getPartialSecretKeyForIdentityOfLocation().toByteArray());
        G1 secretKeyForIdentity = new G1();

        Mcl.add(secretKeyForIdentity, partialSecretKeyForIdentityOfLocation,
                        partialSecretKeyForIdentityOfHealthAuthority);
        QrCodeContent qrCodeContent = QrCodeContent.parseFrom(preTraceWithProof.getInfo());

        byte[] identity = generateIdentity(qrCodeContent, proof.getNonce1().toByteArray(),
                        proof.getNonce2().toByteArray(), affectedHour);

        if (!Arrays.equals(preTrace.getIdentity().toByteArray(), identity)) {
            throw new RuntimeException("Computed identity does not match given identity");
        }

        traceKey.setIdentity(identity);
        traceKey.setSecretKeyForIdentity(secretKeyForIdentity.serialize());

        return traceKey;
    }

    public ch.ubique.notifyme.sdk.backend.model.tracekey.v3.TraceKey createTraceV3(
                    ch.ubique.notifyme.sdk.backend.model.v3.PreTraceWithProofOuterClass.PreTraceWithProof preTraceWithProof,
                    String message, byte[] countryData) throws InvalidProtocolBufferException {

        ch.ubique.notifyme.sdk.backend.model.v3.PreTraceWithProofOuterClass.PreTrace preTrace = preTraceWithProof
                        .getPreTrace();
        ch.ubique.notifyme.sdk.backend.model.v3.PreTraceWithProofOuterClass.TraceProof proof = preTraceWithProof
                        .getProof();

        byte[] ctxha = preTrace.getCipherTextHealthAuthority().toByteArray();
        byte[] mskh_raw = new byte[ctxha.length - Box.SEALBYTES];
        int result = sodium.crypto_box_seal_open(mskh_raw, ctxha, ctxha.length, pk, sk);
        if (result != 0) {
            throw new RuntimeException("crypto_box_seal_open returned a value != 0");
        }

        Fr mskh = new Fr();
        mskh.deserialize(mskh_raw);

        G1 partialSecretKeyForIdentityOfHealthAuthority = keyDer(mskh, preTrace.getIdentity().toByteArray());
        G1 partialSecretKeyForIdentityOfLocation = new G1();
        partialSecretKeyForIdentityOfLocation
                        .deserialize(preTrace.getPartialSecretKeyForIdentityOfLocation().toByteArray());

        G1 secretKeyForIdentity = new G1();
        Mcl.add(secretKeyForIdentity, partialSecretKeyForIdentityOfLocation,
                        partialSecretKeyForIdentityOfHealthAuthority);

        QRCodePayload qrCodePayload = QRCodePayload.parseFrom(preTraceWithProof.getQrCodePayload());
        byte[] identity = generateIdentityV3(qrCodePayload, preTraceWithProof.getStartOfInterval());
        if (!Arrays.equals(preTrace.getIdentity().toByteArray(), identity)) {
            return null;
        }

        // verifyTrace
        int NONCE_LENGTH = 32;
        byte[] msg_orig = getRandomValue(NONCE_LENGTH);
        G2 masterPublicKey = new G2();
        masterPublicKey.deserialize(proof.getMasterPublicKey().toByteArray());
        IBECiphertext ibeCiphertext = encryptInternal(masterPublicKey, identity, msg_orig);
        byte[] msg_dec = decryptInternal(ibeCiphertext, secretKeyForIdentity, identity);
        if (msg_dec == null) {
            throw new RuntimeException("Health Authority could not verify Trace");
        }

        byte[] nonce = getRandomValue(Box.NONCEBYTES);
        byte[] encryptedAssociatedData = encryptAssociatedData(
                        preTraceWithProof.getPreTrace().getNotificationKey().toByteArray(), message, countryData,
                        nonce);

        ch.ubique.notifyme.sdk.backend.model.tracekey.v3.TraceKey traceKey = new ch.ubique.notifyme.sdk.backend.model.tracekey.v3.TraceKey();
        traceKey.setIdentity(preTrace.getIdentity().toByteArray());
        traceKey.setCipherTextNonce(nonce);
        traceKey.setStartTime(Instant.ofEpochSecond(preTraceWithProof.getStartTime()));
        traceKey.setEndTime(Instant.ofEpochSecond(preTraceWithProof.getEndTime()));
        traceKey.setCipherTextNonce(nonce);
        traceKey.setEncryptedAssociatedData(encryptedAssociatedData);
        traceKey.setSecretKeyForIdentity(secretKeyForIdentity.serialize());
        return traceKey;
    }

    private G1 keyDer(Fr msk, byte[] identity) {
        G1 g1_temp = new G1();
        Mcl.hashAndMapToG1(g1_temp, identity);

        G1 result = new G1();
        Mcl.mul(result, g1_temp, msk);
        return result;
    }

    private byte[] generateIdentity(QrCodeContent qrCodeContent, byte[] nonce1, byte[] nonce2, int hour) {
        byte[] hash1 = cryptoHashSHA256(ArrayUtils.addAll(qrCodeContent.toByteArray(), nonce1));
        return cryptoHashSHA256(ArrayUtils.addAll(hash1, ArrayUtils.addAll(nonce2, String.valueOf(hour).getBytes())));
    }

    private byte[] generateIdentityV3(QRCodePayload qrCodePayload, long startOfInterval) {
        return generateIdentityV3(qrCodePayload.toByteArray(), startOfInterval);
    }

    private byte[] generateIdentityV3(byte[] qrCodePayload, long startOfInterval) {
        NoncesAndNotificationKey cryptoData = getNoncesAndNotificationKey(qrCodePayload);
        byte[] preid = cryptoHashSHA256(
                        concatenate("CN-PREID".getBytes(StandardCharsets.US_ASCII), qrCodePayload, cryptoData.nonce1));

        return cryptoHashSHA256(concatenate("CN-ID".getBytes(StandardCharsets.US_ASCII), preid, intToBytes(3600),
                        longToBytes(startOfInterval), cryptoData.nonce2));
    }

    private byte[] cryptoHashSHA256(byte[] in) {
        byte[] out = new byte[HASH_BYTES];
        int result = sodium.crypto_hash_sha256(out, in, in.length);
        if (result != 0) {
            throw new RuntimeException("crypto_hash_sha256 returned a value != 0");
        }
        return out;
    }

    public NoncesAndNotificationKey getNoncesAndNotificationKey(QRCodePayload qrCodePayload) {
        return getNoncesAndNotificationKey(qrCodePayload.toByteArray());
    }

    public NoncesAndNotificationKey getNoncesAndNotificationKey(byte[] qrCodePayload) {
        try {
            byte[] hkdfOutput = Hkdf.computeHkdf("HMACSHA256", qrCodePayload, new byte[0],
                            "CrowdNotifier_v3".getBytes(StandardCharsets.US_ASCII), 96);
            byte[] nonce1 = Arrays.copyOfRange(hkdfOutput, 0, 32);
            byte[] nonce2 = Arrays.copyOfRange(hkdfOutput, 32, 64);
            byte[] notificationKey = Arrays.copyOfRange(hkdfOutput, 64, 96);
            return new NoncesAndNotificationKey(nonce1, nonce2, notificationKey);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("HKDF threw GeneralSecurityException");
        }
    }

    private byte[] longToBytes(long l) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(8);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        byteBuffer.putLong(l);
        return byteBuffer.array();
    }

    private byte[] intToBytes(int i) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(4);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        byteBuffer.putInt(i);
        return byteBuffer.array();
    }

    public byte[] createNonceForMessageEncytion() {
        byte[] nonce = new byte[Box.NONCEBYTES];
        sodium.randombytes_buf(nonce, nonce.length);
        return nonce;
    }

    private byte[] getRandomValue(int bytes) {
        byte[] nonce = new byte[bytes];
        sodium.randombytes_buf(nonce, nonce.length);
        return nonce;
    }

    public byte[] encryptMessage(byte[] secretKey, byte[] nonce, String message) {
        byte[] messageBytes = message.getBytes();
        byte[] encryptedMessage = new byte[messageBytes.length + Box.MACBYTES];
        sodium.randombytes_buf(nonce, nonce.length);
        sodium.crypto_secretbox_easy(encryptedMessage, messageBytes, messageBytes.length, nonce, secretKey);
        return encryptedMessage;
    }

    public IBECiphertext encryptInternal(G2 masterPublicKey, byte[] identity, byte[] message) {

        byte[] x = randomBytesBuf32();

        Fr r = new Fr();
        r.setHashOf(concatenate(x, identity, message));

        G2 c1 = new G2();
        Mcl.mul(c1, baseG2(), r);

        G1 g1_temp = new G1();
        Mcl.hashAndMapToG1(g1_temp, identity);

        GT gt1_temp = new GT();
        Mcl.pairing(gt1_temp, g1_temp, masterPublicKey);

        GT gt_temp = new GT();
        Mcl.pow(gt_temp, gt1_temp, r);

        byte[] c2_pair = cryptoHashSHA256(gt_temp.serialize());
        byte[] c2 = xor(x, c2_pair);

        byte[] nonce = createNonceForMessageEncytion();

        byte[] c3 = cryptoSecretboxEasy(cryptoHashSHA256(x), message, nonce);

        return new IBECiphertext(c1.serialize(), c2, c3, nonce);
    }
    
    private byte[] randomBytesBuf32() {
        byte[] nonce = new byte[NONCE_BYTES];
        sodium.randombytes_buf(nonce, NONCE_BYTES);
        return nonce;
    }

    public byte[] decryptInternal(IBECiphertext ibeCiphertext, G1 secretKeyForIdentity, byte[] identity) {
        G2 c1 = new G2();
        c1.deserialize(ibeCiphertext.getC1());

        GT gt_temp = new GT();
        Mcl.pairing(gt_temp, secretKeyForIdentity, c1);

        byte[] hash = cryptoHashSHA256(gt_temp.serialize());
        byte[] x_p = xor(ibeCiphertext.getC2(), hash);

        byte[] msg_p = new byte[ibeCiphertext.getC3().length - Box.MACBYTES];
        int result = sodium.crypto_secretbox_open_easy(msg_p, ibeCiphertext.getC3(), ibeCiphertext.getC3().length,
                        ibeCiphertext.getNonce(), cryptoHashSHA256(x_p));
        if (result != 0)
            return null;

        // Additional verification
        Fr r_p = new Fr();
        r_p.setHashOf(concatenate(x_p, identity, msg_p));

        G2 c1_p = new G2();
        Mcl.mul(c1_p, baseG2(), r_p);

        if (!c1.equals(c1_p)) {
            return null;
        }

        if (!secretKeyForIdentity.isValidOrder() || secretKeyForIdentity.isZero()) {
            return null;
        }
        return msg_p;
    }

    private byte[] concatenate(byte[]... byteArrays) {
        try {
            byte[] result = new byte[0];
            for (byte[] byteArray : byteArrays) {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream(result.length + byteArray.length);
                outputStream.write(result);
                outputStream.write(byteArray);
                result = outputStream.toByteArray();
            }
            return result;
        } catch (IOException e) {
            throw new RuntimeException("Byte array concatenation failed");
        }
    }

    private byte[] encryptAssociatedData(byte[] secretKey, String message, byte[] countryData, byte[] nonce) {
        AssociatedData associatedData = AssociatedData.newBuilder().setMessage(message)
                        .setCountryData(ByteString.copyFrom(countryData)).setVersion(QR_CODE_VERSION_3).build();

        byte[] messageBytes = associatedData.toByteArray();
        byte[] encrytpedMessage = new byte[messageBytes.length + Box.MACBYTES];
        sodium.crypto_secretbox_easy(encrytpedMessage, messageBytes, messageBytes.length, nonce, secretKey);
        return encrytpedMessage;
    }

    private byte[] cryptoSecretboxEasy(byte[] secretKey, byte[] message, byte[] nonce) {
        byte[] encryptedMessage = new byte[message.length + Box.MACBYTES];
        int result = sodium.crypto_secretbox_easy(encryptedMessage, message, message.length, nonce, secretKey);
        if (result != 0) {
            throw new RuntimeException("crypto_secretbox_easy returned a value != 0");
        }
        return encryptedMessage;
    }

    private byte[] xor(byte[] a, byte[] b) {
        if (a.length != b.length)
            throw new RuntimeException("Cannot xor two byte arrays of different length");
        byte[] c = new byte[a.length];
        for (int i = 0; i < a.length; i++) {
            c[i] = (byte) (a[i] ^ b[i]);
        }
        return c;
    }

    private G2 baseG2() {
        G2 baseG2 = new G2();
        baseG2.setStr("1 3527010695874666181871391160110601448900299527927752"
                        + "40219908644239793785735715026873347600343865175952761926303160 "
                        + "305914434424421370997125981475378163698647032547664755865937320"
                        + "6291635324768958432433509563104347017837885763365758 "
                        + "198515060228729193556805452117717163830086897821565573085937866"
                        + "5066344726373823718423869104263333984641494340347905 "
                        + "927553665492332455747201965776037880757740193453592970025027978"
                        + "793976877002675564980949289727957565575433344219582");
        return baseG2;
    }

    /**
     * Returns the absolute path to sodium library inside JAR (beginning with '/'),
     * e.g. /linux/libsodium.so.
     */
    private static String getSodiumPathInResources() {
        boolean is64Bit = Native.POINTER_SIZE == 8;
        if (Platform.isWindows()) {
            if (is64Bit) {
                return getPath("windows64", "libsodium.dll");
            } else {
                return getPath("windows", "libsodium.dll");
            }
        }
        if (Platform.isARM()) {
            return getPath("armv6", "libsodium.so");
        }
        if (Platform.isLinux()) {
            if (is64Bit) {
                return getPath("linux64", "libsodium.so");
            } else {
                return getPath("linux", "libsodium.so");
            }
        }
        if (Platform.isMac()) {
            return getPath("mac", "libsodium.dylib");
        }

        String message = String.format("Unsupported platform: %s/%s", System.getProperty("os.name"),
                        System.getProperty("os.arch"));
        throw new LibraryLoadingException(message);
    }

    /**
     * Returns the absolute path to sodium library inside JAR (beginning with '/'),
     * e.g. /linux/libmcljava.so.
     */
    private static String getMclPathInResources() {
        boolean is64Bit = Native.POINTER_SIZE == 8;
        if (Platform.isWindows()) {
            if (is64Bit) {
                throw new UnsupportedOperationException("windows64 not supported");
            } else {
                throw new UnsupportedOperationException("windows not supported");

            }
        }
        if (Platform.isARM()) {
            throw new UnsupportedOperationException("arm not supported");
        }
        if (Platform.isLinux()) {
            if (is64Bit) {
                return getPath("linux64", "libmcljava.so");
            } else {
                throw new UnsupportedOperationException("linux32 not supported");
            }
        }
        if (Platform.isMac()) {
            return getPath("mac", "libmcljava.dylib");
        }

        String message = String.format("Unsupported platform: %s/%s", System.getProperty("os.name"),
                        System.getProperty("os.arch"));
        throw new UnsupportedOperationException(message);
    }

    private static String getPath(String folder, String name) {
        return folder + File.separator + name;
    }

    public class NoncesAndNotificationKey {
        public final byte[] nonce1;
        public final byte[] nonce2;
        public final byte[] notificationKey;

        public NoncesAndNotificationKey(byte[] nonce1, byte[] nonce2, byte[] notificationKey) {
            this.nonce1 = nonce1;
            this.nonce2 = nonce2;
            this.notificationKey = notificationKey;
        }
    }

    public class IBECiphertext {
        private byte[] c1;
        private byte[] c2;
        private byte[] c3;
        private byte[] nonce;

        public IBECiphertext(byte[] c1, byte[] c2, byte[] c3, byte[] nonce) {
            this.c1 = c1;
            this.c2 = c2;
            this.c3 = c3;
            this.nonce = nonce;
        }

        public byte[] getC1() {
            return c1;
        }

        public byte[] getC2() {
            return c2;
        }

        public byte[] getC3() {
            return c3;
        }

        public byte[] getNonce() {
            return nonce;
        }

    }
}
