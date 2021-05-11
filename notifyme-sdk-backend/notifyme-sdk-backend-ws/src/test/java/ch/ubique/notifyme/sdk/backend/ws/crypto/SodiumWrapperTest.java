package ch.ubique.notifyme.sdk.backend.ws.crypto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import ch.ubique.notifyme.sdk.backend.model.PreTraceWithProofOuterClass.PreTraceWithProof;
import ch.ubique.notifyme.sdk.backend.model.UserUploadPayloadOuterClass.UploadVenueInfo;
import ch.ubique.notifyme.sdk.backend.model.UserUploadPayloadOuterClass.UserUploadPayload;
import ch.ubique.notifyme.sdk.backend.model.tracekey.v2.TraceKey;
import ch.ubique.notifyme.sdk.backend.model.v3.AssociatedDataOuterClass.AssociatedData;
import ch.ubique.notifyme.sdk.backend.model.v3.NotifyMeAssociatedDataOuterClass.EventCriticality;
import ch.ubique.notifyme.sdk.backend.model.v3.NotifyMeAssociatedDataOuterClass.NotifyMeAssociatedData;
import ch.ubique.notifyme.sdk.backend.model.v3.QrCodePayload.CrowdNotifierData;
import ch.ubique.notifyme.sdk.backend.model.v3.QrCodePayload.QRCodePayload;
import ch.ubique.notifyme.sdk.backend.model.v3.QrCodePayload.TraceLocation;
import ch.ubique.notifyme.sdk.backend.ws.util.CryptoWrapper;
import ch.ubique.notifyme.sdk.backend.ws.util.CryptoUtil.IBECiphertext;
import ch.ubique.notifyme.sdk.backend.ws.util.CryptoUtil.NoncesAndNotificationKey;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.herumi.mcl.G1;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.apache.commons.codec.DecoderException;
import org.junit.Test;

public class SodiumWrapperTest {

  private static final int INTERVAL_LENGTH = 3600; // interval length seconds

  private static final Integer QR_CODE_VERSION_3 = 3;

  private static final int CRYPTOGRAPHIC_SEED_BYTES = 32;

  private final CryptoWrapper cryptoWrapper =
      new CryptoWrapper(
          "36b3b80a1cd2cc98d84b4ed2c109b74e7026f00c0d40a0b12a936b1814aa5e39",
          "e4d2e06641730ce7c9986b1e7e91bf41bb3b8cc1d76d249fa99d0d8925e87a5c",
          "ce23ca6a3fd0d1307d3d0b2578784750b3f0e20b64e0c24e4cafb35561a0af35",
          "956e6fa1345547e8e060c8962ddd38863bf2c85406ed03b204bc340fb5db01296a960d00be240caa08db001664f4f7028a9dbbb33aea172bffd58b4a644f1ecb3b7bbed378a8a7c9756ac8b4b47346d8dbf37a62377703b7fc8da3bb22a21415");

  @Test
  public void test() throws UnsupportedEncodingException, InvalidProtocolBufferException {
    String message = "This is the secret message";
    List<Integer> affectedHours = List.of(447347, 447348, 447349, 447350);
    List<String> preTraces =
        List.of(
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

      cryptoWrapper
          .getCryptoUtilV2()
          .calculateSecretKeyForIdentityAndIdentity(preTraceWithProofProto, affectedHour, traceKey);

      assertNotNull(traceKey.getIdentity());
      assertNotNull(traceKey.getSecretKeyForIdentity());

      byte[] notificationKey =
          preTraceWithProofProto.getPreTrace().getNotificationKey().toByteArray();
      assertTrue(notificationKey.length > 0);

      byte[] nonce = cryptoWrapper.getCryptoUtilV2().createNonce();
      byte[] encryptedMessage =
          cryptoWrapper.getCryptoUtilV2().encryptMessage(notificationKey, nonce, message);
      traceKey.setMessage(encryptedMessage);
      traceKey.setNonce(nonce);
    }
  }

  @Test
  public void testWithVenueTypeOther()
      throws UnsupportedEncodingException, InvalidProtocolBufferException {

    String message = "This is the secret message";
    List<Integer> affectedHours = List.of(447343, 447344, 447345, 447346, 447347);
    List<String> preTraces =
        List.of(
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

      cryptoWrapper
          .getCryptoUtilV2()
          .calculateSecretKeyForIdentityAndIdentity(preTraceWithProofProto, affectedHour, traceKey);

      assertNotNull(traceKey.getIdentity());
      assertNotNull(traceKey.getSecretKeyForIdentity());

      byte[] notificationKey =
          preTraceWithProofProto.getPreTrace().getNotificationKey().toByteArray();
      assertTrue(notificationKey.length > 0);

      byte[] nonce = cryptoWrapper.getCryptoUtilV2().createNonce();
      byte[] encryptedMessage =
          cryptoWrapper.getCryptoUtilV2().encryptMessage(notificationKey, nonce, message);
      traceKey.setMessage(encryptedMessage);
      traceKey.setNonce(nonce);
    }
  }

  @Test
  public void testUserUpload()
      throws UnsupportedEncodingException, InvalidProtocolBufferException, DecoderException {
    Instant now = Instant.now();
    Instant uploadStart = now.minus(10, ChronoUnit.HOURS);
    Instant uploadEnd = now.minus(1, ChronoUnit.HOURS);

    Instant validFrom = now.minus(10, ChronoUnit.HOURS);
    Instant validTo = now.minus(1, ChronoUnit.HOURS);

    List<Long> intervalStarts =
        getAffectedIntervalStarts(
            uploadStart.toEpochMilli() / 1000, uploadEnd.toEpochMilli() / 1000);
    UserUploadPayload.Builder userUploadBuilder = UserUploadPayload.newBuilder().setVersion(1);

    VenueInfo venueInfo =
        generateEntryQrCode(
            "description",
            "address",
            "countryData".getBytes("UTF-8"),
            validFrom.toEpochMilli(),
            validTo.toEpochMilli(),
            cryptoWrapper.getCryptoUtilV3().getMpkG2().serialize());

    for (Long intervalStart : intervalStarts) {

      PreIdAndTimeKey preIdAndTimeKey =
          getPreIdAndTimeKey(venueInfo.getQrCodePayload(), intervalStart, INTERVAL_LENGTH);

      userUploadBuilder.addVenueInfos(
          UploadVenueInfo.newBuilder()
              .setFake(false)
              .setPreId(ByteString.copyFrom(preIdAndTimeKey.preId))
              .setNotificationKey(ByteString.copyFrom(venueInfo.getNotificationKey()))
              .setTimeKey(ByteString.copyFrom(preIdAndTimeKey.timeKey))
              .setIntervalStartMs(Math.max(intervalStart * 1000, uploadStart.toEpochMilli()))
              .setIntervalEndMs(
                  Math.min((intervalStart + INTERVAL_LENGTH) * 1000, uploadEnd.toEpochMilli()))
              .build());
    }

    UserUploadPayload userUpload = userUploadBuilder.build();

    List<ch.ubique.notifyme.sdk.backend.model.tracekey.v3.TraceKey> traceKeys =
        cryptoWrapper.getCryptoUtilV3().createTraceV3ForUserUpload(userUpload);

    // 1 hour checkin, should give 2 matches, as long es the test does not run
    // exactly at the full hour.
    Instant matchStart = now.minus(4, ChronoUnit.HOURS);
    Instant matchEnd = now.minus(3, ChronoUnit.HOURS);

    int matchCount = 0;

    for (ch.ubique.notifyme.sdk.backend.model.tracekey.v3.TraceKey k : traceKeys) {
      byte[] decryptedAssociatedData =
          cryptoWrapper
              .getCryptoUtilV3()
              .cryptoSecretboxOpenEasy(
                  venueInfo.getNotificationKey(),
                  k.getEncryptedAssociatedData(),
                  k.getCipherTextNonce());
      assertFalse(decryptedAssociatedData.length == 0);
      AssociatedData associatedData = AssociatedData.parseFrom(decryptedAssociatedData);
      NotifyMeAssociatedData notifyMeAssociatedData =
          NotifyMeAssociatedData.parseFrom(associatedData.getCountryData().toByteArray());
      assertEquals("", associatedData.getMessage());
      assertEquals(EventCriticality.LOW, notifyMeAssociatedData.getCriticality());

      if (doIntersect(
          matchStart.toEpochMilli(),
          matchEnd.toEpochMilli(),
          associatedData.getStartTimestamp() * 1000,
          associatedData.getEndTimestamp() * 1000)) {
        matchCount++;
      }
    }
    assertEquals(2, matchCount);
  }

  @Test
  public void testUserUploadMatching()
      throws UnsupportedEncodingException, InvalidProtocolBufferException {
    byte[] identity = fromBase64("x-oEasv8-6-B5BjpawjPYEQs5uv7k0l_J461lZRNyFo=");
    byte[] secretKeyForIdentity =
        fromBase64("sWofRGzy7NLQXYTfq8Vk09X9ZltMJfniVBiheAyrXn0ajbvqAdjGcZZ_JedJyd0J");
    byte[] encryptedAssociatedData = fromBase64("fhRQTTtKnvKpvfyuv_BjJsCqRCb7jg==");
    byte[] cipherTextNonce = fromBase64("Xzbn4Wr041OEsrJ525Q_E2IjBaMHVKwe");
    byte[] notificationKey =
        new byte[] {
          100, -18, 12, -102, -114, -120, -55, 93, 47, 96, -120, -12, 60, -7, 89, 84, 2, -92, 11,
          92, 84, -86, 19, -91, -89, -1, -18, 22, -126, -79, 78, -23
        };
    byte[] decryptedAssociatedData =
        cryptoWrapper
            .getCryptoUtilV3()
            .cryptoSecretboxOpenEasy(notificationKey, encryptedAssociatedData, cipherTextNonce);
    assertFalse(decryptedAssociatedData.length == 0);
    AssociatedData associatedData = AssociatedData.parseFrom(decryptedAssociatedData);
    NotifyMeAssociatedData notifyMeAssociatedData =
        NotifyMeAssociatedData.parseFrom(associatedData.getCountryData().toByteArray());
    assertEquals("", associatedData.getMessage());
    assertEquals(EventCriticality.LOW, notifyMeAssociatedData.getCriticality());

    // verifyTrace
    G1 secretKeyForIdentityG1 = new G1();
    secretKeyForIdentityG1.deserialize(secretKeyForIdentity);

    int NONCE_LENGTH = 32;
    byte[] msg_orig = cryptoWrapper.getCryptoUtilV3().createNonce(NONCE_LENGTH);
    IBECiphertext ibeCiphertext =
        cryptoWrapper
            .getCryptoUtilV3()
            .encryptInternal(cryptoWrapper.getCryptoUtilV3().getMpkG2(), identity, msg_orig);
    byte[] msg_dec =
        cryptoWrapper
            .getCryptoUtilV3()
            .decryptInternal(ibeCiphertext, secretKeyForIdentityG1, identity);
    if (msg_dec == null) {
      throw new RuntimeException("Health Authority could not verify Trace");
    }
  }

  private boolean doIntersect(long startTime1, long endTime1, long startTime2, long endTime2) {
    return startTime1 <= endTime2 && endTime1 >= startTime2;
  }

  private PreIdAndTimeKey getPreIdAndTimeKey(
      byte[] qrCodePayload, long startOfInterval, int intervalLength) {
    NoncesAndNotificationKey cryptoData =
        cryptoWrapper.getCryptoUtilV3().getNoncesAndNotificationKey(qrCodePayload);
    byte[] preId =
        cryptoWrapper.getCryptoUtilV3().cryptoHashSHA256(
            cryptoWrapper
                .getCryptoUtilV3()
                .concatenate(
                    "CN-PREID".getBytes(StandardCharsets.US_ASCII),
                    qrCodePayload,
                    cryptoData.noncePreId));

    byte[] timeKey =
        cryptoWrapper.getCryptoUtilV3().cryptoHashSHA256(
            cryptoWrapper
                .getCryptoUtilV3()
                .concatenate(
                    "CN-TIMEKEY".getBytes(StandardCharsets.US_ASCII),
                    cryptoWrapper.getCryptoUtilV3().intToBytes(intervalLength),
                    cryptoWrapper.getCryptoUtilV3().longToBytes(startOfInterval),
                    cryptoData.nonceTimekey));

    return new PreIdAndTimeKey(preId, timeKey);
  }

  /**
   * @param arrivalTime time since Unix Epoch in seconds
   * @param departureTime time since Unix Epoch in seconds
   * @return a List of Long containing all intervalStart values since UNIX epoch (in seconds) that
   *     intersect with the (arrivalTime, departureTime) interval.
   */
  private List<Long> getAffectedIntervalStarts(long arrivalTime, long departureTime) {
    long start = arrivalTime / INTERVAL_LENGTH;
    long end = departureTime / INTERVAL_LENGTH;
    List<Long> result = new ArrayList<>();
    for (long i = start; i <= end; i += 1) {
      result.add(i * INTERVAL_LENGTH);
    }
    return result;
  }

  /**
   * Generates Base64 encoded String of an Entry QR Code
   *
   * @throws InvalidProtocolBufferException
   * @throws UnsupportedEncodingException
   */
  public VenueInfo generateEntryQrCode(
      String description,
      String address,
      byte[] countryData,
      long validFrom,
      long validTo,
      byte[] masterPublicKey)
      throws UnsupportedEncodingException, InvalidProtocolBufferException {

    TraceLocation traceLocation =
        TraceLocation.newBuilder()
            .setVersion(QR_CODE_VERSION_3)
            .setStartTimestamp(validFrom)
            .setEndTimestamp(validTo)
            .setDescription(description)
            .setAddress(address)
            .build();

    CrowdNotifierData crowdNotifierData =
        CrowdNotifierData.newBuilder()
            .setVersion(QR_CODE_VERSION_3)
            .setCryptographicSeed(
                ByteString.copyFrom(cryptoWrapper.getCryptoUtilV3().createNonce(CRYPTOGRAPHIC_SEED_BYTES)))
            .setPublicKey(ByteString.copyFrom(masterPublicKey))
            .build();

    QRCodePayload qrCodePayload =
        QRCodePayload.newBuilder()
            .setVersion(QR_CODE_VERSION_3)
            .setCrowdNotifierData(crowdNotifierData)
            .setLocationData(traceLocation)
            .setCountryData(ByteString.copyFrom(countryData))
            .build();

    return getVenueInfoFromQrCode(qrCodePayload);
  }

  public static byte[] fromBase64(String base64) throws UnsupportedEncodingException {
    return Base64.getUrlDecoder().decode(base64.getBytes("UTF-8"));
  }

  public static String toBase64(byte[] bytes) throws UnsupportedEncodingException {
    return Base64.getUrlEncoder().encode(bytes).toString();
  }

  private class PreIdAndTimeKey {
    public final byte[] preId;
    public final byte[] timeKey;

    public PreIdAndTimeKey(byte[] preId, byte[] timeKey) {
      this.preId = preId;
      this.timeKey = timeKey;
    }
  }

  private VenueInfo getVenueInfoFromQrCode(QRCodePayload qrCodeEntry)
      throws UnsupportedEncodingException, InvalidProtocolBufferException {
    TraceLocation locationData = qrCodeEntry.getLocationData();
    CrowdNotifierData crowdNotifierData = qrCodeEntry.getCrowdNotifierData();

    NoncesAndNotificationKey cryptoData =
        cryptoWrapper.getCryptoUtilV3().getNoncesAndNotificationKey(qrCodeEntry);

    return new VenueInfo(
        locationData.getDescription(),
        locationData.getAddress(),
        cryptoData.notificationKey,
        crowdNotifierData.getPublicKey().toByteArray(),
        cryptoData.noncePreId,
        cryptoData.nonceTimekey,
        locationData.getStartTimestamp(),
        locationData.getEndTimestamp(),
        qrCodeEntry.toByteArray(),
        qrCodeEntry.getCountryData().toByteArray());
  }

  private class VenueInfo {
    private String description;
    private String address;
    private byte[] notificationKey;
    private byte[] publicKey;
    private byte[] noncePreId;
    private byte[] nonceTimekey;
    private long validFrom;
    private long validTo;
    private byte[] qrCodePayload;
    private byte[] countryData;

    public VenueInfo(
        String description,
        String address,
        byte[] notificationKey,
        byte[] publicKey,
        byte[] noncePreId,
        byte[] nonceTimekey,
        long validFrom,
        long validTo,
        byte[] qrCodePayload,
        byte[] countryData) {
      this.description = description;
      this.address = address;
      this.notificationKey = notificationKey;
      this.publicKey = publicKey;
      this.noncePreId = noncePreId;
      this.nonceTimekey = nonceTimekey;
      this.validFrom = validFrom;
      this.validTo = validTo;
      this.qrCodePayload = qrCodePayload;
      this.countryData = countryData;
    }

    public String getDescription() {
      return description;
    }

    public String getAddress() {
      return address;
    }

    public byte[] getNotificationKey() {
      return notificationKey;
    }

    public String getTitle() {
      return description;
    }

    public byte[] getNoncePreId() {
      return noncePreId;
    }

    public byte[] getNonceTimekey() {
      return nonceTimekey;
    }

    public long getValidFrom() {
      return validFrom;
    }

    public long getValidTo() {
      return validTo;
    }

    public byte[] getQrCodePayload() {
      return qrCodePayload;
    }

    public byte[] getPublicKey() {
      return publicKey;
    }

    public byte[] getCountryData() {
      return countryData;
    }

    public String toQrCodeString(String prefix) throws UnsupportedEncodingException {
      return prefix + "?v=3#" + toBase64(qrCodePayload);
    }
  }
}
