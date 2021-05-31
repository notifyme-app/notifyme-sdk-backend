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

public class JdbcSwissCovidDataServiceImplTest extends BaseDataServiceTest {

  @Autowired private SwissCovidDataService swissCovidDataService;

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
    assertNotNull(swissCovidDataService);
  }

  @Test
  @Transactional
  public void removeTraceKeysTest() {
    final TraceKey keyToRemove = getTraceKey();
    keyToRemove.setDay(keyToRemove.getDay().minus(2, ChronoUnit.DAYS));
    TraceKey keyToKeep = getTraceKey();
    swissCovidDataService.insertTraceKey(keyToRemove);
    swissCovidDataService.insertTraceKey(keyToKeep);
    swissCovidDataService.removeTraceKeys(keyToKeep.getDay().minus(1, ChronoUnit.DAYS));
    List<TraceKey> traceKeyList = swissCovidDataService.findTraceKeys(null);
    assertEquals(1, traceKeyList.size());
    swissCovidDataService.removeTraceKeys(start);
    traceKeyList = swissCovidDataService.findTraceKeys(null);
    assertTrue(traceKeyList.isEmpty());
  }

  @Test
  @Transactional
  public void insertTraceKeyTest() {
    final TraceKey actualKey = getTraceKey();
    swissCovidDataService.insertTraceKey(actualKey);
    swissCovidDataService.insertTraceKey(actualKey);
    swissCovidDataService.insertTraceKey(actualKey);
    final List<TraceKey> traceKeyList = swissCovidDataService.findTraceKeys(null);
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
    swissCovidDataService.insertTraceKey(actualTraceKeyList);
    final List<TraceKey> storedTraceKeyList = swissCovidDataService.findTraceKeys(null);
    assertEquals(3, storedTraceKeyList.size());
    verifyStored(storedTraceKeyList.get(0));
  }
}
