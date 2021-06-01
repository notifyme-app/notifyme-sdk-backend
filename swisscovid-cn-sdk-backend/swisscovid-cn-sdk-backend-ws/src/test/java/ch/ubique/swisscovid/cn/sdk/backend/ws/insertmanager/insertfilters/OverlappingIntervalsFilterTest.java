package ch.ubique.swisscovid.cn.sdk.backend.ws.insertmanager.insertfilters;

import static org.junit.Assert.assertThrows;

import ch.ubique.swisscovid.cn.sdk.backend.model.UserUploadPayloadOuterClass.UploadVenueInfo;
import ch.ubique.swisscovid.cn.sdk.backend.ws.insertmanager.OverlappingIntervalsException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class OverlappingIntervalsFilterTest extends UploadInsertionFilterTest {
    @Override
    List<UploadVenueInfo> getValidVenueInfo() {
        final var venueInfoList = new ArrayList<UploadVenueInfo>();
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusHours(1);
        final var venueInfo1 = getVenueInfo(start, end);
        venueInfoList.add(venueInfo1);
        start = end.plusMinutes(1);
        end = start;
        final var venueInfo2 = getVenueInfo(start, end);
        venueInfoList.add(venueInfo2);
        return venueInfoList;
    }

    @Override
    List<UploadVenueInfo> getInvalidVenueInfo() {
        final var venueInfoList = new ArrayList<UploadVenueInfo>();
        final var now = LocalDateTime.now();
        LocalDateTime start = now;
        LocalDateTime end = start.plusHours(1);
        final var venueInfo1 = getVenueInfo(start, end);
        venueInfoList.add(venueInfo1);
        start = end.minusMinutes(1);
        end = start.plusHours(1);
        final var venueInfo2 = getVenueInfo(start, end);
        venueInfoList.add(venueInfo2);
        start = now.minusMinutes(59);
        end = start.plusHours(1);
        final var venueInfo3 = getVenueInfo(start, end);
        venueInfoList.add(venueInfo3);
        return venueInfoList;
    }

    @Override
    @Test
    public void testFilterInvalid() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        final List<UploadVenueInfo> uploadVenueInfoList = getInvalidVenueInfo();
        final var token = getToken(now);
        assertThrows(
                OverlappingIntervalsException.class,
                () -> insertionFilter().filter(now, uploadVenueInfoList, token));
    }

    @Override
    UploadInsertionFilter insertionFilter() {
        return new OverlappingIntervalsFilter();
    }
}
