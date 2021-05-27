package ch.ubique.swisscovid.cn.sdk.backend.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import mockit.Mock;
import mockit.MockUp;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

public class JDBCInteractionDurationDataServiceImplTest extends BaseDataServiceTest {

    @Autowired private InteractionDurationDataService interactionDurationDataService;

    @Test
    public void contextLoadsTest() {
        assertNotNull(interactionDurationDataService);
    }

    @Test
    @Transactional
    public void insertAndFindInteractionsTest() {
        Instant now = Instant.now();
        assertTrue(
                interactionDurationDataService
                        .findInteractions(now.minus(14, ChronoUnit.DAYS))
                        .isEmpty());
        interactionDurationDataService.insertInteraction(60000);
        // Check that before is treated as strict inequality
        assertTrue(
                interactionDurationDataService
                        .findInteractions(now.minus(1, ChronoUnit.DAYS))
                        .isEmpty());
        // Wind clock forward
        Clock clock = Clock.fixed(now.plus(2, ChronoUnit.DAYS), ZoneOffset.UTC);
        new MockUp<Instant>() {
            @Mock
            public Instant now() {
                return Instant.now(clock);
            }
        };
        // Check that after works as inclusive lower bound
        assertTrue(
                interactionDurationDataService
                        .findInteractions(now.plus(1, ChronoUnit.DAYS))
                        .isEmpty());
        assertEquals(
                1,
                interactionDurationDataService
                        .findInteractions(now.minus(7, ChronoUnit.DAYS))
                        .size());
        assertEquals(
                60000,
                interactionDurationDataService
                        .findInteractions(now.minus(7, ChronoUnit.DAYS))
                        .get(0)
                        .intValue());
    }

    @Test
    @Transactional
    public void removeInteractionsTest() {
        Instant now = Instant.now();
        assertTrue(
                interactionDurationDataService
                        .findInteractions(now.minus(14, ChronoUnit.DAYS))
                        .isEmpty());
        interactionDurationDataService.insertInteraction(60000);
        // Wind clock forward
        Clock clock = Clock.fixed(now.plus(1, ChronoUnit.DAYS), ZoneOffset.UTC);
        new MockUp<Instant>() {
            @Mock
            public Instant now() {
                return Instant.now(clock);
            }
        };
        assertEquals(
                1,
                interactionDurationDataService
                        .findInteractions(now.minus(14, ChronoUnit.DAYS))
                        .size());
        interactionDurationDataService.removeDurations(Duration.ZERO);
        assertTrue(
                interactionDurationDataService
                        .findInteractions(now.minus(14, ChronoUnit.DAYS))
                        .isEmpty());
    }
}
