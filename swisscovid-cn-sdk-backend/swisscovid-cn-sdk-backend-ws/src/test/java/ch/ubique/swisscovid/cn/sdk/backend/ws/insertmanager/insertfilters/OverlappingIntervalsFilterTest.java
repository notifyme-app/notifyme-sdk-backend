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
    List<List<UploadVenueInfo>> getValidVenueInfo() {
        final var testcaseList = new ArrayList<List<UploadVenueInfo>>();
        testcaseList.add(validCase1());
        testcaseList.add(validCase2());
        return testcaseList;
    }

    private ArrayList<UploadVenueInfo> validCase1() {
        final var venueInfoList = new ArrayList<UploadVenueInfo>();
        final var noncesAndNotificationKey =
                cryptoWrapper
                        .getCryptoUtil()
                        .getNoncesAndNotificationKey(
                                cryptoWrapper.getCryptoUtil().createNonce(256));
        // Stayed at venue A for 1.5h
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusHours(2);
        final var venueInfo1 = venueInfoHelper.getVenueInfo(start, end, false, noncesAndNotificationKey);
        // Stayed at venue B for 15min
        start = end.minusMinutes(30);
        end = start.plusMinutes(45);
        final var venueInfo2 = getVenueInfo(start, end);
        // Stayed at venue A for 30 minutes
        start = end.minusMinutes(30);
        end = start.plusHours(1);
        final var venueInfo3 = venueInfoHelper.getVenueInfo(start, end, false, noncesAndNotificationKey);
        venueInfoList.addAll(venueInfo1);
        venueInfoList.addAll(venueInfo2);
        venueInfoList.addAll(venueInfo3);
        return venueInfoList;
    }

    private ArrayList<UploadVenueInfo> validCase2() {
        final var venueInfoList = new ArrayList<UploadVenueInfo>();
        final var noncesAndNotificationKey =
                cryptoWrapper
                        .getCryptoUtil()
                        .getNoncesAndNotificationKey(
                                cryptoWrapper.getCryptoUtil().createNonce(256));
        // Stayed at venue A for 1.5h
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusHours(2);
        final var venueInfo1 = venueInfoHelper.getVenueInfo(start, end, false, noncesAndNotificationKey);
        // Checked out and back in at venue A for another 1h
        start = end.minusMinutes(30);
        end = start.plusHours(1);
        final var venueInfo2 = venueInfoHelper.getVenueInfo(start, end, false, noncesAndNotificationKey);
        // Stayed at venue B for 15min
        start = end.minusMinutes(30);
        end = start.plusMinutes(45);
        final var venueInfo3 = getVenueInfo(start, end);
        // Stayed at venue A for 30 minutes
        start = end.minusMinutes(30);
        end = start.plusHours(1);
        final var venueInfo4 = venueInfoHelper.getVenueInfo(start, end, false, noncesAndNotificationKey);
        venueInfoList.addAll(venueInfo1);
        venueInfoList.addAll(venueInfo2);
        venueInfoList.addAll(venueInfo3);
        venueInfoList.addAll(venueInfo4);
        return venueInfoList;
    }

    @Override
    List<List<UploadVenueInfo>> getInvalidVenueInfo() {
        final var testcaseList = new ArrayList<List<UploadVenueInfo>>();
        testcaseList.add(invalidCase1());
        testcaseList.add(invalidCase2());
        testcaseList.add(invalidCase3());
        return testcaseList;
    }

    private ArrayList<UploadVenueInfo> invalidCase1() {
        final var venueInfoList = new ArrayList<UploadVenueInfo>();
        final var noncesAndNotificationKey =
                cryptoWrapper
                        .getCryptoUtil()
                        .getNoncesAndNotificationKey(
                                cryptoWrapper.getCryptoUtil().createNonce(256));
        // Stayed at venue A for 1h
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusMinutes(30);
        final var venueInfo1 = venueInfoHelper.getVenueInfo(start, end, false, noncesAndNotificationKey);
        start = end;
        end = start.plusMinutes(60);
        final var venueInfo2 = venueInfoHelper.getVenueInfo(start, end, false, noncesAndNotificationKey);
        // Overlapping checkin at venue B for 15min
        start = end.minusMinutes(45);
        end = start.plusMinutes(45);
        final var venueInfo3 = getVenueInfo(start, end);
        // Stayed at venue A for another 15min
        start = end.minusMinutes(30);
        end = start.plusMinutes(45);
        final var venueInfo4 = venueInfoHelper.getVenueInfo(start, end, false, noncesAndNotificationKey);
        // Request arrives with correct order
        venueInfoList.addAll(venueInfo1);
        venueInfoList.addAll(venueInfo2);
        venueInfoList.addAll(venueInfo3);
        venueInfoList.addAll(venueInfo4);
        return venueInfoList;
    }

    private ArrayList<UploadVenueInfo> invalidCase2() {
        final var venueInfoList = new ArrayList<UploadVenueInfo>();
        final var noncesAndNotificationKey =
                cryptoWrapper
                        .getCryptoUtil()
                        .getNoncesAndNotificationKey(
                                cryptoWrapper.getCryptoUtil().createNonce(256));
        // Two overlapping checkins at venue A for 30min
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusMinutes(60);
        final var venueInfo1 = venueInfoHelper.getVenueInfo(start, end, false, noncesAndNotificationKey);
        final var venueInfo2 = venueInfoHelper.getVenueInfo(start, end, false, noncesAndNotificationKey);
        // Request arrives with correct order
        venueInfoList.addAll(venueInfo1);
        venueInfoList.addAll(venueInfo2);
        return venueInfoList;
    }

    private ArrayList<UploadVenueInfo> invalidCase3() {
        final var venueInfoList = new ArrayList<UploadVenueInfo>();
        final var noncesAndNotificationKey =
                cryptoWrapper
                        .getCryptoUtil()
                        .getNoncesAndNotificationKey(
                                cryptoWrapper.getCryptoUtil().createNonce(256));
        // Stayed at venue B for 1h
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusMinutes(90);
        final var venueInfo1 = getVenueInfo(start, end);
        // Stayed at venue A for 15min
        start = end.minusMinutes(30);
        end = start.plusMinutes(45);
        final var venueInfo2 = venueInfoHelper.getVenueInfo(start, end, false, noncesAndNotificationKey);
        // Overlapping checkin at venue A for 15min
        start = start.minusMinutes(15);
        end = start.plusMinutes(45);
        final var venueInfo3 = venueInfoHelper.getVenueInfo(start, end, false, noncesAndNotificationKey);
        // Request arrives with correct order
        venueInfoList.addAll(venueInfo1);
        venueInfoList.addAll(venueInfo2);
        venueInfoList.addAll(venueInfo3);
        return venueInfoList;
    }

    @Override
    @Test
    public void testFilterInvalid() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        final List<List<UploadVenueInfo>> testcaseList = getInvalidVenueInfo();
        final var token = getToken(now);
        for (List<UploadVenueInfo> uploadVenueInfoList : testcaseList) {
            assertThrows(
                    OverlappingIntervalsException.class,
                    () -> insertionFilter().filter(now, uploadVenueInfoList, token));
        }
    }

    @Override
    UploadInsertionFilter insertionFilter() {
        return new OverlappingIntervalsFilter();
    }
}
