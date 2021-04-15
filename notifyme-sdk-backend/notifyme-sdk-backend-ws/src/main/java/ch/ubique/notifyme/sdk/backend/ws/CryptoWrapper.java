package ch.ubique.notifyme.sdk.backend.ws;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.InvalidProtocolBufferException;
import com.goterl.lazycode.lazysodium.SodiumJava;
import com.goterl.lazycode.lazysodium.interfaces.Box;
import com.goterl.lazycode.lazysodium.utils.LibraryLoadingException;
import com.herumi.mcl.Fr;
import com.herumi.mcl.G1;
import com.herumi.mcl.Mcl;
import com.sun.jna.Native;
import com.sun.jna.Platform;

import ch.ubique.notifyme.sdk.backend.model.PreTraceWithProofOuterClass.PreTrace;
import ch.ubique.notifyme.sdk.backend.model.PreTraceWithProofOuterClass.PreTraceWithProof;
import ch.ubique.notifyme.sdk.backend.model.PreTraceWithProofOuterClass.TraceProof;
import ch.ubique.notifyme.sdk.backend.model.QrCodeContentOuterClass.QrCodeContent;
import ch.ubique.notifyme.sdk.backend.model.tracekey.v2.TraceKey;

public class CryptoWrapper {

    private static final Logger logger = LoggerFactory.getLogger(CryptoWrapper.class);

    private static final int HASH_BYTES = 32;

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

    private byte[] cryptoHashSHA256(byte[] in) {
        byte[] out = new byte[HASH_BYTES];
        int result = sodium.crypto_hash_sha256(out, in, in.length);
        if (result != 0) {
            throw new RuntimeException("crypto_hash_sha256 returned a value != 0");
        }
        return out;
    }

    public byte[] createNonceForMessageEncytion() {
        byte[] nonce = new byte[Box.NONCEBYTES];
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
}
