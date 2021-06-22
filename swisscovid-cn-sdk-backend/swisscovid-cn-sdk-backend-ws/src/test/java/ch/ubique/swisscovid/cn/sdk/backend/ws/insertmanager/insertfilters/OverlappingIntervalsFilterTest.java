package ch.ubique.swisscovid.cn.sdk.backend.ws.insertmanager.insertfilters;

import static org.junit.Assert.assertThrows;

import ch.ubique.swisscovid.cn.sdk.backend.model.UserUploadPayloadOuterClass.UploadVenueInfo;
import ch.ubique.swisscovid.cn.sdk.backend.ws.insertmanager.OverlappingIntervalsException;
import ch.ubique.swisscovid.cn.sdk.backend.ws.insertmanager.insertfilters.OverlappingIntervalsFilterTest.IntervalTestVectors.CheckinParams;
import ch.ubique.swisscovid.cn.sdk.backend.ws.insertmanager.insertfilters.OverlappingIntervalsFilterTest.IntervalTestVectors.TestCase;
import ch.ubique.swisscovid.cn.sdk.backend.ws.util.CryptoUtil.NoncesAndNotificationKey;
import com.google.gson.Gson;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

public class OverlappingIntervalsFilterTest extends UploadInsertionFilterTest {

    private IntervalTestVectors intervalTestVectors;

    @Before
    public void init() throws FileNotFoundException {
        intervalTestVectors =
                new Gson()
                        .fromJson(
                                new InputStreamReader(
                                        new FileInputStream(
                                                "src/test/resources/crowd_notifier_overlapping_intervals.json")),
                                IntervalTestVectors.class);
    }

    @Override
    List<List<UploadVenueInfo>> getValidVenueInfo() {
        return getTestCases(intervalTestVectors.validTestcases);
    }

    @Override
    List<List<UploadVenueInfo>> getInvalidVenueInfo() {
        return getTestCases(intervalTestVectors.invalidTestcases);
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

    private List<List<UploadVenueInfo>> getTestCases(List<TestCase> testCases) {
        List<List<UploadVenueInfo>> testCaseList = new ArrayList<>();
        for (TestCase testCase : testCases) {
            List<UploadVenueInfo> venueInfoList = new ArrayList<>();
            Map<String, NoncesAndNotificationKey> venues = new HashMap<>();
            for (CheckinParams checkin : testCase.checkins) {
                NoncesAndNotificationKey venueParams =
                        venues.computeIfAbsent(
                                checkin.venueId,
                                k ->
                                        cryptoWrapper
                                                .getCryptoUtil()
                                                .getNoncesAndNotificationKey(
                                                        cryptoWrapper
                                                                .getCryptoUtil()
                                                                .createNonce(256)));
                venueInfoList.addAll(
                        venueInfoHelper.getVenueInfo(
                                LocalDateTime.parse(checkin.start),
                                LocalDateTime.parse(checkin.end),
                                false,
                                venueParams));
            }
            testCaseList.add(venueInfoList);
        }
        return testCaseList;
    }

    public static class IntervalTestVectors {

        public List<TestCase> validTestcases;
        public List<TestCase> invalidTestcases;

        public static class TestCase {
            public List<CheckinParams> checkins;
        }

        public static class CheckinParams {
            public String venueId;
            public String start;
            public String end;
        }
    }
}
