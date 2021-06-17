package ch.ubique.swisscovid.cn.sdk.backend.ws.insertmanager.insertfilters;

import ch.ubique.swisscovid.cn.sdk.backend.model.UserUploadPayloadOuterClass.UploadVenueInfo;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class IntervalThresholdFilterTest extends UploadInsertionFilterTest {
    @Override
    List<List<UploadVenueInfo>> getValidVenueInfo() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusMinutes(30);
        // Not an edge case
        final var venueInfoCase1 = getVenueInfo(start, end);
        final var venueInfoList = new ArrayList<>(venueInfoCase1);
        start = LocalDateTime.now();
        end = start.plusHours(1);
        // Exactly 1 hour
        final var venueInfoCase2 = getVenueInfo(start, end);
        venueInfoList.addAll(venueInfoCase2);
        return List.of(venueInfoList);
    }

    @Override
    List<List<UploadVenueInfo>> getInvalidVenueInfo() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusHours(-1);
        // End < Start
        final var venueInfoCase1 = getVenueInfo(start, end);
        final var venueInfoList = new ArrayList<>(venueInfoCase1);
        return List.of(venueInfoList);
    }

    @Override
    UploadInsertionFilter insertionFilter() {
        return new IntervalThresholdFilter();
    }
}
