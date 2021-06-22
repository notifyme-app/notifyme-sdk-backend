package ch.ubique.swisscovid.cn.sdk.backend.data;

import static org.junit.Assert.assertEquals;

import java.time.Duration;
import java.time.LocalDateTime;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class KPIDataServiceImplTest extends BaseDataServiceTest {

    @Autowired KPIDataService kpiDataService;

    @Test
    public void insertCheckinCount() {
        LocalDateTime uploadTime = LocalDateTime.parse("2021-06-21T12:00:00");
        // Shouldn't throw
        kpiDataService.insertCheckinCount(uploadTime, 0);
        kpiDataService.insertCheckinCount(uploadTime, -1);
        kpiDataService.insertCheckinCount(uploadTime, 1);
    }

    @Test
    public void cleanDB() {
        LocalDateTime uploadTime = LocalDateTime.now();
        // Shouldn't throw
        kpiDataService.insertCheckinCount(uploadTime, 0);
        kpiDataService.insertCheckinCount(uploadTime, -1);
        kpiDataService.insertCheckinCount(uploadTime.minusDays(1), 1);
        kpiDataService.insertCheckinCount(uploadTime, 1);
        assertEquals(1, kpiDataService.cleanDB(Duration.ofMinutes(30)));
    }
}
