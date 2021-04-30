package ch.ubique.notifyme.sdk.backend.data;

import ch.ubique.notifyme.sdk.backend.model.tracekey.v3.TraceKey;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class JdbcNotifyMeDataServiceV3ImplTest extends BaseDataServiceTest {

  @Autowired private NotifyMeDataServiceV3 notifyMeDataServiceV3;

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

  private void verifyStored(TraceKey storedKey) {
    assertNotNull(storedKey.getId());
    assertEquals(3, storedKey.getVersion());
    assertEquals(identityString, new String(storedKey.getIdentity(), charset));
    assertEquals(secretKey, new String(storedKey.getSecretKeyForIdentity(), charset));
    assertEquals(start.getEpochSecond(), storedKey.getStartTime().getEpochSecond());
    assertEquals(end.getEpochSecond(), storedKey.getEndTime().getEpochSecond());
    assertEquals(associatedData, new String(storedKey.getEncryptedAssociatedData(), charset));
    assertEquals(cipherTextNonce, new String(storedKey.getCipherTextNonce(), charset));
  }

  @Test
  public void t1_contextLoads() {
    assertNotNull(notifyMeDataServiceV3);
  }

  @Test
  public void t2_RemoveTraceKeys() {
    final TraceKey keyToRemove = getTraceKey();
    TraceKey keyToKeep = getTraceKey();
    keyToKeep.setEndTime(end.plusSeconds(2));
    notifyMeDataServiceV3.insertTraceKey(keyToRemove);
    notifyMeDataServiceV3.insertTraceKey(keyToKeep);
    notifyMeDataServiceV3.removeTraceKeys(end.plusSeconds(1));
    List<TraceKey> traceKeyList = notifyMeDataServiceV3.findTraceKeys(null);
    assertEquals(1, traceKeyList.size());
    notifyMeDataServiceV3.removeTraceKeys(end.plusSeconds(3));
    traceKeyList = notifyMeDataServiceV3.findTraceKeys(null);
    assertTrue(traceKeyList.isEmpty());
  }

  @Test
  public void t3_InsertTraceKey() {
    final TraceKey actualKey = getTraceKey();
    notifyMeDataServiceV3.insertTraceKey(actualKey);
    notifyMeDataServiceV3.insertTraceKey(actualKey);
    notifyMeDataServiceV3.insertTraceKey(actualKey);
    final List<TraceKey> traceKeyList = notifyMeDataServiceV3.findTraceKeys(null);
    assertEquals(3, traceKeyList.size());
    final TraceKey storedKey = traceKeyList.get(0);
    verifyStored(storedKey);
    notifyMeDataServiceV3.removeTraceKeys(end.plusSeconds(1));
    assertTrue(notifyMeDataServiceV3.findTraceKeys(null).isEmpty());
  }

  @Test
  public void t4_InsertTraceKeyList() {
    final List<TraceKey> actualTraceKeyList = new ArrayList<>();
    actualTraceKeyList.add(getTraceKey());
    actualTraceKeyList.add(getTraceKey());
    actualTraceKeyList.add(getTraceKey());
    notifyMeDataServiceV3.insertTraceKey(actualTraceKeyList);
    final List<TraceKey> storedTraceKeyList = notifyMeDataServiceV3.findTraceKeys(null);
    assertEquals(3, storedTraceKeyList.size());
    verifyStored(storedTraceKeyList.get(0));
    notifyMeDataServiceV3.removeTraceKeys(end.plusSeconds(1));
    assertTrue(notifyMeDataServiceV3.findTraceKeys(null).isEmpty());
  }
}
