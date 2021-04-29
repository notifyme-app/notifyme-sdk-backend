package ch.ubique.notifyme.sdk.backend.ws.controller;

import ch.ubique.notifyme.sdk.backend.data.NotifyMeDataServiceV3;
import ch.ubique.notifyme.sdk.backend.model.UserUploadPayloadOuterClass.UserUploadPayload;
import ch.ubique.notifyme.sdk.backend.model.tracekey.v3.TraceKey;
import ch.ubique.notifyme.sdk.backend.model.v3.ProblematicEventWrapperOuterClass;
import ch.ubique.notifyme.sdk.backend.ws.util.TokenHelper;
import com.google.protobuf.ByteString;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"dev", "jwt"})
@TestPropertySource(properties = {"ws.app.jwt.publickey=classpath://generated_public_test.pem"})
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

  private TraceKey getTraceKey() {
    TraceKey traceKey = new TraceKey();
    traceKey.setId(0);
    traceKey.setVersion(3);
    traceKey.setIdentity(identityString.getBytes(charset));
    traceKey.setSecretKeyForIdentity(secretKey.getBytes(charset));
    traceKey.setStartTime(start);
    traceKey.setEndTime(end);
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
  public void testGetTraceKeys() throws Exception {
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
    assertEquals(identityString, event.getIdentity().toString(charset));
    assertEquals(secretKey, event.getSecretKeyForIdentity().toString(charset));
    assertEquals(associatedData, event.getEncryptedAssociatedData().toString(charset));
    assertEquals(cipherTextNonce, event.getCipherTextNonce().toString(charset));
  }

  @Test
  public void testUserUploadDuration() throws Exception {
    final var payload =
        UserUploadPayload.newBuilder()
            .setVersion(3)
            .addIdentities(ByteString.copyFromUtf8("hello"))
            .build();
    final byte[] payloadBytes = payload.toByteArray();
    final var expiry = LocalDateTime.now().plusMinutes(5).toInstant(ZoneOffset.UTC);
    final var token =
        tokenHelper.createToken("2021-04-29", "0", "notifyMe", "userupload", Date.from(expiry), true);

    final var start = LocalDateTime.now();
    final var mvcResult =
        mockMvc
            .perform(
                post("/v3/userupload")
                    .contentType("application/x-protobuf")
                    .header("Authorization", "Bearer " + token)
                    .content(payloadBytes))
            .andExpect(request().asyncStarted())
            .andReturn();
    mockMvc.perform(asyncDispatch(mvcResult)).andExpect(status().isOk());
    final var duration = start.until(LocalDateTime.now(), ChronoUnit.MILLIS);

    assertTrue(requestTime <= duration);
  }

  @Test
  public void testUserUploadValidToken() throws Exception {
    final var payload =
        UserUploadPayload.newBuilder()
            .setVersion(3)
            .addIdentities(ByteString.copyFromUtf8("hello"))
            .build();
    final byte[] payloadBytes = payload.toByteArray();
    final var expiry = LocalDateTime.now().plusMinutes(5).toInstant(ZoneOffset.UTC);
    final var token =
        tokenHelper.createToken("2021-04-29", "0", "notifyMe", "userupload", Date.from(expiry), true);

    final var mvcResult =
        mockMvc
            .perform(
                post("/v3/userupload")
                    .contentType("application/x-protobuf")
                    .header("Authorization", "Bearer " + token)
                    .content(payloadBytes))
            .andExpect(request().asyncStarted())
            .andReturn();
    mockMvc.perform(asyncDispatch(mvcResult)).andExpect(status().isOk());
  }

  @Test
  public void testUserUploadInvalidToken() throws Exception {
    final var payload =
        UserUploadPayload.newBuilder()
            .setVersion(3)
            .addIdentities(ByteString.copyFromUtf8("hello"))
            .build();
    final byte[] payloadBytes = payload.toByteArray();
    final var expiry = LocalDateTime.now().plusMinutes(120).toInstant(ZoneOffset.UTC);
    final var token =
        tokenHelper.createToken("2021-04-29", "0", "notifyMe", "userupload", Date.from(expiry), false);

    final var result =
        mockMvc
            .perform(
                post("/v3/userupload")
                    .contentType("application/x-protobuf")
                    .header("Authorization", "Bearer " + token)
                    .content(payloadBytes))
                    .andExpect(request().asyncNotStarted())
                    .andExpect(status().is(401))
            .andReturn();
    String authenticationError = result.getResponse().getHeader("www-authenticate");
    assertTrue(authenticationError.contains("Bearer"));
  }
}
