package ch.ubique.notifyme.sdk.backend.ws.insert_manager;

import ch.ubique.notifyme.sdk.backend.data.NotifyMeDataServiceV3;
import ch.ubique.notifyme.sdk.backend.model.UserUploadPayloadOuterClass.UploadVenueInfo;
import ch.ubique.notifyme.sdk.backend.ws.config.WSDevConfig;
import ch.ubique.notifyme.sdk.backend.ws.insert_manager.insertion_filters.UploadInsertionFilter;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles({"dev", "test-config"})
public class InsertManagerTest {

  InsertManager insertManager;

  @Autowired NotifyMeDataServiceV3 notifyMeDataServiceV3;
  @Autowired CryptoWrapper cryptoWrapper;

  @Autowired
  TransactionManager transactionManager;

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
    assertTrue(notifyMeDataServiceV3.findTraceKeys(now.minusDays(1).toInstant(ZoneOffset.UTC)).isEmpty());
    insertWith(null, new ArrayList<>(), now);
    assertTrue(notifyMeDataServiceV3.findTraceKeys(now.minusDays(1).toInstant(ZoneOffset.UTC)).isEmpty());
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
                    now.toInstant(ZoneOffset.UTC), now.plusMinutes(60).toInstant(ZoneOffset.UTC)));
    insertWith(removeAll, uploadVenueInfoList, now);
    assertTrue(notifyMeDataServiceV3.findTraceKeys(now.minusDays(1).toInstant(ZoneOffset.UTC)).isEmpty());
  }

  @Test
  @Transactional
  public void testInsertInvalidUserAgent() throws Exception {
    // TODO: Add filter that removes any upload
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
                    now.toInstant(ZoneOffset.UTC), now.plusMinutes(60).toInstant(ZoneOffset.UTC)));
    insertWith(removeNone, uploadVenueInfoList, now);
    assertFalse(notifyMeDataServiceV3.findTraceKeys(now.minusDays(1).toInstant(ZoneOffset.UTC)).isEmpty());
  }

  private void insertWith(UploadInsertionFilter insertionFilter, List<UploadVenueInfo> uploadVenueInfoList, LocalDateTime now) throws Exception {
    if (insertionFilter != null) {
      insertManager.addFilter(insertionFilter);
    }
    final String userAgent = "ch.admin.bag.notifyMe.dev;1.0.7;1595591959493;Android;29";
    final var expiry = LocalDateTime.now().plusMinutes(5).toInstant(ZoneOffset.UTC);
    final var token =
        tokenHelper.createToken(
            "2021-04-29", "0", "notifyMe", "userupload", Date.from(expiry), true);
    insertManager.insertIntoDatabase(uploadVenueInfoList, userAgent, token, now);
  }

  private UploadVenueInfo createUploadVenueInfo(Instant start, Instant end) {
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
                crypto.longToBytes(start.getEpochSecond()),
                noncesAndNotificationKey.nonceTimekey));
    return UploadVenueInfo.newBuilder()
        .setPreId(ByteString.copyFrom(preid))
        .setTimeKey(ByteString.copyFrom(timekey))
        .setIntervalStartMs(start.getEpochSecond())
        .setIntervalEndMs(end.getEpochSecond())
        .setNotificationKey(ByteString.copyFrom(noncesAndNotificationKey.notificationKey))
        .build();
  }
}
