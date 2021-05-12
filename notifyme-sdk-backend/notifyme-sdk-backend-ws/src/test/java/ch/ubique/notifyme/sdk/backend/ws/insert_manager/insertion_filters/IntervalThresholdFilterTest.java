package ch.ubique.notifyme.sdk.backend.ws.insert_manager.insertion_filters;

import ch.ubique.notifyme.sdk.backend.model.UserUploadPayloadOuterClass;
import com.google.protobuf.ByteString;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class IntervalThresholdFilterTest extends UploadInsertionFilterTest {
  @Override
  UserUploadPayloadOuterClass.UploadVenueInfo getValidVenueInfo() {
    LocalDateTime start = LocalDateTime.now();
    LocalDateTime end = start.plusHours(2);
    return getVenueInfo(start, end);
  }

  @Override
  UserUploadPayloadOuterClass.UploadVenueInfo getInvalidVenueInfo() {
    LocalDateTime start = LocalDateTime.now();
    LocalDateTime end = start.plusHours(-1);
    return getVenueInfo(start, end);
  }

  @Override
  UploadInsertionFilter insertionFilter() {
    return new IntervalThresholdFilter();
  }

  private UserUploadPayloadOuterClass.UploadVenueInfo getVenueInfo(
      LocalDateTime start, LocalDateTime end) {
    final var crypto = cryptoWrapper.getCryptoUtilV3();
    final var noncesAndNotificationKey =
        crypto.getNoncesAndNotificationKey(crypto.createNonce(256));
    byte[] preid =
        crypto.cryptoHashSHA256(
            crypto.concatenate(
                "CN-PREID".getBytes(StandardCharsets.US_ASCII),
                "payload".getBytes(StandardCharsets.US_ASCII),
                noncesAndNotificationKey.noncePreId));
    byte[] timekey =
        crypto.cryptoHashSHA256(
            crypto.concatenate(
                "CN-TIMEKEY".getBytes(StandardCharsets.US_ASCII),
                crypto.longToBytes(3600L),
                crypto.longToBytes(start.toInstant(ZoneOffset.UTC).getEpochSecond()),
                noncesAndNotificationKey.nonceTimekey));
    // TODO: What happens when we don't set fields?
    return UserUploadPayloadOuterClass.UploadVenueInfo.newBuilder()
        .setPreId(ByteString.copyFrom(preid))
        .setTimeKey(ByteString.copyFrom(timekey))
        .setIntervalStartMs(start.toInstant(ZoneOffset.UTC).getEpochSecond())
        .setIntervalEndMs(end.toInstant(ZoneOffset.UTC).getEpochSecond())
        .setNotificationKey(ByteString.copyFrom(noncesAndNotificationKey.notificationKey))
        .setFake(false)
        .build();
  }
}
