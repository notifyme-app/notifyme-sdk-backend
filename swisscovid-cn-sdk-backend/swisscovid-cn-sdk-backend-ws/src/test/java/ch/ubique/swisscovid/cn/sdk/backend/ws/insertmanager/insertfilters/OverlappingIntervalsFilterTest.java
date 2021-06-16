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
        // Stayed at venue A for 1.5h
        final var noncesAndNotificationKey =
                cryptoWrapper
                        .getCryptoUtil()
                        .getNoncesAndNotificationKey(
                                cryptoWrapper.getCryptoUtil().createNonce(256));
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusHours(2);
        final var venueInfo1 = getVenueInfo(start, end, false, noncesAndNotificationKey);
        venueInfoList.add(venueInfo1);
        // Stayed at venue B for 15min
        start = end.minusMinutes(30);
        end = start.plusMinutes(45);
        final var venueInfo2 = getVenueInfo(start, end);
        venueInfoList.add(venueInfo2);
        // Stayed at venue A for 30 minutes
        start = end.minusMinutes(30);
        end = start.plusHours(1);
        final var venueInfo3 = getVenueInfo(start, end, false, noncesAndNotificationKey);
        venueInfoList.add(venueInfo3);
        return venueInfoList;
    }

    @Override
    List<UploadVenueInfo> getInvalidVenueInfo() {
        final var venueInfoList = new ArrayList<UploadVenueInfo>();
        // Stayed at venue A for 1h
        final var noncesAndNotificationKey =
            cryptoWrapper
                .getCryptoUtil()
                .getNoncesAndNotificationKey(
                    cryptoWrapper.getCryptoUtil().createNonce(256));
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusMinutes(30);
        final var venueInfo1 = getVenueInfo(start, end, false, noncesAndNotificationKey);
        start = end;
        end = start.plusMinutes(60);
        final var venueInfo2 = getVenueInfo(start, end, false, noncesAndNotificationKey);
        // Stayed at venue B for 15min concurrently
        start = end.minusMinutes(45);
        end = start.plusMinutes(45);
        final var venueInfo3 = getVenueInfo(start, end);
        // Stayed at venue A for another 15min
        start = end.minusMinutes(30);
        end = start.plusMinutes(45);
        final var venueInfo4 = getVenueInfo(start, end, false, noncesAndNotificationKey);
        // Request arrives with correct order
        venueInfoList.add(venueInfo1);
        venueInfoList.add(venueInfo2);
        venueInfoList.add(venueInfo3);
        venueInfoList.add(venueInfo4);
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
