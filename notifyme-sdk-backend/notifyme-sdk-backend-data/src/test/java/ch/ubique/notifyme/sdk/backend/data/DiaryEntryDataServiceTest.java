package ch.ubique.notifyme.sdk.backend.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import ch.ubique.notifyme.sdk.backend.model.VenueTypeOuterClass.VenueType;
import ch.ubique.notifyme.sdk.backend.model.event.CriticalEvent;
import ch.ubique.notifyme.sdk.backend.model.event.DiaryEntry;
import java.util.List;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class DiaryEntryDataServiceTest extends BaseDataServiceTest {

    @Autowired private DiaryEntryDataService diaryEntryDataService;

    @Test
    public void contextLoads() {
        assertNotNull(diaryEntryDataService);
    }

    @Test
    public void insertDiaryEntry() {
        List<DiaryEntry> diaryEntries = DataServiceTestHelper.getDiaryEntries();
        diaryEntryDataService.insertDiaryEntry(diaryEntries.get(0));
        diaryEntryDataService.insertDiaryEntries(diaryEntries.subList(1, diaryEntries.size() - 1));
        final List<CriticalEvent> criticalEvents = diaryEntryDataService.getCriticalEvents();

        assertEquals(2, criticalEvents.size());

        final var diaryEntriesForEvent0 =
                diaryEntryDataService.getDiaryEntriesForEvent(criticalEvents.get(0));
        assertNotNull(criticalEvents.get(0));
        assertEquals(4, criticalEvents.get(0).getCaseCount());
        assertEquals(4, diaryEntriesForEvent0.size());
        assertEquals("lecture0", diaryEntriesForEvent0.get(0).getName());
        assertEquals("location0", diaryEntriesForEvent0.get(0).getLocation());
        assertEquals("room0", diaryEntriesForEvent0.get(0).getRoom());
        assertEquals(VenueType.LECTURE_ROOM, diaryEntriesForEvent0.get(0).getVenueType());
        assertEquals(1577746800000L, diaryEntriesForEvent0.get(0).getCheckinTime().toEpochMilli());
        assertEquals(1577833200000L, diaryEntriesForEvent0.get(0).getCheckoutTime().toEpochMilli());

        final var diaryEntriesForEvent1 =
                diaryEntryDataService.getDiaryEntriesForEvent(criticalEvents.get(1));
        assertNotNull(criticalEvents.get(1));
        assertEquals(2, criticalEvents.get(1).getCaseCount());
        assertEquals(2, diaryEntriesForEvent1.size());
        assertEquals("meeting3", diaryEntriesForEvent1.get(1).getName());
        assertEquals("location3", diaryEntriesForEvent1.get(1).getLocation());
        assertEquals("room3", diaryEntriesForEvent1.get(1).getRoom());
        assertEquals(VenueType.MEETING_ROOM, diaryEntriesForEvent1.get(1).getVenueType());
        assertEquals(1578006000000L, diaryEntriesForEvent1.get(1).getCheckinTime().toEpochMilli());
        assertEquals(1578092400000L, diaryEntriesForEvent1.get(1).getCheckoutTime().toEpochMilli());
    }
}
