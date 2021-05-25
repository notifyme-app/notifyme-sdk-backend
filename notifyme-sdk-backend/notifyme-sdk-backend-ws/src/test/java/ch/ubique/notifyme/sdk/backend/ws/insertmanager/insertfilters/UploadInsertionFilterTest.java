package ch.ubique.notifyme.sdk.backend.ws.insertmanager.insertfilters;

import ch.ubique.notifyme.sdk.backend.model.UserUploadPayloadOuterClass.UploadVenueInfo;
import ch.ubique.notifyme.sdk.backend.ws.insertmanager.OSType;
import ch.ubique.notifyme.sdk.backend.ws.semver.Version;
import ch.ubique.notifyme.sdk.backend.ws.util.CryptoWrapper;
import ch.ubique.notifyme.sdk.backend.ws.util.TokenHelper;
import com.google.protobuf.ByteString;
import java.nio.charset.StandardCharsets;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles({"dev", "test-config", "jwt"})
@TestPropertySource(properties = {"ws.app.jwt.publickey=classpath://generated_public_test.pem"})
public abstract class UploadInsertionFilterTest {
  protected static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofPattern("YYYY-MM-dd");
  protected TokenHelper tokenHelper;
  @Autowired CryptoWrapper cryptoWrapper;
  @Autowired JwtDecoder jwtDecoder;

  abstract List<UploadVenueInfo> getValidVenueInfo();

  abstract List<UploadVenueInfo> getInvalidVenueInfo();

  abstract UploadInsertionFilter insertionFilter();

  public Jwt getToken(LocalDateTime now) throws Exception {
    final var onset = now.minusDays(5).truncatedTo(ChronoUnit.DAYS).format(DATE_FORMATTER);
    final var expiry = now.plusMinutes(5).toInstant(ZoneOffset.UTC);
    return jwtDecoder.decode(tokenHelper.createToken(onset, "0", "notifyMe", "userupload", Date.from(expiry), true, now.toInstant(ZoneOffset.UTC)));
  }

  @Before
  public void setUp() throws Exception {
    tokenHelper = new TokenHelper();
  }

  @Test
  public void testFilterValid() throws Exception {
    LocalDateTime now = LocalDateTime.now();
    final List<UploadVenueInfo> uploadVenueInfoList = getValidVenueInfo();
    final var token = getToken(now);
    assertEquals(uploadVenueInfoList.size(),
        insertionFilter()
            .filter(now, uploadVenueInfoList, token)
            .size());
  }

  @Test
  public void testFilterInvalid() throws Exception {
    LocalDateTime now = LocalDateTime.now();
    final List<UploadVenueInfo> uploadVenueInfoList = getInvalidVenueInfo();
    final var token = getToken(now);
    assertTrue(
        insertionFilter()
            .filter(now, uploadVenueInfoList, token)
            .isEmpty());
  }

  public UploadVenueInfo getVenueInfo(LocalDateTime start, LocalDateTime end) {
    return getVenueInfo(start, end, false);
  }

  public UploadVenueInfo getVenueInfo(boolean fake) {
    LocalDateTime start = LocalDateTime.now();
    LocalDateTime end = start.plusMinutes(30);
    return getVenueInfo(start, end, fake);
  }

  public UploadVenueInfo getVenueInfo(
      LocalDateTime start, LocalDateTime end, boolean fake) {
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
        .setFake(fake)
        .build();
  }
}
