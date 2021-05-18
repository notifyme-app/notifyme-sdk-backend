package ch.ubique.notifyme.sdk.backend.ws.insertmanager;

import ch.ubique.notifyme.sdk.backend.data.NotifyMeDataServiceV3;
import ch.ubique.notifyme.sdk.backend.model.UserUploadPayloadOuterClass.UploadVenueInfo;
import ch.ubique.notifyme.sdk.backend.ws.insertmanager.insertfilters.FakeRequestFilter;
import ch.ubique.notifyme.sdk.backend.ws.insertmanager.insertfilters.IntervalThresholdFilter;
import ch.ubique.notifyme.sdk.backend.ws.insertmanager.insertfilters.OverlappingIntervalsFilter;
import ch.ubique.notifyme.sdk.backend.ws.insertmanager.insertfilters.UploadInsertionFilter;
import ch.ubique.notifyme.sdk.backend.ws.semver.Version;
import ch.ubique.notifyme.sdk.backend.ws.util.CryptoWrapper;
import ch.ubique.notifyme.sdk.backend.ws.util.TokenHelper;
import com.google.protobuf.ByteString;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles({"dev", "test-config"})
public class InsertManagerTest {

  InsertManager insertManager;

  @Autowired NotifyMeDataServiceV3 notifyMeDataServiceV3;
  @Autowired CryptoWrapper cryptoWrapper;

  @Autowired TransactionManager transactionManager;

  private TokenHelper tokenHelper;

  @Before
  public void setUp() throws Exception {
    tokenHelper = new TokenHelper();
    insertManager = new InsertManager(cryptoWrapper, notifyMeDataServiceV3);
  }

  @Test
  @Transactional
  public void testInsertEmptyList() throws Exception {
    final LocalDateTime now = LocalDateTime.now();
    assertTrue(
        notifyMeDataServiceV3.findTraceKeys(now.minusDays(1).toInstant(ZoneOffset.UTC)).isEmpty());
    insertWith(new ArrayList<>(), new ArrayList<>(), now);
    assertTrue(
        notifyMeDataServiceV3.findTraceKeys(now.minusDays(1).toInstant(ZoneOffset.UTC)).isEmpty());
  }

  @Test
  @Transactional
  public void testInsertInvalidVenueInfo() throws Exception {
    final LocalDateTime now = LocalDateTime.now();
    UploadInsertionFilter removeAll =
        new UploadInsertionFilter() {
          @Override
          public List<UploadVenueInfo> filter(
              LocalDateTime now,
              List<UploadVenueInfo> uploadVenueInfoList,
              OSType osType,
              Version osVersion,
              Version appVersion,
              Object principal)
              throws InsertException {
            return new ArrayList<>();
          }
        };
    final List<UploadVenueInfo> uploadVenueInfoList = new ArrayList<>();
    uploadVenueInfoList.add(
        createUploadVenueInfo(
            now, now.plusMinutes(60), false));
    insertWith(Collections.singletonList(removeAll), uploadVenueInfoList, now);
    assertTrue(
        notifyMeDataServiceV3.findTraceKeys(now.minusDays(1).toInstant(ZoneOffset.UTC)).isEmpty());
  }

  @Test
  @Transactional
  public void testInsertValid() throws Exception {
    final LocalDateTime now = LocalDateTime.now();
    UploadInsertionFilter removeNone =
        new UploadInsertionFilter() {
          @Override
          public List<UploadVenueInfo> filter(
              LocalDateTime now,
              List<UploadVenueInfo> uploadVenueInfoList,
              OSType osType,
              Version osVersion,
              Version appVersion,
              Object principal)
              throws InsertException {
            return uploadVenueInfoList;
          }
        };
    final List<UploadVenueInfo> uploadVenueInfoList = new ArrayList<>();
    uploadVenueInfoList.add(
        createUploadVenueInfo(
            now, now.plusMinutes(60), false));
    insertWith(Collections.singletonList(removeNone), uploadVenueInfoList, now);
    assertEquals(1,
        notifyMeDataServiceV3.findTraceKeys(now.minusDays(1).toInstant(ZoneOffset.UTC)).size());
  }

  @Test
  @Transactional
  public void testAddRequestFilters() throws Exception {
    final LocalDateTime now = LocalDateTime.now();
    final var venueInfoList = new ArrayList<UploadVenueInfo>();
    final var fakeUpload = createUploadVenueInfo(now.minusDays(6), now.minusDays(5), true);
    venueInfoList.add(fakeUpload);
    final var negativeIntervalUpload = createUploadVenueInfo(now.minusDays(3), now.minusDays(4), false);
    venueInfoList.add(negativeIntervalUpload);
    final var overlapIntervalUpload1 = createUploadVenueInfo(now.minusHours(60), now.minusDays(2), false);
    final var overlapIntervalUpload2 = createUploadVenueInfo(now.minusHours(54), now.minusHours(36), false);
    venueInfoList.add(overlapIntervalUpload1);
    venueInfoList.add(overlapIntervalUpload2);
    final var validUpload = createUploadVenueInfo(now.minusHours(24), now.minusHours(23), false);
    venueInfoList.add(validUpload);
    insertWith(Arrays.asList(new FakeRequestFilter(), new IntervalThresholdFilter(), new OverlappingIntervalsFilter()), venueInfoList, now);
    assertEquals(1, notifyMeDataServiceV3.findTraceKeys(now.minusDays(1).toInstant(ZoneOffset.UTC)).size());
  }

  private void insertWith(
          List<UploadInsertionFilter> insertionFilterList,
          List<UploadVenueInfo> uploadVenueInfoList,
          LocalDateTime now)
      throws Exception {

    for(var insertionFilter: insertionFilterList) {
      if (insertionFilter != null) {
        insertManager.addFilter(insertionFilter);
      }
    }
    final String userAgent = "ch.admin.bag.notifyMe.dev;1.0.7;1595591959493;Android;29";
    final var expiry = LocalDateTime.now().plusMinutes(5).toInstant(ZoneOffset.UTC);
    final var token =
        tokenHelper.createToken(
            "2021-04-29", "0", "notifyMe", "userupload", Date.from(expiry), true, Instant.now());
    insertManager.insertIntoDatabase(uploadVenueInfoList, userAgent, token, now);
  }

  private UploadVenueInfo createUploadVenueInfo(LocalDateTime start, LocalDateTime end, boolean fake) {
    final var startInstant = start.toInstant(ZoneOffset.UTC);
    final var endInstant = end.toInstant(ZoneOffset.UTC);
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
                crypto.longToBytes(startInstant.getEpochSecond()),
                noncesAndNotificationKey.nonceTimekey));
    return UploadVenueInfo.newBuilder()
        .setPreId(ByteString.copyFrom(preid))
        .setTimeKey(ByteString.copyFrom(timekey))
        .setIntervalStartMs(startInstant.toEpochMilli())
        .setIntervalEndMs(endInstant.toEpochMilli())
        .setNotificationKey(ByteString.copyFrom(noncesAndNotificationKey.notificationKey))
        .setFake(fake)
        .build();
  }
}
