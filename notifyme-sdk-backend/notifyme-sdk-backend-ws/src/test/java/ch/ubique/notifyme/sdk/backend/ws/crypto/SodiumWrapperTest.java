package ch.ubique.notifyme.sdk.backend.ws.crypto;

import static org.junit.Assert.assertNotNull;

import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.List;

import org.junit.Test;

import com.google.protobuf.InvalidProtocolBufferException;

import ch.ubique.notifyme.sdk.backend.model.PreTraceWithProofOuterClass.PreTraceWithProof;
import ch.ubique.notifyme.sdk.backend.model.tracekey.TraceKey;
import ch.ubique.notifyme.sdk.backend.ws.CryptoWrapper;

public class SodiumWrapperTest {

    @Test
    public void test() throws UnsupportedEncodingException, InvalidProtocolBufferException {
        CryptoWrapper cryptoWrapper = new CryptoWrapper(
                        "36b3b80a1cd2cc98d84b4ed2c109b74e7026f00c0d40a0b12a936b1814aa5e39",
                        "e4d2e06641730ce7c9986b1e7e91bf41bb3b8cc1d76d249fa99d0d8925e87a5c");

        String message = "This is the secret message";
        List<Integer> affectedHours = List.of(447323, 447324, 447325, 447326, 447327);
        List<String> preTraces = List.of(
                        "CqYBCiCGxTFZh-GAe4nNPW5n4SjziE_d45SkRaU2AILR6z0UkBIw0aFoOgi-kLADDX_8kSn9R-92BRq2JcXk24q1yG4NKA3f7lDKiZOmxqgz8e9RsDOKGlBs-4XMy7TkGXAE-setFYbGoKY4NTfM4I-viHjGtzt0bRFFkeFhQibYLM5wzEQgOwqpFnwu4CSGyNyPKWO0c2iFCV2HpbJ2dVICeZc_BwNbxhKmAQpgU1xkfIFU9Uc4hAS06upNlf4S7JSWZP9ml29RBczz9QENxM1qCgXtzyXrPnXkQj0M5BK8-Zq0epFwrn2EavlJZv57JGk2U3uza0m1dST6ugijsh3SnjNlk5ARvqJ3N8-REiC9113CccuonklzjxNiWK1JKI3lQCDG_af8iqYaycUZ_Bog_cgwg7BalyCYN73JIRcgSas3UgfLuCyKWdy2uF_6o9sadAoSVGl0ZWwgZm9yIFVuaXRUZXN0EhdVbnRlcnRpdGVsIGZvciBVbml0VGVzdBoTWnVzYXR6IGZvciBVbml0VGVzdCABKiAdN_wgnLa1vN7HSYiyMiMJci97D-ojJ_BMtHgfomCl7zCAs5z07i44gOu1ne8u",
                        "CqYBCiCPYwcoXUGLXvacc-7f9c1hJwclFwFmwhyuCjHSb27NVBIwa7RufJAxkuwEgWMFBs_IFHA9KSxETezX-5TtUQL_ZerFGK_8-kZlyNOSaK5EDd4TGlBs-4XMy7TkGXAE-setFYbGoKY4NTfM4I-viHjGtzt0bRFFkeFhQibYLM5wzEQgOwqpFnwu4CSGyNyPKWO0c2iFCV2HpbJ2dVICeZc_BwNbxhKmAQpgU1xkfIFU9Uc4hAS06upNlf4S7JSWZP9ml29RBczz9QENxM1qCgXtzyXrPnXkQj0M5BK8-Zq0epFwrn2EavlJZv57JGk2U3uza0m1dST6ugijsh3SnjNlk5ARvqJ3N8-REiC9113CccuonklzjxNiWK1JKI3lQCDG_af8iqYaycUZ_Bog_cgwg7BalyCYN73JIRcgSas3UgfLuCyKWdy2uF_6o9sadAoSVGl0ZWwgZm9yIFVuaXRUZXN0EhdVbnRlcnRpdGVsIGZvciBVbml0VGVzdBoTWnVzYXR6IGZvciBVbml0VGVzdCABKiAdN_wgnLa1vN7HSYiyMiMJci97D-ojJ_BMtHgfomCl7zCAs5z07i44gOu1ne8u",
                        "CqYBCiAyWLkPEVAdN7Tl2874EfrgDD6pj2a4p6cEtNTxrdENjxIwuYUym418T9SEaV9gqYCyJ3cKUmhVq8OOknf5c2ug-GTRhJYO4tVxO2mKcFz9gLgSGlBs-4XMy7TkGXAE-setFYbGoKY4NTfM4I-viHjGtzt0bRFFkeFhQibYLM5wzEQgOwqpFnwu4CSGyNyPKWO0c2iFCV2HpbJ2dVICeZc_BwNbxhKmAQpgU1xkfIFU9Uc4hAS06upNlf4S7JSWZP9ml29RBczz9QENxM1qCgXtzyXrPnXkQj0M5BK8-Zq0epFwrn2EavlJZv57JGk2U3uza0m1dST6ugijsh3SnjNlk5ARvqJ3N8-REiC9113CccuonklzjxNiWK1JKI3lQCDG_af8iqYaycUZ_Bog_cgwg7BalyCYN73JIRcgSas3UgfLuCyKWdy2uF_6o9sadAoSVGl0ZWwgZm9yIFVuaXRUZXN0EhdVbnRlcnRpdGVsIGZvciBVbml0VGVzdBoTWnVzYXR6IGZvciBVbml0VGVzdCABKiAdN_wgnLa1vN7HSYiyMiMJci97D-ojJ_BMtHgfomCl7zCAs5z07i44gOu1ne8u",
                        "CqYBCiBWPYb6jrnnlz2uDf6O0lwQlznK80Uwsw9OKYnNuA8OmhIwqJt5RPM2_pbWNWrVtnibwFRpxf3Ux3bT8pWoJyYWMa5ZEdnQR0mASOl6Aa1WL2qTGlBs-4XMy7TkGXAE-setFYbGoKY4NTfM4I-viHjGtzt0bRFFkeFhQibYLM5wzEQgOwqpFnwu4CSGyNyPKWO0c2iFCV2HpbJ2dVICeZc_BwNbxhKmAQpgU1xkfIFU9Uc4hAS06upNlf4S7JSWZP9ml29RBczz9QENxM1qCgXtzyXrPnXkQj0M5BK8-Zq0epFwrn2EavlJZv57JGk2U3uza0m1dST6ugijsh3SnjNlk5ARvqJ3N8-REiC9113CccuonklzjxNiWK1JKI3lQCDG_af8iqYaycUZ_Bog_cgwg7BalyCYN73JIRcgSas3UgfLuCyKWdy2uF_6o9sadAoSVGl0ZWwgZm9yIFVuaXRUZXN0EhdVbnRlcnRpdGVsIGZvciBVbml0VGVzdBoTWnVzYXR6IGZvciBVbml0VGVzdCABKiAdN_wgnLa1vN7HSYiyMiMJci97D-ojJ_BMtHgfomCl7zCAs5z07i44gOu1ne8u",
                        "CqYBCiA7qhqX7tKptve2ufHbrG5MsTkh1kOf0kYOhfrpzt6uFBIwnM-GIunoY1N79ur8zT6zJOxydWqopK5Y4a1_rZDi7UAstTPqrZ8Vsk2O71cCT3eRGlBs-4XMy7TkGXAE-setFYbGoKY4NTfM4I-viHjGtzt0bRFFkeFhQibYLM5wzEQgOwqpFnwu4CSGyNyPKWO0c2iFCV2HpbJ2dVICeZc_BwNbxhKmAQpgU1xkfIFU9Uc4hAS06upNlf4S7JSWZP9ml29RBczz9QENxM1qCgXtzyXrPnXkQj0M5BK8-Zq0epFwrn2EavlJZv57JGk2U3uza0m1dST6ugijsh3SnjNlk5ARvqJ3N8-REiC9113CccuonklzjxNiWK1JKI3lQCDG_af8iqYaycUZ_Bog_cgwg7BalyCYN73JIRcgSas3UgfLuCyKWdy2uF_6o9sadAoSVGl0ZWwgZm9yIFVuaXRUZXN0EhdVbnRlcnRpdGVsIGZvciBVbml0VGVzdBoTWnVzYXR6IGZvciBVbml0VGVzdCABKiAdN_wgnLa1vN7HSYiyMiMJci97D-ojJ_BMtHgfomCl7zCAs5z07i44gOu1ne8u");

        for (int i = 0; i < affectedHours.size(); i++) {
            String preTraceKeyBase64 = preTraces.get(i);
            Integer affectedHour = affectedHours.get(i);

            TraceKey traceKey = new TraceKey();

            byte[] preTraceKeyBytes = Base64.getUrlDecoder().decode(preTraceKeyBase64.getBytes("UTF-8"));
            PreTraceWithProof preTraceWithProofProto = PreTraceWithProof.parseFrom(preTraceKeyBytes);

            cryptoWrapper.calculateSecretKeyForIdentityAndIdentity(preTraceWithProofProto, affectedHour, traceKey);

            assertNotNull(traceKey.getIdentity());
            assertNotNull(traceKey.getSecretKeyForIdentity());

            byte[] nonce = cryptoWrapper.createNonceForMessageEncytion();
            byte[] encryptedMessage = cryptoWrapper.encryptMessage(
                            preTraceWithProofProto.getPreTrace().getNotificationKey().toByteArray(), nonce, message);
            traceKey.setMessage(encryptedMessage);
            traceKey.setNonce(nonce);

        }
    }

    @Test
    public void test2() throws UnsupportedEncodingException, InvalidProtocolBufferException {
        CryptoWrapper cryptoWrapper = new CryptoWrapper(
                        "36b3b80a1cd2cc98d84b4ed2c109b74e7026f00c0d40a0b12a936b1814aa5e39",
                        "e4d2e06641730ce7c9986b1e7e91bf41bb3b8cc1d76d249fa99d0d8925e87a5c");

        String message = "This is the secret message";
        List<Integer> affectedHours = List.of(447323);
        List<String> preTraces = List.of(
                        "CqYBCiBdDNWz2TGxXQpZ1VQBX-6u2CF0UsqE_dLPdNeiZzHNGRIwz0UnbNKSJ0u6RCA6G8nwX90XKvb_sulLij4nsT4YNdR_5kOCmSKNdEdtEg5qnL6OGlB_xkpUe8L4-oQsFCHdmryYjsL2irezIy0c_shYOFyUTnBrKSCPFICw3KgeofnIen2EtzpvhSSJJCI5NaZjgMn1dm9EojNyb4dh4vrt1hsB7RKmAQpgLd_oO60Aw9V1RoBRLwsqNmo_YkLyEkBU5Exax75UcC8emdTJ0e-xtHYBc22Taw4NPCsV_QPDE7rFIezqjdTGwNmt5tvFkEix5w587756pCRtojkK4omG8JR5k0hYbZCXEiAIjHlgnrqOLOYCeZM9eHS7AZvwmwUr8KHe1YmtozYM-hog8XXFV9BZNEKRHPlbY7MhOk6UmR_elMnQRs_WxxeCAtwadAoSVGl0ZWwgZm9yIFVuaXRUZXN0EhdVbnRlcnRpdGVsIGZvciBVbml0VGVzdBoTWnVzYXR6IGZvciBVbml0VGVzdCAAKiDvsuWrcgiag-A6USYB_RIq1l3IYykdIENuGpjxEx-6UjCAs5z07i44gOu1ne8u");
        for (int i = 0; i < affectedHours.size(); i++) {
            String preTraceKeyBase64 = preTraces.get(i);
            Integer affectedHour = affectedHours.get(i);

            TraceKey traceKey = new TraceKey();

            byte[] preTraceKeyBytes = Base64.getUrlDecoder().decode(preTraceKeyBase64.getBytes("UTF-8"));
            PreTraceWithProof preTraceWithProofProto = PreTraceWithProof.parseFrom(preTraceKeyBytes);

            cryptoWrapper.calculateSecretKeyForIdentityAndIdentity(preTraceWithProofProto, affectedHour, traceKey);

            assertNotNull(traceKey.getIdentity());
            assertNotNull(traceKey.getSecretKeyForIdentity());

            byte[] nonce = cryptoWrapper.createNonceForMessageEncytion();
            byte[] encryptedMessage = cryptoWrapper.encryptMessage(
                            preTraceWithProofProto.getPreTrace().getNotificationKey().toByteArray(), nonce, message);
            traceKey.setMessage(encryptedMessage);
            traceKey.setNonce(nonce);

        }
    }
}
