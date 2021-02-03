package ch.ubique.notifyme.sdk.backend.ws.crypto;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
        List<Integer> affectedHours = List.of(447347, 447348, 447349, 447350);
        List<String> preTraces = List.of(
                        "CsgBCiBAzNWU7qWQm4rFZWHEQHxhMSuoyJtgFNYD3E8r6pK0URIwraClwvK8Sx07WmS60hoNWZFt1H3GRsaXLZRF9hJUEh2dj4IOjnlkzBCFCrDojViUGlCXLoE4-SbSc1gwSWVijdyR3uwoeuqdZyG_9FZA7cBjCjtVoZO4_C1ZrISRpukF7l2m7vsZArA5w5MDYQbG64My8A02mUWeStIIBAGhntVJASog3ogH375bltnHWTQ7HvfCOuedgnD4OaJagn5kAvhSxAISpgEKYJLq8RBroSeFJdxEq9JsBSb3uzZzGLmxd6wgqep-CpgfIOGtvca9m9K-DLAuCuIMCi7iqbLQE_DNfrIwEvIq6zCDrTae1pe_4HFTL58WvoZ0IKnDtmHxY9X9YthH96lzihIgRykOFIjxdNkTnmwY4_ODQAHGkIkmCxuuPoJUAtWkDvUaIJC72kXD8AFKuEdXEypUY3j0mesNDP40uITSsg9pM5hfGnQKElRpdGVsIGZvciBVbml0VGVzdBIXVW50ZXJ0aXRlbCBmb3IgVW5pdFRlc3QaE1p1c2F0eiBmb3IgVW5pdFRlc3QgCiog3ogH375bltnHWTQ7HvfCOuedgnD4OaJagn5kAvhSxAIwgOu1ne8uOICjz8bvLg",
                        "CsgBCiBqfB8MENG5x1W9ajCHuPfFa0ks_rpAFxlk1CgMye7c0xIw5Rb5pCAvu8qmd1zMHSs7ZdK5-HacRNXVZj2GoX3M_WOyyo4v2DVolUEJtpJnBnkDGlCXLoE4-SbSc1gwSWVijdyR3uwoeuqdZyG_9FZA7cBjCjtVoZO4_C1ZrISRpukF7l2m7vsZArA5w5MDYQbG64My8A02mUWeStIIBAGhntVJASog3ogH375bltnHWTQ7HvfCOuedgnD4OaJagn5kAvhSxAISpgEKYJLq8RBroSeFJdxEq9JsBSb3uzZzGLmxd6wgqep-CpgfIOGtvca9m9K-DLAuCuIMCi7iqbLQE_DNfrIwEvIq6zCDrTae1pe_4HFTL58WvoZ0IKnDtmHxY9X9YthH96lzihIgRykOFIjxdNkTnmwY4_ODQAHGkIkmCxuuPoJUAtWkDvUaIJC72kXD8AFKuEdXEypUY3j0mesNDP40uITSsg9pM5hfGnQKElRpdGVsIGZvciBVbml0VGVzdBIXVW50ZXJ0aXRlbCBmb3IgVW5pdFRlc3QaE1p1c2F0eiBmb3IgVW5pdFRlc3QgCiog3ogH375bltnHWTQ7HvfCOuedgnD4OaJagn5kAvhSxAIwgOu1ne8uOICjz8bvLg",
                        "CsgBCiBOPDLfaM4PzSRJlhHG9IPwzwxUIRHBXuncooN62nqX_RIwbMg4uAM3P9m9xpTVATiSw_0IF7TOLJB4IooYFp5YfwJCmKCAZd36UkXIsxV5ZK0OGlCXLoE4-SbSc1gwSWVijdyR3uwoeuqdZyG_9FZA7cBjCjtVoZO4_C1ZrISRpukF7l2m7vsZArA5w5MDYQbG64My8A02mUWeStIIBAGhntVJASog3ogH375bltnHWTQ7HvfCOuedgnD4OaJagn5kAvhSxAISpgEKYJLq8RBroSeFJdxEq9JsBSb3uzZzGLmxd6wgqep-CpgfIOGtvca9m9K-DLAuCuIMCi7iqbLQE_DNfrIwEvIq6zCDrTae1pe_4HFTL58WvoZ0IKnDtmHxY9X9YthH96lzihIgRykOFIjxdNkTnmwY4_ODQAHGkIkmCxuuPoJUAtWkDvUaIJC72kXD8AFKuEdXEypUY3j0mesNDP40uITSsg9pM5hfGnQKElRpdGVsIGZvciBVbml0VGVzdBIXVW50ZXJ0aXRlbCBmb3IgVW5pdFRlc3QaE1p1c2F0eiBmb3IgVW5pdFRlc3QgCiog3ogH375bltnHWTQ7HvfCOuedgnD4OaJagn5kAvhSxAIwgOu1ne8uOICjz8bvLg",
                        "CsgBCiBrGPvV2tH2ztwdfBk4aAZ-Fem2-T9WOuLVgI-reYHZVRIwK8-5kSnd6XK8b6ndZwi1jwlkckIUwwMo1GKm4R-BnCxPsGCk5CpcJ7RX-jp8lnEEGlCXLoE4-SbSc1gwSWVijdyR3uwoeuqdZyG_9FZA7cBjCjtVoZO4_C1ZrISRpukF7l2m7vsZArA5w5MDYQbG64My8A02mUWeStIIBAGhntVJASog3ogH375bltnHWTQ7HvfCOuedgnD4OaJagn5kAvhSxAISpgEKYJLq8RBroSeFJdxEq9JsBSb3uzZzGLmxd6wgqep-CpgfIOGtvca9m9K-DLAuCuIMCi7iqbLQE_DNfrIwEvIq6zCDrTae1pe_4HFTL58WvoZ0IKnDtmHxY9X9YthH96lzihIgRykOFIjxdNkTnmwY4_ODQAHGkIkmCxuuPoJUAtWkDvUaIJC72kXD8AFKuEdXEypUY3j0mesNDP40uITSsg9pM5hfGnQKElRpdGVsIGZvciBVbml0VGVzdBIXVW50ZXJ0aXRlbCBmb3IgVW5pdFRlc3QaE1p1c2F0eiBmb3IgVW5pdFRlc3QgCiog3ogH375bltnHWTQ7HvfCOuedgnD4OaJagn5kAvhSxAIwgOu1ne8uOICjz8bvLg");

        for (int i = 0; i < affectedHours.size(); i++) {
            String preTraceKeyBase64 = preTraces.get(i);
            Integer affectedHour = affectedHours.get(i);

            TraceKey traceKey = new TraceKey();

            byte[] preTraceKeyBytes = Base64.getUrlDecoder().decode(preTraceKeyBase64.getBytes("UTF-8"));
            PreTraceWithProof preTraceWithProofProto = PreTraceWithProof.parseFrom(preTraceKeyBytes);

            cryptoWrapper.calculateSecretKeyForIdentityAndIdentity(preTraceWithProofProto, affectedHour, traceKey);

            assertNotNull(traceKey.getIdentity());
            assertNotNull(traceKey.getSecretKeyForIdentity());

            byte[] notificationKey = preTraceWithProofProto.getPreTrace().getNotificationKey().toByteArray();
            assertTrue(notificationKey.length > 0);

            byte[] nonce = cryptoWrapper.createNonceForMessageEncytion();
            byte[] encryptedMessage = cryptoWrapper.encryptMessage(notificationKey, nonce, message);
            traceKey.setMessage(encryptedMessage);
            traceKey.setNonce(nonce);

        }
    }

    @Test
    public void testWithVenueTypeOther() throws UnsupportedEncodingException, InvalidProtocolBufferException {
        CryptoWrapper cryptoWrapper = new CryptoWrapper(
                        "36b3b80a1cd2cc98d84b4ed2c109b74e7026f00c0d40a0b12a936b1814aa5e39",
                        "e4d2e06641730ce7c9986b1e7e91bf41bb3b8cc1d76d249fa99d0d8925e87a5c");

        String message = "This is the secret message";
        List<Integer> affectedHours = List.of(447343, 447344, 447345, 447346, 447347);
        List<String> preTraces = List.of(
                        "CsgBCiD_TLxamAiOUR9mCw5BArm55970U8PCIq92Lajqh7FNJBIw0NTqvJDbcPBPoNNtI_J9IsIAQyVmMS6NUiftS60mgJ0R1gkwD1X6MsspKhS2iC2KGlA5bdVoAzg1ziaO5enPNTaJ3_JfakxPq6GWq4qRR5hmfS5mOz2kcA7sMaNJPA8p_Ik-mWyQ8apC7sWF-3EMxt6lhFVhADTwf92mTMCCRBaajCog7MthtQxpXJz0ENi9ndiY5mrskV34QBe2Eo0fLKUe0hYSpgEKYBepQedjPJTnNPztXtLdbdGS-8D1wFr-r7E7AVhaG-k_NmCo_MyUPK2AbqsJTXTWDQXpriSdfqLNMny7rY0ya2qh4sUGT2LhnaXoPaRs_VjVS27FXBArI4IrqsF24C6tkRIgaLeZcycH1Knk4DpOenjwF7KmI7-ef2YcAwsRMM0VClcaIGRaKuQf4lBa9mj35M-y0GuX9jqmtbaWAseWEXlJWzhaGnIKElRpdGVsIGZvciBVbml0VGVzdBIXVW50ZXJ0aXRlbCBmb3IgVW5pdFRlc3QaE1p1c2F0eiBmb3IgVW5pdFRlc3QqIOzLYbUMaVyc9BDYvZ3YmOZq7JFd-EAXthKNHyylHtIWMIDrtZ3vLjiAo8_G7y4",
                        "CsgBCiA0Eg6t0gzsvraZycJc5EmzEpsBGAZOLoYIFyYOYqL_iBIw5812RGSTuNlGf8hGjz9pXByHe2X1cICdA4yruqc8elRKNMXrFijlRBVzOt-Gie-PGlA5bdVoAzg1ziaO5enPNTaJ3_JfakxPq6GWq4qRR5hmfS5mOz2kcA7sMaNJPA8p_Ik-mWyQ8apC7sWF-3EMxt6lhFVhADTwf92mTMCCRBaajCog7MthtQxpXJz0ENi9ndiY5mrskV34QBe2Eo0fLKUe0hYSpgEKYBepQedjPJTnNPztXtLdbdGS-8D1wFr-r7E7AVhaG-k_NmCo_MyUPK2AbqsJTXTWDQXpriSdfqLNMny7rY0ya2qh4sUGT2LhnaXoPaRs_VjVS27FXBArI4IrqsF24C6tkRIgaLeZcycH1Knk4DpOenjwF7KmI7-ef2YcAwsRMM0VClcaIGRaKuQf4lBa9mj35M-y0GuX9jqmtbaWAseWEXlJWzhaGnIKElRpdGVsIGZvciBVbml0VGVzdBIXVW50ZXJ0aXRlbCBmb3IgVW5pdFRlc3QaE1p1c2F0eiBmb3IgVW5pdFRlc3QqIOzLYbUMaVyc9BDYvZ3YmOZq7JFd-EAXthKNHyylHtIWMIDrtZ3vLjiAo8_G7y4",
                        "CsgBCiD_WDNY4iEWST56ZOfTlYGIQOW2Cw2qCc4yBVjeVUPm6RIwf1NOTntn2mVre45JbhiHukJFs_IrsXXH_MZvV9EuJZFfbc-nK5Y_E0CNLHqdBpADGlA5bdVoAzg1ziaO5enPNTaJ3_JfakxPq6GWq4qRR5hmfS5mOz2kcA7sMaNJPA8p_Ik-mWyQ8apC7sWF-3EMxt6lhFVhADTwf92mTMCCRBaajCog7MthtQxpXJz0ENi9ndiY5mrskV34QBe2Eo0fLKUe0hYSpgEKYBepQedjPJTnNPztXtLdbdGS-8D1wFr-r7E7AVhaG-k_NmCo_MyUPK2AbqsJTXTWDQXpriSdfqLNMny7rY0ya2qh4sUGT2LhnaXoPaRs_VjVS27FXBArI4IrqsF24C6tkRIgaLeZcycH1Knk4DpOenjwF7KmI7-ef2YcAwsRMM0VClcaIGRaKuQf4lBa9mj35M-y0GuX9jqmtbaWAseWEXlJWzhaGnIKElRpdGVsIGZvciBVbml0VGVzdBIXVW50ZXJ0aXRlbCBmb3IgVW5pdFRlc3QaE1p1c2F0eiBmb3IgVW5pdFRlc3QqIOzLYbUMaVyc9BDYvZ3YmOZq7JFd-EAXthKNHyylHtIWMIDrtZ3vLjiAo8_G7y4",
                        "CsgBCiBI_FZejSPzejgUtc8VoBoi-SJzgdXOZQz8Fx3zC8oKDRIwzpk_IOcUXsbp61I_8DRPInijjeFc3I-j2yhOL3aEY6G0m9pPn6BYIG--YTrPSm-TGlA5bdVoAzg1ziaO5enPNTaJ3_JfakxPq6GWq4qRR5hmfS5mOz2kcA7sMaNJPA8p_Ik-mWyQ8apC7sWF-3EMxt6lhFVhADTwf92mTMCCRBaajCog7MthtQxpXJz0ENi9ndiY5mrskV34QBe2Eo0fLKUe0hYSpgEKYBepQedjPJTnNPztXtLdbdGS-8D1wFr-r7E7AVhaG-k_NmCo_MyUPK2AbqsJTXTWDQXpriSdfqLNMny7rY0ya2qh4sUGT2LhnaXoPaRs_VjVS27FXBArI4IrqsF24C6tkRIgaLeZcycH1Knk4DpOenjwF7KmI7-ef2YcAwsRMM0VClcaIGRaKuQf4lBa9mj35M-y0GuX9jqmtbaWAseWEXlJWzhaGnIKElRpdGVsIGZvciBVbml0VGVzdBIXVW50ZXJ0aXRlbCBmb3IgVW5pdFRlc3QaE1p1c2F0eiBmb3IgVW5pdFRlc3QqIOzLYbUMaVyc9BDYvZ3YmOZq7JFd-EAXthKNHyylHtIWMIDrtZ3vLjiAo8_G7y4",
                        "CsgBCiD6LrfUXElqahqDxcMf_pW4OrKNjsfSSNkSdNiSb4IIzhIwNV2IGX1Jsf497oi-cNixCpJsOjJmlFQVEJUsJGwAT5Ytll7r6XVahc9-dbG05jaJGlA5bdVoAzg1ziaO5enPNTaJ3_JfakxPq6GWq4qRR5hmfS5mOz2kcA7sMaNJPA8p_Ik-mWyQ8apC7sWF-3EMxt6lhFVhADTwf92mTMCCRBaajCog7MthtQxpXJz0ENi9ndiY5mrskV34QBe2Eo0fLKUe0hYSpgEKYBepQedjPJTnNPztXtLdbdGS-8D1wFr-r7E7AVhaG-k_NmCo_MyUPK2AbqsJTXTWDQXpriSdfqLNMny7rY0ya2qh4sUGT2LhnaXoPaRs_VjVS27FXBArI4IrqsF24C6tkRIgaLeZcycH1Knk4DpOenjwF7KmI7-ef2YcAwsRMM0VClcaIGRaKuQf4lBa9mj35M-y0GuX9jqmtbaWAseWEXlJWzhaGnIKElRpdGVsIGZvciBVbml0VGVzdBIXVW50ZXJ0aXRlbCBmb3IgVW5pdFRlc3QaE1p1c2F0eiBmb3IgVW5pdFRlc3QqIOzLYbUMaVyc9BDYvZ3YmOZq7JFd-EAXthKNHyylHtIWMIDrtZ3vLjiAo8_G7y4");

        for (int i = 0; i < affectedHours.size(); i++) {
            String preTraceKeyBase64 = preTraces.get(i);
            Integer affectedHour = affectedHours.get(i);

            TraceKey traceKey = new TraceKey();

            byte[] preTraceKeyBytes = Base64.getUrlDecoder().decode(preTraceKeyBase64.getBytes("UTF-8"));
            PreTraceWithProof preTraceWithProofProto = PreTraceWithProof.parseFrom(preTraceKeyBytes);

            cryptoWrapper.calculateSecretKeyForIdentityAndIdentity(preTraceWithProofProto, affectedHour, traceKey);

            assertNotNull(traceKey.getIdentity());
            assertNotNull(traceKey.getSecretKeyForIdentity());

            byte[] notificationKey = preTraceWithProofProto.getPreTrace().getNotificationKey().toByteArray();
            assertTrue(notificationKey.length > 0);

            byte[] nonce = cryptoWrapper.createNonceForMessageEncytion();
            byte[] encryptedMessage = cryptoWrapper.encryptMessage(notificationKey, nonce, message);
            traceKey.setMessage(encryptedMessage);
            traceKey.setNonce(nonce);

        }
    }
}
