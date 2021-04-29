package ch.ubique.notifyme.sdk.backend.ws.config;

import ch.ubique.notifyme.sdk.backend.data.UUIDDataService;
import ch.ubique.notifyme.sdk.backend.ws.security.KeyVault;
import ch.ubique.notifyme.sdk.backend.ws.security.NotifyMeJwtValidator;
import ch.ubique.notifyme.sdk.backend.ws.util.TokenHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

import static org.junit.Assert.assertThrows;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = WSDevConfig.class)
@ActiveProfiles({"dev", "jwt"})
@TestPropertySource(properties = {"ws.app.jwt.publickey=classpath://generated_public_test.pem"})
public class JWTValidationTest {

  @Autowired UUIDDataService uuidDataService;

  // TODO: How to do this nicely with dependency injection?
  JwtDecoder jwtDecoder;
  TokenHelper tokenHelper;

  @Before
  public void setup() throws Exception {
    tokenHelper = new TokenHelper();
    jwtDecoder = jwtDecoder();
  }

  @Test
  public void testDecoderValid() throws Exception {
    final var now = LocalDateTime.now();
    final var expiry = now.plusMinutes(5).toInstant(ZoneOffset.UTC);
    final var accessToken =
        tokenHelper.createToken("2021-04-29", "0", "notifyMe", "userupload", Date.from(expiry), true);
    jwtDecoder.decode(accessToken);
  }

  @Test
  public void testDecoderInvalid() throws Exception {
    final var now = LocalDateTime.now();
    final var expiry = now.plusMinutes(5).toInstant(ZoneOffset.UTC);
    final var accessToken =
        tokenHelper.createToken("2021-04-29", "0", "notifyMe", "userupload", Date.from(expiry), false);
    assertThrows(JwtException.class, () -> jwtDecoder.decode(accessToken));
  }

  private NotifyMeJwtValidator jwtValidator() {
    return new NotifyMeJwtValidator(uuidDataService, Duration.ofMinutes(60));
  }

  private JwtDecoder jwtDecoder()
      throws IOException, KeyVault.PublicKeyNoSuitableEncodingFoundException {
    final var nimbusJwtDecoder =
        NimbusJwtDecoder.withPublicKey(tokenHelper.getPublicKey())
            .signatureAlgorithm(SignatureAlgorithm.RS256)
            .build();
    nimbusJwtDecoder.setJwtValidator(jwtValidator());
    return nimbusJwtDecoder;
  }
}
