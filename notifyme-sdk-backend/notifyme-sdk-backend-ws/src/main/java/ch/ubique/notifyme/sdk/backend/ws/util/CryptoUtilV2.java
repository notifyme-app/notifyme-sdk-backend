package ch.ubique.notifyme.sdk.backend.ws.util;

import ch.ubique.notifyme.sdk.backend.model.PreTraceWithProofOuterClass;
import ch.ubique.notifyme.sdk.backend.model.QrCodeContentOuterClass;
import ch.ubique.notifyme.sdk.backend.model.tracekey.v2.TraceKey;
import com.google.protobuf.InvalidProtocolBufferException;
import com.goterl.lazycode.lazysodium.SodiumJava;
import com.goterl.lazycode.lazysodium.interfaces.Box;
import com.herumi.mcl.G1;
import com.herumi.mcl.G2;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;

public class CryptoUtilV2 extends CryptoUtil{

  public CryptoUtilV2(String skHex, String pkHex, SodiumJava sodium) {
    super(skHex, pkHex, sodium);
  }

    public byte[] encryptMessage(byte[] secretKey, byte[] nonce, String message) {
        var messageBytes = message.getBytes();
        var encryptedMessage = new byte[messageBytes.length + Box.MACBYTES];
        sodium.randombytes_buf(nonce, nonce.length);
        sodium.crypto_secretbox_easy(
                encryptedMessage, messageBytes, messageBytes.length, nonce, secretKey);
        return encryptedMessage;
    }

    public void calculateSecretKeyForIdentityAndIdentity(
            PreTraceWithProofOuterClass.PreTraceWithProof preTraceWithProof, int affectedHour, TraceKey traceKey)
            throws InvalidProtocolBufferException {
        var preTrace = preTraceWithProof.getPreTrace();
        PreTraceWithProofOuterClass.TraceProof proof = preTraceWithProof.getProof();

        var ctxha = preTrace.getCipherTextHealthAuthority().toByteArray();
        G1 secretKeyForIdentity =
                getSecretKeyForIdentity(
                        ctxha,
                        preTrace.getIdentity().toByteArray(),
                        preTrace.getPartialSecretKeyForIdentityOfLocation().toByteArray());
        var qrCodeContent = QrCodeContentOuterClass.QrCodeContent.parseFrom(preTraceWithProof.getInfo());

        byte[] identity =
                generateIdentity(
                        qrCodeContent,
                        proof.getNonce1().toByteArray(),
                        proof.getNonce2().toByteArray(),
                        affectedHour);

        if (!Arrays.equals(preTrace.getIdentity().toByteArray(), identity)) {
            throw new RuntimeException("Computed identity does not match given identity");
        }

        traceKey.setIdentity(identity);
        traceKey.setSecretKeyForIdentity(secretKeyForIdentity.serialize());
    }

    private byte[] generateIdentity(
            QrCodeContentOuterClass.QrCodeContent qrCodeContent, byte[] nonce1, byte[] nonce2, int hour) {
        byte[] hash1 = cryptoHashSHA256(ArrayUtils.addAll(qrCodeContent.toByteArray(), nonce1));
        return cryptoHashSHA256(
                ArrayUtils.addAll(hash1, ArrayUtils.addAll(nonce2, String.valueOf(hour).getBytes())));
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
