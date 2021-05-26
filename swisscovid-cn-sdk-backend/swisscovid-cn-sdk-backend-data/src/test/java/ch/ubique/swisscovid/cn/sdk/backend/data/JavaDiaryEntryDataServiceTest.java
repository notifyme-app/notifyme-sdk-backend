package ch.ubique.swisscovid.cn.sdk.backend.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import ch.ubique.swisscovid.cn.sdk.backend.model.VenueTypeOuterClass.VenueType;
import ch.ubique.swisscovid.cn.sdk.backend.model.event.CriticalEvent;
import ch.ubique.swisscovid.cn.sdk.backend.model.event.JavaDiaryEntry;
import java.util.List;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class JavaDiaryEntryDataServiceTest extends BaseDataServiceTest {

    @Autowired private DiaryEntryDataService diaryEntryDataService;

    @Test
    public void contextLoads() {
        assertNotNull(diaryEntryDataService);
    }

    @Test
    public void insertDiaryEntry() {
        List<JavaDiaryEntry> diaryEntries = DataServiceTestHelper.getDiaryEntries();
        diaryEntryDataService.insertDiaryEntry(diaryEntries.get(0));
        diaryEntryDataService.insertDiaryEntries(diaryEntries.subList(1, diaryEntries.size() - 1));
        final List<CriticalEvent> criticalEvents = diaryEntryDataService.getCriticalEvents();

        assertEquals(2, criticalEvents.size());

        // block to not mix up variables
        {
            final var criticalEvent0 = criticalEvents.get(0);
            assertNotNull(criticalEvent0);
            assertEquals(4, criticalEvent0.getCaseCount());

            final var diaryEntriesForEvent0 =
                    diaryEntryDataService.getDiaryEntriesForEvent(criticalEvent0);
            assertNotNull(diaryEntriesForEvent0);
            assertEquals(criticalEvent0.getCaseCount(), diaryEntriesForEvent0.size());

            final var diaryEntry0forEvent0 = diaryEntriesForEvent0.get(0);
            assertEquals("lecture0", diaryEntry0forEvent0.getName());
            assertEquals("location0", diaryEntry0forEvent0.getLocation());
            assertEquals("room0", diaryEntry0forEvent0.getRoom());
            assertEquals(VenueType.LECTURE_ROOM, diaryEntry0forEvent0.getVenueType());
            assertEquals(1577746800000L, diaryEntry0forEvent0.getCheckinTime().toEpochMilli());
            assertEquals(1577833200000L, diaryEntry0forEvent0.getCheckoutTime().toEpochMilli());
        }

        // block to not mix up variables
        {
            final var criticalEvent1 = criticalEvents.get(1);
            assertNotNull(criticalEvent1);
            assertEquals(2, criticalEvent1.getCaseCount());

            final var diaryEntriesForEvent1 =
                    diaryEntryDataService.getDiaryEntriesForEvent(criticalEvents.get(1));
            assertNotNull(diaryEntriesForEvent1);
            assertEquals(criticalEvent1.getCaseCount(), diaryEntriesForEvent1.size());

            final var diaryEntry0forEvent1 = diaryEntriesForEvent1.get(1);
            assertEquals("meeting3", diaryEntry0forEvent1.getName());
            assertEquals("location3", diaryEntry0forEvent1.getLocation());
            assertEquals("room3", diaryEntry0forEvent1.getRoom());
            assertEquals(VenueType.MEETING_ROOM, diaryEntry0forEvent1.getVenueType());
            assertEquals(1578006000000L, diaryEntry0forEvent1.getCheckinTime().toEpochMilli());
            assertEquals(1578092400000L, diaryEntry0forEvent1.getCheckoutTime().toEpochMilli());
        }
    }
}
