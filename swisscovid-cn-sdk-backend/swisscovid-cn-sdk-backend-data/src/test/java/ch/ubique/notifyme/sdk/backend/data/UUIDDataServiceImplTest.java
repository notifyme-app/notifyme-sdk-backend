package ch.ubique.notifyme.sdk.backend.data;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;

import static org.junit.Assert.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class UUIDDataServiceImplTest extends BaseDataServiceTest {

  @Autowired UUIDDataService uuidDataService;

  private String foo = "foo", bar = "bar", empty = "";

  @Test
  public void t0_contextLoads() {
    assertNotNull(uuidDataService);
  }

  @Test
  public void t1_testCheckAndInsertPublishUUID() {
    assertTrue(uuidDataService.checkAndInsertPublishUUID(foo));
    assertTrue(uuidDataService.checkAndInsertPublishUUID(bar));
    assertTrue(uuidDataService.checkAndInsertPublishUUID(empty));
    assertFalse(uuidDataService.checkAndInsertPublishUUID(foo));
    assertFalse(uuidDataService.checkAndInsertPublishUUID(bar));
  }

  @Test
  public void t2_testCleanDB() {
    final var deleteNone1 = Duration.ofDays(3);// 3 days in the past
    final var deleteNone2 = Duration.ofDays(0);// present
    final var deleteAll = Duration.ofDays(-3); // 3 days in the future
    uuidDataService.cleanDB(deleteNone1);
    assertFalse(uuidDataService.checkAndInsertPublishUUID(foo));
    uuidDataService.cleanDB(deleteNone2);
    // JWT's validity likely hasn't expired yet --> Shouldn't be deleted!
    assertFalse(uuidDataService.checkAndInsertPublishUUID(foo));
    uuidDataService.cleanDB(deleteAll);
    assertTrue(uuidDataService.checkAndInsertPublishUUID(foo));
  }
}
