package ch.ubique.notifyme.sdk.backend.ws.controller;

import ch.ubique.notifyme.sdk.backend.data.NotifyMeDataServiceV3;
import ch.ubique.notifyme.sdk.backend.model.v3.ProblematicEventWrapperOuterClass;
import ch.ubique.notifyme.sdk.backend.model.tracekey.v3.TraceKey;
import ch.ubique.notifyme.sdk.backend.model.UserUploadPayloadOuterClass.UserUploadPayload;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import com.google.protobuf.ByteString;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"dev", "enable-nmControllerV3"})
public class NotifyMeControllerV3Test extends BaseControllerTest {

  @Autowired NotifyMeControllerV3 notifyMeControllerV3;
  @Autowired NotifyMeDataServiceV3 notifyMeDataServiceV3;

  @Value("${traceKey.traceKeysCacheControlInMs}")
  Long traceKeysCacheControlInMs;

  private static boolean setUpIsDone = false;

  private final Charset charset = StandardCharsets.UTF_8;
  private final String identityString = "identity";
  private final String secretKey = "secret";
  private final String associatedData = "message";
  private final String cipherTextNonce = "nonce";
  private final Instant end = Instant.now();
  private final Instant start = end.minusSeconds(60 * 60);

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
  public void setUp() {
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
  public void testUserUpload() throws Exception {
    // TODO: Add more sensible identities once specified
    final var payload = UserUploadPayload.newBuilder().setVersion(3).addIdentities(ByteString.copyFromUtf8("hello")).build();
    final byte[] bytes = payload.toByteArray();
    mockMvc.perform(post("/v3/userupload").contentType("application/x-protobuf").content(bytes)).andExpect(status().isOk());
  }

}

