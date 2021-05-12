package ch.ubique.notifyme.sdk.backend.ws.insert_manager.insertion_filters;

import ch.ubique.notifyme.sdk.backend.model.UserUploadPayloadOuterClass.UploadVenueInfo;
import ch.ubique.notifyme.sdk.backend.ws.insert_manager.OSType;
import ch.ubique.notifyme.sdk.backend.ws.semver.Version;
import ch.ubique.notifyme.sdk.backend.ws.util.CryptoWrapper;
import ch.ubique.notifyme.sdk.backend.ws.util.TokenHelper;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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

  abstract UploadVenueInfo getValidVenueInfo();

  abstract UploadVenueInfo getInvalidVenueInfo();

  abstract UploadInsertionFilter insertionFilter();

  public Jwt getToken(LocalDateTime now) throws Exception {
    final var onset = now.minusDays(5).truncatedTo(ChronoUnit.DAYS).format(DATE_FORMATTER);
    final var expiry = now.plusMinutes(5).toInstant(ZoneOffset.UTC);
    return jwtDecoder.decode(tokenHelper.createToken(onset, "0", "notifyMe", "userupload", Date.from(expiry), true));
  }

  @Before
  public void setUp() throws Exception {
    tokenHelper = new TokenHelper();
  }

  @Test
  public void testFilterValid() throws Exception {
    LocalDateTime now = LocalDateTime.now();
    final List<UploadVenueInfo> uploadVenueInfoList = new ArrayList<>();
    uploadVenueInfoList.add(getValidVenueInfo());
    final var osType = OSType.ANDROID;
    final var osVersion = new Version("29");
    final var appVersion = new Version("1.0.0+0");
    final var token = getToken(now);
    assertFalse(
        insertionFilter()
            .filter(now, uploadVenueInfoList, osType, osVersion, appVersion, token)
            .isEmpty());
  }

  @Test
  public void testFilterInvalid() throws Exception {
    LocalDateTime now = LocalDateTime.now();
    final List<UploadVenueInfo> uploadVenueInfoList = new ArrayList<>();
    uploadVenueInfoList.add(getInvalidVenueInfo());
    final var osType = OSType.ANDROID;
    final var osVersion = new Version("29");
    final var appVersion = new Version("1.0.0+0");
    final var token = getToken(now);
    assertTrue(
        insertionFilter()
            .filter(now, uploadVenueInfoList, osType, osVersion, appVersion, token)
            .isEmpty());
  }
}
