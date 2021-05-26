package ch.ubique.swisscovid.cn.sdk.backend.ws.util;

import ch.ubique.swisscovid.cn.sdk.backend.model.UserUploadPayloadOuterClass;
import ch.ubique.swisscovid.cn.sdk.backend.model.tracekey.v3.TraceKey;
import ch.ubique.swisscovid.cn.sdk.backend.model.v3.AssociatedDataOuterClass;
import ch.ubique.swisscovid.cn.sdk.backend.model.v3.NotifyMeAssociatedDataOuterClass;
import ch.ubique.swisscovid.cn.sdk.backend.model.v3.QrCodePayload;
import com.google.crypto.tink.subtle.Hkdf;
import com.google.protobuf.ByteString;
import com.goterl.lazycode.lazysodium.SodiumJava;
import com.goterl.lazycode.lazysodium.interfaces.Box;
import com.herumi.mcl.Fr;
import com.herumi.mcl.G1;
import com.herumi.mcl.G2;
import com.herumi.mcl.GT;
import com.herumi.mcl.Mcl;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CryptoUtilV3 extends CryptoUtil {

    private static final Logger logger = LoggerFactory.getLogger(CryptoUtilV3.class);

    private final byte[] msk;
    private final byte[] mpk;

    private final Fr mskFr;
    private final G2 mpkG2;

    public CryptoUtilV3(
            String skHex, String pkHex, String mskHex, String mpkHex, SodiumJava sodium) {
        super(skHex, pkHex, sodium);
        try {
            this.msk = Hex.decodeHex(mskHex);
            this.mskFr = new Fr();
            this.mskFr.deserialize(this.msk);
        } catch (DecoderException e) {
            logger.error("unable to parse sk hexstring", e);
            throw new RuntimeException(e);
        }
        try {
            this.mpk = Hex.decodeHex(mpkHex);
            this.mpkG2 = new G2();
            this.mpkG2.deserialize(this.mpk);
        } catch (DecoderException e) {
            logger.error("unable to parse pk hexstring", e);
            throw new RuntimeException(e);
        }
    }

    public G2 getMpkG2() {
        return mpkG2;
    }

    public void testFlow() {
        Fr msk = new Fr();
        msk.setByCSPRNG();
        G2 mpk = new G2();
        Mcl.mul(mpk, baseG2(), msk);

        byte[] identity = createNonce(32);
        G1 secretKeyForIdentity = keyDer(this.mskFr, identity);

        // verifyTrace
        int NONCE_LENGTH = 32;
        byte[] msg_orig = createNonce(NONCE_LENGTH);
        IBECiphertext ibeCiphertext = encryptInternal(this.mpkG2, identity, msg_orig);
        byte[] msg_dec = decryptInternal(ibeCiphertext, secretKeyForIdentity, identity);
        if (msg_dec == null) {
            throw new RuntimeException("Health Authority could not verify Trace");
        }
    }

    public List<TraceKey> createTraceV3ForUserUpload(
            List<UserUploadPayloadOuterClass.UploadVenueInfo> uploadVenueInfoList) {
        var traceKeys = new ArrayList<TraceKey>();
        for (UserUploadPayloadOuterClass.UploadVenueInfo venueInfo : uploadVenueInfoList) {
            if (!venueInfo.getFake()) {
                final var startInSeconds = venueInfo.getIntervalStartMs() / 1000;
                byte[] identity =
                        cryptoHashSHA256(
                                concatenate(
                                        "CN-ID".getBytes(StandardCharsets.US_ASCII),
                                        venueInfo.getPreId().toByteArray(),
                                        intToBytes(3600),
                                        // Round down to nearest hour
                                        longToBytes(startInSeconds - startInSeconds % 3600),
                                        venueInfo.getTimeKey().toByteArray()));

                G1 secretKeyForIdentity = keyDer(this.mskFr, identity);

                byte[] nonce = createNonce();
                NotifyMeAssociatedDataOuterClass.NotifyMeAssociatedData countryData =
                        NotifyMeAssociatedDataOuterClass.NotifyMeAssociatedData.newBuilder()
                                .setCriticality(
                                        NotifyMeAssociatedDataOuterClass.EventCriticality.LOW)
                                .setVersion(4)
                                .build();
                byte[] encryptedAssociatedData =
                        this.encryptAssociatedData(
                                venueInfo.getNotificationKey().toByteArray(),
                                nonce,
                                "",
                                countryData.toByteArray(),
                                venueInfo.getIntervalStartMs(),
                                venueInfo.getIntervalEndMs());

                var traceKeyV3 = new TraceKey();
                traceKeyV3.setVersion(4);
                traceKeyV3.setCipherTextNonce(nonce);
                traceKeyV3.setEncryptedAssociatedData(encryptedAssociatedData);
                traceKeyV3.setDay(
                        Instant.ofEpochMilli(venueInfo.getIntervalStartMs())
                                .truncatedTo(ChronoUnit.DAYS));
                traceKeyV3.setIdentity(identity);
                traceKeyV3.setSecretKeyForIdentity(secretKeyForIdentity.serialize());
                traceKeys.add(traceKeyV3);

                // verifyTrace
                int NONCE_LENGTH = 32;
                byte[] msg_orig = createNonce(NONCE_LENGTH);
                IBECiphertext ibeCiphertext = encryptInternal(this.mpkG2, identity, msg_orig);
                byte[] msg_dec = decryptInternal(ibeCiphertext, secretKeyForIdentity, identity);
                if (msg_dec == null || !Arrays.equals(msg_orig, msg_dec)) {
                    throw new RuntimeException("Health Authority could not verify Trace");
                }
            }
        }

        return traceKeys;
    }

    public NoncesAndNotificationKey getNoncesAndNotificationKey(
            QrCodePayload.QRCodePayload qrCodePayload) {
        return getNoncesAndNotificationKey(qrCodePayload.toByteArray());
    }

    public NoncesAndNotificationKey getNoncesAndNotificationKey(byte[] qrCodePayload) {
        try {
            byte[] hkdfOutput =
                    Hkdf.computeHkdf(
                            "HMACSHA256",
                            qrCodePayload,
                            new byte[0],
                            "CrowdNotifier_v3".getBytes(StandardCharsets.US_ASCII),
                            96);
            byte[] noncePreId = Arrays.copyOfRange(hkdfOutput, 0, 32);
            byte[] nonceTimekey = Arrays.copyOfRange(hkdfOutput, 32, 64);
            byte[] notificationKey = Arrays.copyOfRange(hkdfOutput, 64, 96);
            return new NoncesAndNotificationKey(noncePreId, nonceTimekey, notificationKey);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("HKDF threw GeneralSecurityException");
        }
    }

    public IBECiphertext encryptInternal(G2 masterPublicKey, byte[] identity, byte[] message) {

        byte[] x = createNonce(NONCE_BYTES);

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

        byte[] nonce = createNonce();

        byte[] c3 = cryptoSecretboxEasy(cryptoHashSHA256(x), message, nonce);

        return new IBECiphertext(c1.serialize(), c2, c3, nonce);
    }

    public byte[] decryptInternal(
            IBECiphertext ibeCiphertext, G1 secretKeyForIdentity, byte[] identity) {
        G2 c1 = new G2();
        c1.deserialize(ibeCiphertext.getC1());

        GT gt_temp = new GT();
        Mcl.pairing(gt_temp, secretKeyForIdentity, c1);

        byte[] hash = cryptoHashSHA256(gt_temp.serialize());
        byte[] x_p = xor(ibeCiphertext.getC2(), hash);

        byte[] msg_p = new byte[ibeCiphertext.getC3().length - Box.MACBYTES];
        int result =
                sodium.crypto_secretbox_open_easy(
                        msg_p,
                        ibeCiphertext.getC3(),
                        ibeCiphertext.getC3().length,
                        ibeCiphertext.getNonce(),
                        cryptoHashSHA256(x_p));
        if (result != 0) return null;

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

    public byte[] cryptoSecretboxOpenEasy(byte[] key, byte[] cipherText, byte[] nonce) {
        final var decryptedMessage = new byte[cipherText.length - Box.MACBYTES];
        int result =
                sodium.crypto_secretbox_open_easy(
                        decryptedMessage, cipherText, cipherText.length, nonce, key);
        if (result != 0) return new byte[0];
        return decryptedMessage;
    }

    public byte[] encryptAssociatedData(
            byte[] secretKey,
            byte[] nonce,
            String message,
            byte[] countryData,
            long startTimestampMs,
            long endTimestampMs) {
        final var associatedData =
                AssociatedDataOuterClass.AssociatedData.newBuilder()
                        .setMessage(message)
                        .setStartTimestamp(startTimestampMs / 1000)
                        .setEndTimestamp(endTimestampMs / 1000)
                        .setCountryData(ByteString.copyFrom(countryData))
                        .setVersion(QR_CODE_VERSION_4)
                        .build();

        byte[] messageBytes = associatedData.toByteArray();
        return cryptoSecretboxEasy(secretKey, messageBytes, nonce);
    }

    public byte[] longToBytes(long l) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(8);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        byteBuffer.putLong(l);
        return byteBuffer.array();
    }

    public byte[] intToBytes(int i) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(4);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        byteBuffer.putInt(i);
        return byteBuffer.array();
    }

    public byte[] concatenate(byte[]... byteArrays) {
        try {
            byte[] result = new byte[0];
            for (byte[] byteArray : byteArrays) {
                ByteArrayOutputStream outputStream =
                        new ByteArrayOutputStream(result.length + byteArray.length);
                outputStream.write(result);
                outputStream.write(byteArray);
                result = outputStream.toByteArray();
            }
            return result;
        } catch (IOException e) {
            throw new RuntimeException("Byte array concatenation failed");
        }
    }

    private byte[] cryptoSecretboxEasy(byte[] secretKey, byte[] message, byte[] nonce) {
        byte[] encryptedMessage = new byte[message.length + Box.MACBYTES];
        int result =
                sodium.crypto_secretbox_easy(
                        encryptedMessage, message, message.length, nonce, secretKey);
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

    public G2 baseG2() {
        G2 baseG2 = new G2();
        baseG2.setStr(
                "1 3527010695874666181871391160110601448900299527927752"
                        + "40219908644239793785735715026873347600343865175952761926303160 "
                        + "305914434424421370997125981475378163698647032547664755865937320"
                        + "6291635324768958432433509563104347017837885763365758 "
                        + "198515060228729193556805452117717163830086897821565573085937866"
                        + "5066344726373823718423869104263333984641494340347905 "
                        + "927553665492332455747201965776037880757740193453592970025027978"
                        + "793976877002675564980949289727957565575433344219582");
        return baseG2;
    }
}
