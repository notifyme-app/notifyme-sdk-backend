package ch.ubique.swisscovid.cn.sdk.backend.ws.util;

import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jwts;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TokenHelper {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  private final String PATH_TO_PK = "src/test/resources/generated_public_test.pem";
  private final String PATH_TO_SK = "src/test/resources/generated_private_test.der";
  private final String PATH_TO_SK_OTHER = "src/test/resources/generated_private_test_2.der";

  private RSAPublicKey publicKey;
  private RSAPrivateKey privateKey;

  private RSAPrivateKey loadPrivateKey(String path) throws Exception {
    File skFile = new File(path);
    FileInputStream fis = new FileInputStream(skFile);
    DataInputStream dis = new DataInputStream(fis);

    byte[] encoded = new byte[(int) skFile.length()];
    dis.readFully(encoded);
    dis.close();

    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
    PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
    return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
  }

  private RSAPublicKey loadPublicKey(String path) throws Exception {
    File pkFile = new File(PATH_TO_PK);
    String key = new String(Files.readAllBytes(pkFile.toPath()), Charset.defaultCharset());
    String publicKeyPEM =
        key.replace("-----BEGIN PUBLIC KEY-----", "")
            .replaceAll(System.lineSeparator(), "")
            .replace("-----END PUBLIC KEY-----", "");

    byte[] encoded = Base64.getDecoder().decode(publicKeyPEM);

    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
    X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
    return (RSAPublicKey) keyFactory.generatePublic(keySpec);
  }

  public TokenHelper() throws Exception {
    publicKey = loadPublicKey(PATH_TO_PK);
    privateKey = loadPrivateKey(PATH_TO_SK);
  }

  public String createToken(
          String onsetDate,
          String fake,
          String audience,
          String scope,
          Date expiresAt,
          boolean validSig,
          Instant now)
      throws Exception {
    final var sigKey = validSig ? privateKey : loadPrivateKey(PATH_TO_SK_OTHER);

    return Jwts.builder()
        .setId(UUID.randomUUID().toString())
        .setIssuedAt(Date.from(now))
        .setNotBefore(Date.from(now))
        .setExpiration(expiresAt)
        .setHeaderParam(Header.TYPE, Header.JWT_TYPE)
        .setAudience(audience)
        .claim("scope", scope)
        .claim("fake", fake)
        .claim("onset", onsetDate)
        .signWith(sigKey)
        .compact();
  }

  public RSAPublicKey getPublicKey() {
    return publicKey;
  }

  public RSAPrivateKey getPrivateKey() {
    return privateKey;
  }
}
