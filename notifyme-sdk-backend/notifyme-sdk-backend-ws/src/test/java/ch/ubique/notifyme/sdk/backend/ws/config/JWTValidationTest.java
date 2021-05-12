package ch.ubique.notifyme.sdk.backend.ws.config;

import static org.junit.Assert.assertThrows;

import ch.ubique.notifyme.sdk.backend.data.UUIDDataService;
import ch.ubique.notifyme.sdk.backend.ws.util.TokenHelper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles({"dev", "jwt"})
@TestPropertySource(properties = {"ws.app.jwt.publickey=classpath://generated_public_test.pem"})
public class JWTValidationTest {

  @Autowired UUIDDataService uuidDataService;

  @Autowired JwtDecoder jwtDecoder;
  TokenHelper tokenHelper;

  @Before
  public void setup() throws Exception {
    tokenHelper = new TokenHelper();
  }

  @Test
  public void testDecoderValid() throws Exception {
    final var now = LocalDateTime.now();
    final var expiry = now.plusMinutes(5).toInstant(ZoneOffset.UTC);
    final var accessToken =
        tokenHelper.createToken(
            "2021-04-29", "0", "notifyMe", "userupload", Date.from(expiry), true, Instant.now());
    jwtDecoder.decode(accessToken);
  }

  @Test
  public void testDecoderInvalid() throws Exception {
    final var now = LocalDateTime.now();
    final var expiry = now.plusMinutes(5).toInstant(ZoneOffset.UTC);
    final var accessToken =
        tokenHelper.createToken(
            "2021-04-29", "0", "notifyMe", "userupload", Date.from(expiry), false, Instant.now());
    assertThrows(JwtException.class, () -> jwtDecoder.decode(accessToken));
  }
}
