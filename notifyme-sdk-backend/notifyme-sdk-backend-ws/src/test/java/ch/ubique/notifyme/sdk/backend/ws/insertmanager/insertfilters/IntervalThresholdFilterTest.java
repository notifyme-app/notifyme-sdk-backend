package ch.ubique.notifyme.sdk.backend.ws.insertmanager.insertfilters;

import ch.ubique.notifyme.sdk.backend.model.UserUploadPayloadOuterClass.UploadVenueInfo;
import com.google.protobuf.ByteString;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

public class IntervalThresholdFilterTest extends UploadInsertionFilterTest {
  @Override
  List<UploadVenueInfo> getValidVenueInfo() {
    final var venueInfoList = new ArrayList<UploadVenueInfo>();
    LocalDateTime start = LocalDateTime.now();
    LocalDateTime end = start.plusMinutes(30);
    // Not an edge case
    final var venueInfoCase1 = getVenueInfo(start, end);
    venueInfoList.add(venueInfoCase1);
    start = LocalDateTime.now();
    end = start.plusHours(1);
    // Exactly 1 hour
    final var venueInfoCase2 = getVenueInfo(start, end);
    venueInfoList.add(venueInfoCase2);
    return venueInfoList;
  }

  @Override
  List<UploadVenueInfo> getInvalidVenueInfo() {
    final var venueInfoList = new ArrayList<UploadVenueInfo>();
    LocalDateTime start = LocalDateTime.now();
    LocalDateTime end = start.plusHours(-1);
    // End < Start
    final var venueInfoCase1 = getVenueInfo(start, end);
    venueInfoList.add(venueInfoCase1);
    start = LocalDateTime.now();
    end = start.plusHours(25);
    // End - Start > 24
    final var venueInfoCase2 = getVenueInfo(start, end);
    venueInfoList.add(venueInfoCase2);
    return venueInfoList;
  }

  @Override
  UploadInsertionFilter insertionFilter() {
    return new IntervalThresholdFilter();
  }

  private UploadVenueInfo getVenueInfo(
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
    return UploadVenueInfo.newBuilder()
        .setPreId(ByteString.copyFrom(preid))
        .setTimeKey(ByteString.copyFrom(timekey))
        .setIntervalStartMs(start.toInstant(ZoneOffset.UTC).toEpochMilli())
        .setIntervalEndMs(end.toInstant(ZoneOffset.UTC).toEpochMilli())
        .setNotificationKey(ByteString.copyFrom(noncesAndNotificationKey.notificationKey))
        .setFake(false)
        .build();
  }
}
