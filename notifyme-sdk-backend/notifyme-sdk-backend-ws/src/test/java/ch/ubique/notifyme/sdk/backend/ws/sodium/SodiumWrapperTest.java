package ch.ubique.notifyme.sdk.backend.ws.sodium;

import static org.junit.Assert.assertEquals;

import ch.ubique.notifyme.sdk.backend.model.SeedMessageOuterClass;
import ch.ubique.notifyme.sdk.backend.model.SeedMessageOuterClass.SeedMessage;
import ch.ubique.notifyme.sdk.backend.ws.SodiumWrapper;
import com.google.protobuf.InvalidProtocolBufferException;
import com.goterl.lazycode.lazysodium.SodiumJava;
import com.goterl.lazycode.lazysodium.interfaces.Box;
import java.io.UnsupportedEncodingException;
import java.util.Base64;
import org.junit.Test;

public class SodiumWrapperTest {

    @Test
    public void test() throws UnsupportedEncodingException, InvalidProtocolBufferException {
        SodiumWrapper sodiumWrapper =
                new SodiumWrapper(
                        "36b3b80a1cd2cc98d84b4ed2c109b74e7026f00c0d40a0b12a936b1814aa5e39",
                        "e4d2e06641730ce7c9986b1e7e91bf41bb3b8cc1d76d249fa99d0d8925e87a5c");
        SodiumJava sodium = sodiumWrapper.getSodium();

        String ctx =
                "i1uxLdj9sE6rajaZ8ULZYZ-2fTzK6dYmoTxJRCfLOz1TY9Egc4nfnF2bGAD3P5nFfvHWhsvKR9vr5318LKp0stEmnkjY_N8wf93_And1t_2UqFFFI3XNT9vUUfDVkZ7Zv_YYvnOCEldMWS4_1WBVcNdfvPP3bSJhXxTOkPWqGbdsHf2YSvFk";

        String message = "This is the secret message";
        byte[] ctxBytes = Base64.getUrlDecoder().decode(ctx.getBytes("UTF-8"));
        byte[] seedBytes = sodiumWrapper.decryptQrTrace(ctxBytes);
        byte[] secretKey = sodiumWrapper.deriveSecretKeyFromSeed(seedBytes, ctxBytes);
        SeedMessage seed = SeedMessageOuterClass.SeedMessage.parseFrom(seedBytes);
        byte[] nonce = sodiumWrapper.createNonceForMessageEncytion();
        byte[] encryptedMessage =
                sodiumWrapper.encryptMessage(
                        seed.getNotificationKey().toByteArray(), nonce, message);

        byte[] decryptedMessage = new byte[encryptedMessage.length - Box.MACBYTES];
        sodium.crypto_secretbox_open_easy(
                decryptedMessage,
                encryptedMessage,
                encryptedMessage.length,
                nonce,
                seed.getNotificationKey().toByteArray());
        String decryptedMessageString = new String(decryptedMessage);
        assertEquals(message, decryptedMessageString);
        System.out.println("Message: " + message + " DecryptedMessge: " + decryptedMessageString);
    }
}
