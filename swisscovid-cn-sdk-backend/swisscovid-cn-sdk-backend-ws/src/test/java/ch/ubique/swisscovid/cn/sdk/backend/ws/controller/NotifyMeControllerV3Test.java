package ch.ubique.swisscovid.cn.sdk.backend.ws.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.ubique.notifyme.sdk.backend.data.NotifyMeDataServiceV3;
import ch.ubique.notifyme.sdk.backend.model.UserUploadPayloadOuterClass.UploadVenueInfo;
import ch.ubique.notifyme.sdk.backend.model.UserUploadPayloadOuterClass.UserUploadPayload;
import ch.ubique.notifyme.sdk.backend.model.tracekey.v3.TraceKey;
import ch.ubique.notifyme.sdk.backend.model.v3.ProblematicEventWrapperOuterClass;
import ch.ubique.swisscovid.cn.sdk.backend.ws.util.TokenHelper;
import com.google.protobuf.ByteString;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;

import mockit.Mock;
import mockit.MockUp;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"dev", "jwt", "test-config"})
@TestPropertySource(
    properties = {
      "ws.app.jwt.publickey=classpath://generated_public_test.pem",
      "traceKey.bucketSizeInMs=1"
    })
public class NotifyMeControllerV3Test extends BaseControllerTest {

  private static boolean setUpIsDone = false;
  private final Charset charset = StandardCharsets.UTF_8;
  private final String identityString = "identity";
  private final String secretKey = "secret";
  private final String associatedData = "message";
  private final String cipherTextNonce = "nonce";
  private final Instant end = Instant.now();
  private final Instant start = end.minusSeconds(60 * 60);
  @Autowired NotifyMeControllerV3 notifyMeControllerV3;
  @Autowired NotifyMeDataServiceV3 notifyMeDataServiceV3;

  @Value("${traceKey.traceKeysCacheControlInMs}")
  Long traceKeysCacheControlInMs;

  @Value("${userupload.requestTime}")
  Long requestTime;

  private static TokenHelper tokenHelper;

  public NotifyMeControllerV3Test() {
    super(true);
  }

  private TraceKey getTraceKey() {
    TraceKey traceKey = new TraceKey();
    traceKey.setId(0);
    traceKey.setVersion(3);
    traceKey.setIdentity(identityString.getBytes(charset));
    traceKey.setSecretKeyForIdentity(secretKey.getBytes(charset));
    traceKey.setDay(start.truncatedTo(ChronoUnit.DAYS));
    traceKey.setCreatedAt(start.minusSeconds(60 * 60 * 3));
    traceKey.setEncryptedAssociatedData(associatedData.getBytes(charset));
    traceKey.setCipherTextNonce(cipherTextNonce.getBytes(charset));
    return traceKey;
  }

  @Before
  public void setUp() throws Exception {
    tokenHelper = new TokenHelper();
    if (!setUpIsDone) {
      final TraceKey traceKey = getTraceKey();
      notifyMeDataServiceV3.insertTraceKey(traceKey);
      setUpIsDone = true;
    }
  }

  @Test
  public void testHello() throws Exception {
    String hello =
        mockMvc
            .perform(get("/v3"))
            .andExpect(status().is2xxSuccessful())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertTrue(hello.contains("Hello from NotifyMe WS v3."));
  }

  @Test
  public void testGetTraceKeysJson() throws Exception {
    final MockHttpServletResponse response =
        mockMvc
            .perform(get("/v3/traceKeys").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().is2xxSuccessful())
            .andExpect(content().contentType("application/json"))
            .andReturn()
            .getResponse();
    assertTrue(
        response
            .getHeader("Cache-Control")
            .contains("max-age=" + (traceKeysCacheControlInMs / 1000)));
    final String content = response.getContentAsString();
    assertTrue(content.length() > 0);
  }

  @Test
  @Transactional
  public void testEmptyGetTraceKeys() throws Exception {
    final MockHttpServletResponse response =
        mockMvc
            .perform(get("/v3/traceKeys").accept("application/protobuf"))
            .andExpect(status().is2xxSuccessful())
            .andExpect(content().contentType("application/x-protobuf"))
            .andReturn()
            .getResponse();
    assertTrue(
        response
            .getHeader("Cache-Control")
            .contains("max-age=" + (traceKeysCacheControlInMs / 1000)));
    assertTrue(response.containsHeader("x-key-bundle-tag"));
    final byte[] content = response.getContentAsByteArray();
    final var wrapper =
        ProblematicEventWrapperOuterClass.ProblematicEventWrapper.parseFrom(content);
    assertEquals(0, wrapper.getEventsCount());
  }

  @Test
  @Transactional
  public void testUploadAndGetTraceKeys() throws Exception {
    final var now = LocalDateTime.now();
    final var payload = createUserUploadPayload(now.minusDays(1), now.minusHours(23));
    final byte[] payloadBytes = payload.toByteArray();
    final var expiry = now.plusMinutes(5).toInstant(ZoneOffset.UTC);
    final var token =
        tokenHelper.createToken(
            "2021-04-29", "0", "checkin", "userupload", Date.from(expiry), true, now.toInstant(ZoneOffset.UTC));

    final String userAgent = "ch.admin.bag.notifyMe.dev;1.0.7;1595591959493;Android;29";
    final var start = LocalDateTime.now();
    final var mvcResult =
        mockMvc
            .perform(
                post("/v3/userupload")
                    .contentType("application/x-protobuf")
                    .header("Authorization", "Bearer " + token)
                    .header("User-Agent", userAgent)
                    .content(payloadBytes))
            .andExpect(request().asyncStarted())
            .andReturn();
    mockMvc.perform(asyncDispatch(mvcResult)).andExpect(status().isOk());
    Clock clock = Clock.fixed(now.plusDays(1).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    new MockUp<Instant>() {
      @Mock
      public Instant now() {
        return Instant.now(clock);
      }
    };
    final MockHttpServletResponse response =
        mockMvc
            .perform(get("/v3/traceKeys").accept("application/protobuf"))
            .andExpect(status().is2xxSuccessful())
            .andExpect(content().contentType("application/x-protobuf"))
            .andReturn()
            .getResponse();
    assertTrue(
        response
            .getHeader("Cache-Control")
            .contains("max-age=" + (traceKeysCacheControlInMs / 1000)));
    assertTrue(response.containsHeader("x-key-bundle-tag"));
    final byte[] content = response.getContentAsByteArray();
    final var wrapper =
        ProblematicEventWrapperOuterClass.ProblematicEventWrapper.parseFrom(content);
    assertEquals(1, wrapper.getEventsCount());
    final var event = wrapper.getEvents(0);
    assertNotNull(event);
  }

  @Test
  @Transactional
  public void testUserUploadValidToken() throws Exception {
    final var now = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS);
    final var initSize = notifyMeDataServiceV3.findTraceKeys(now.minusDays(1).toInstant(ZoneOffset.UTC)).size();
    final var payload = createUserUploadPayload(now.minusDays(1), now.minusHours(23));
    final byte[] payloadBytes = payload.toByteArray();
    final var expiry = now.plusMinutes(5).toInstant(ZoneOffset.UTC);
    final var token =
        tokenHelper.createToken(
            "2021-04-29", "0", "checkin", "userupload", Date.from(expiry), true, now.toInstant(ZoneOffset.UTC));
    final String userAgent = "ch.admin.bag.notifyMe.dev;1.0.7;1595591959493;Android;29";
    final var start = LocalDateTime.now();
    final var mvcResult =
        mockMvc
            .perform(
                post("/v3/userupload")
                    .contentType("application/x-protobuf")
                    .header("Authorization", "Bearer " + token)
                    .header("User-Agent", userAgent)
                    .content(payloadBytes))
            .andExpect(request().asyncStarted())
            .andReturn();
    mockMvc.perform(asyncDispatch(mvcResult)).andExpect(status().isOk());
    final var duration = start.until(LocalDateTime.now(), ChronoUnit.MILLIS);
    Clock clock = Clock.fixed(now.plusDays(1).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    new MockUp<Instant>() {
      @Mock
      public Instant now() {
        return Instant.now(clock);
      }
    };
    final var traceKeys = notifyMeDataServiceV3.findTraceKeys(now.minusDays(1).toInstant(ZoneOffset.UTC));
    assertEquals(initSize + 1, traceKeys.size());
    assertTrue(requestTime <= duration);
  }

  @Test
  @Transactional
  public void testUserUploadInvalidTokenSig() throws Exception {
    final var now = LocalDateTime.now();
    final var payload = createUserUploadPayload(now.minusDays(1), now.minusHours(12));
    final byte[] payloadBytes = payload.toByteArray();
    final var expiry = now.plusMinutes(120).toInstant(ZoneOffset.UTC);
    final var token =
        tokenHelper.createToken(
            "2021-04-29", "0", "checkin", "userupload", Date.from(expiry), false, now.toInstant(ZoneOffset.UTC));
    final String userAgent = "ch.admin.bag.notifyMe.dev;1.0.7;1595591959493;Android;29";
    final var result =
        mockMvc
            .perform(
                post("/v3/userupload")
                    .contentType("application/x-protobuf")
                    .header("Authorization", "Bearer " + token)
                    .header("User-Agent", userAgent)
                    .content(payloadBytes))
            .andExpect(request().asyncNotStarted())
            .andExpect(status().is(401))
            .andReturn();
    String authenticationError = result.getResponse().getHeader("www-authenticate");
    assertTrue(authenticationError.contains("Bearer"));
  }

  @Test
  @Transactional
  public void testUserUploadVisitFilters() throws Exception {
    final var now = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS);
    final var initSize = notifyMeDataServiceV3.findTraceKeys(now.minusDays(1).toInstant(ZoneOffset.UTC)).size();
    final var fakeUpload = getUploadVenueInfo(now.minusDays(3), now.minusHours(60), true);
    final var invalidIntervalUpload = getUploadVenueInfo(now.minusHours(59), now.minusHours(34), false);
    final var validUpload = getUploadVenueInfo(now.minusHours(12), now.minusHours(11), false);
    final var payload = getUserUploadPayload(fakeUpload, invalidIntervalUpload, validUpload);
    final byte[] payloadBytes = payload.toByteArray();
    final var expiry = now.plusMinutes(5).toInstant(ZoneOffset.UTC);
    final var token =
            tokenHelper.createToken(
                    "2021-04-29", "0", "checkin", "userupload", Date.from(expiry), true, now.toInstant(ZoneOffset.UTC));
    final String userAgent = "ch.admin.bag.notifyMe.dev;1.0.7;1595591959493;Android;29";
    final var start = LocalDateTime.now();
    final var mvcResult =
            mockMvc
                    .perform(
                            post("/v3/userupload")
                                    .contentType("application/x-protobuf")
                                    .header("Authorization", "Bearer " + token)
                                    .header("User-Agent", userAgent)
                                    .content(payloadBytes))
                    .andExpect(request().asyncStarted())
                    .andReturn();
    mockMvc.perform(asyncDispatch(mvcResult)).andExpect(status().isOk());
    final var duration = start.until(LocalDateTime.now(), ChronoUnit.MILLIS);
    Clock clock = Clock.fixed(now.plusDays(1).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    new MockUp<Instant>() {
      @Mock
      public Instant now() {
        return Instant.now(clock);
      }
    };
    final var traceKeys = notifyMeDataServiceV3.findTraceKeys(now.minusDays(1).toInstant(ZoneOffset.UTC));
    assertEquals(initSize + 1, traceKeys.size());
    assertTrue(requestTime <= duration);
  }

  /**
   * Creates simple user upload payload with one venue info
   *
   * @return
   */
  private UserUploadPayload createUserUploadPayload(LocalDateTime start, LocalDateTime end) {
    UploadVenueInfo venueInfo = getUploadVenueInfo(start, end, false);
    return getUserUploadPayload(venueInfo);
  }

  private UserUploadPayload getUserUploadPayload(UploadVenueInfo... venueInfo) {
    final var userUpload =
        UserUploadPayload.newBuilder().setVersion(3).addAllVenueInfos(Arrays.asList(venueInfo)).build();
    return userUpload;
  }

  private UploadVenueInfo getUploadVenueInfo(LocalDateTime start, LocalDateTime end, boolean fake) {
    final var from = start.toInstant(ZoneOffset.UTC);
    final var to = end.toInstant(ZoneOffset.UTC);
    var venueInfo =
        UploadVenueInfo.newBuilder()
            .setFake(fake)
            .setPreId(ByteString.copyFromUtf8("preId"))
            .setNotificationKey(ByteString.copyFromUtf8("notificationKey"))
            .setTimeKey(ByteString.copyFromUtf8("timeKey"))
            .setIntervalStartMs(from.toEpochMilli())
            .setIntervalEndMs(to.toEpochMilli())
            .build();
    return venueInfo;
  }
}
