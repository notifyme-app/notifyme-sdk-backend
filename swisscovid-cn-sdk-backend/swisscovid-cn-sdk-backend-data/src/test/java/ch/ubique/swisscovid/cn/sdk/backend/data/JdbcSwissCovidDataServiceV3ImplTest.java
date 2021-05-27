package ch.ubique.swisscovid.cn.sdk.backend.data;

import static org.junit.Assert.*;

import ch.ubique.swisscovid.cn.sdk.backend.model.tracekey.TraceKey;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

public class JdbcSwissCovidDataServiceV3ImplTest extends BaseDataServiceTest {

  @Autowired private SwissCovidDataServiceV3 swissCovidDataServiceV3;

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
    traceKey.setDay(start.truncatedTo(ChronoUnit.DAYS));
    traceKey.setCreatedAt(start.minusSeconds(60 * 60 * 3));
    traceKey.setEncryptedAssociatedData(associatedData.getBytes(charset));
    traceKey.setCipherTextNonce(cipherTextNonce.getBytes(charset));
    return traceKey;
  }

  private void verifyStored(TraceKey storedKey) {
    assertNotNull(storedKey.getId());
    assertEquals(3, storedKey.getVersion());
    assertEquals(identityString, new String(storedKey.getIdentity(), charset));
    assertEquals(secretKey, new String(storedKey.getSecretKeyForIdentity(), charset));
    assertEquals(
        start.truncatedTo(ChronoUnit.DAYS).toEpochMilli(), storedKey.getDay().toEpochMilli());
    assertEquals(associatedData, new String(storedKey.getEncryptedAssociatedData(), charset));
    assertEquals(cipherTextNonce, new String(storedKey.getCipherTextNonce(), charset));
  }

  @Test
  public void contextLoadsTest() {
    assertNotNull(swissCovidDataServiceV3);
  }

  @Test
  @Transactional
  public void removeTraceKeysTest() {
    final TraceKey keyToRemove = getTraceKey();
    keyToRemove.setDay(keyToRemove.getDay().minus(2, ChronoUnit.DAYS));
    TraceKey keyToKeep = getTraceKey();
    swissCovidDataServiceV3.insertTraceKey(keyToRemove);
    swissCovidDataServiceV3.insertTraceKey(keyToKeep);
    swissCovidDataServiceV3.removeTraceKeys(keyToKeep.getDay().minus(1, ChronoUnit.DAYS));
    List<TraceKey> traceKeyList = swissCovidDataServiceV3.findTraceKeys(null);
    assertEquals(1, traceKeyList.size());
    swissCovidDataServiceV3.removeTraceKeys(start);
    traceKeyList = swissCovidDataServiceV3.findTraceKeys(null);
    assertTrue(traceKeyList.isEmpty());
  }

  @Test
  @Transactional
  public void insertTraceKeyTest() {
    final TraceKey actualKey = getTraceKey();
    swissCovidDataServiceV3.insertTraceKey(actualKey);
    swissCovidDataServiceV3.insertTraceKey(actualKey);
    swissCovidDataServiceV3.insertTraceKey(actualKey);
    final List<TraceKey> traceKeyList = swissCovidDataServiceV3.findTraceKeys(null);
    assertEquals(3, traceKeyList.size());
    final TraceKey storedKey = traceKeyList.get(0);
    verifyStored(storedKey);
  }

  @Test
  @Transactional
  public void insertTraceKeyListTest() {
    final List<TraceKey> actualTraceKeyList = new ArrayList<>();
    actualTraceKeyList.add(getTraceKey());
    actualTraceKeyList.add(getTraceKey());
    actualTraceKeyList.add(getTraceKey());
    swissCovidDataServiceV3.insertTraceKey(actualTraceKeyList);
    final List<TraceKey> storedTraceKeyList = swissCovidDataServiceV3.findTraceKeys(null);
    assertEquals(3, storedTraceKeyList.size());
    verifyStored(storedTraceKeyList.get(0));
  }
}
