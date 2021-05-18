package ch.ubique.notifyme.sdk.backend.ws.util;

import com.goterl.lazycode.lazysodium.SodiumJava;
import com.goterl.lazycode.lazysodium.interfaces.Box;
import com.herumi.mcl.Fr;
import com.herumi.mcl.G1;
import com.herumi.mcl.G2;
import com.herumi.mcl.Mcl;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Base64;

public abstract class CryptoUtil {

    private static final Logger logger = LoggerFactory.getLogger(CryptoUtil.class);

    static final int HASH_BYTES = 32;
    static final int NONCE_BYTES = 32;
    static final int QR_CODE_VERSION_3 = 3;
    final SodiumJava sodium;

    private final byte[] sk;
    private final byte[] pk;

    public CryptoUtil(String skHex, String pkHex, SodiumJava sodium) {
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
        this.sodium = sodium;
    }

    public byte[] getSk() {
        return sk.clone();
    }

    public byte[] getPk() {
        return pk.clone();
    }

    public void genKeys() throws UnsupportedEncodingException {
        Fr msk = new Fr();
        msk.setByCSPRNG();
        G2 mpk = new G2();
        Mcl.mul(mpk, baseG2(), msk);
        System.out.println("MSK: " + Arrays.toString(msk.serialize()));
        System.out.println("MSK: " + Hex.encodeHexString(msk.serialize()));
        System.out.println("MSK: " + toBase64(msk.serialize()));

        System.out.println("MPK: " + Arrays.toString(mpk.serialize()));
        System.out.println("MPK: " + Hex.encodeHexString(mpk.serialize()));
        System.out.println("MPK: " + toBase64(mpk.serialize()));
    }

    public static String toBase64(byte[] bytes) throws UnsupportedEncodingException {
        return Base64.getUrlEncoder().encodeToString(bytes);
    }

    public static byte[] fromBase64(String base64) throws UnsupportedEncodingException {
        return Base64.getUrlDecoder().decode(base64.getBytes("UTF-8"));
    }

    abstract public G2 baseG2();

    public G1 keyDer(Fr msk, byte[] identity) {
        G1 g1_temp = new G1();
        Mcl.hashAndMapToG1(g1_temp, identity);

        G1 result = new G1();
        Mcl.mul(result, g1_temp, msk);
        return result;
    }

    public G1 getSecretKeyForIdentity(byte[] ctxha, byte[] identity, byte[] pskidL) {
        byte[] mskhRaw = new byte[ctxha.length - Box.SEALBYTES];
        int result = sodium.crypto_box_seal_open(mskhRaw, ctxha, ctxha.length, pk, sk);
        if (result != 0) {
            throw new RuntimeException("crypto_box_seal_open returned a value != 0");
        }
        Fr mskh = new Fr();
        mskh.deserialize(mskhRaw);

        G1 partialSecretKeyForIdentityOfHealthAuthority = keyDer(mskh, identity);
        G1 partialSecretKeyForIdentityOfLocation = new G1();
        partialSecretKeyForIdentityOfLocation.deserialize(pskidL);
        G1 secretKeyForIdentity = new G1();

        Mcl.add(
                secretKeyForIdentity,
                partialSecretKeyForIdentityOfLocation,
                partialSecretKeyForIdentityOfHealthAuthority);
        return secretKeyForIdentity;
    }

    public byte[] cryptoHashSHA256(byte[] in) {
        byte[] out = new byte[HASH_BYTES];
        int result = sodium.crypto_hash_sha256(out, in, in.length);
        if (result != 0) {
            throw new RuntimeException("crypto_hash_sha256 returned a value != 0");
        }
        return out;
    }

    /** Generates a random nonce of length Box.NONCEBYTES (at time of writing this, 24 bytes) */
    public byte[] createNonce() {
        return createNonce(Box.NONCEBYTES);
    }

    public byte[] createNonce(int bytes) {
        byte[] nonce = new byte[bytes];
        sodium.randombytes_buf(nonce, nonce.length);
        return nonce;
    }

    public class NoncesAndNotificationKey {
        public final byte[] noncePreId;
        public final byte[] nonceTimekey;
        public final byte[] notificationKey;

        public NoncesAndNotificationKey(
                byte[] noncePreId, byte[] nonceTimekey, byte[] notificationKey) {
            this.noncePreId = noncePreId;
            this.nonceTimekey = nonceTimekey;
            this.notificationKey = notificationKey;
        }
    }

    public class IBECiphertext {
        private final byte[] c1;
        private final byte[] c2;
        private final byte[] c3;
        private final byte[] nonce;

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
