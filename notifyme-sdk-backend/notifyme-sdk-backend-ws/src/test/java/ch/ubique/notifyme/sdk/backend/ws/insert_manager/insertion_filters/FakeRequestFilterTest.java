package ch.ubique.notifyme.sdk.backend.ws.insert_manager.insertion_filters;

import ch.ubique.notifyme.sdk.backend.model.UserUploadPayloadOuterClass.UploadVenueInfo;
import com.google.protobuf.ByteString;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class FakeRequestFilterTest extends UploadInsertionFilterTest {
  @Override
  UploadVenueInfo getValidVenueInfo() {
    final UploadVenueInfo.Builder builder = getBuilder();
    return builder.setFake(false).build();
  }

  @Override
  UploadVenueInfo getInvalidVenueInfo() {
    final UploadVenueInfo.Builder builder = getBuilder();
    return builder.setFake(true).build();
  }

  @Override
  UploadInsertionFilter insertionFilter() {
    return new FakeRequestFilter();
  }

  private UploadVenueInfo.Builder getBuilder() {
      LocalDateTime start = LocalDateTime.now();
      LocalDateTime end = start.plusMinutes(30);
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
    final var builder =
        UploadVenueInfo.newBuilder()
            .setPreId(ByteString.copyFrom(preid))
            .setTimeKey(ByteString.copyFrom(timekey))
            .setIntervalStartMs(start.toInstant(ZoneOffset.UTC).getEpochSecond())
            .setIntervalEndMs(end.toInstant(ZoneOffset.UTC).getEpochSecond())
            .setNotificationKey(ByteString.copyFrom(noncesAndNotificationKey.notificationKey));
    return builder;
  }
}
