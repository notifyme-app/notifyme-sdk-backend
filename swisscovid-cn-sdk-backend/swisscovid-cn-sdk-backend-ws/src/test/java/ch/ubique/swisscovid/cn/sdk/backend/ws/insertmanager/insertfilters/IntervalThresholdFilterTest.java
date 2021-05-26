package ch.ubique.swisscovid.cn.sdk.backend.ws.insertmanager.insertfilters;

import ch.ubique.swisscovid.cn.sdk.backend.model.UserUploadPayloadOuterClass.UploadVenueInfo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class IntervalThresholdFilterTest extends UploadInsertionFilterTest {
  @Override
  List<UploadVenueInfo> getValidVenueInfo() {
    final var venueInfoList = new ArrayList<UploadVenueInfo>();
    LocalDateTime start = LocalDateTime.now();
    LocalDateTime end = start.plusMinutes(30);
    // Not an edge case
    final var venueInfoCase1 = getVenueInfo(start, end);
    venueInfoList.add(venueInfoCase1);
    start = LocalDateTime.now();
    end = start.plusHours(1);
    // Exactly 1 hour
    final var venueInfoCase2 = getVenueInfo(start, end);
    venueInfoList.add(venueInfoCase2);
    return venueInfoList;
  }

  @Override
  List<UploadVenueInfo> getInvalidVenueInfo() {
    final var venueInfoList = new ArrayList<UploadVenueInfo>();
    LocalDateTime start = LocalDateTime.now();
    LocalDateTime end = start.plusHours(-1);
    // End < Start
    final var venueInfoCase1 = getVenueInfo(start, end);
    venueInfoList.add(venueInfoCase1);
    start = LocalDateTime.now();
    end = start.plusHours(25);
    // End - Start > 24
    final var venueInfoCase2 = getVenueInfo(start, end);
    venueInfoList.add(venueInfoCase2);
    return venueInfoList;
  }

  @Override
  UploadInsertionFilter insertionFilter() {
    return new IntervalThresholdFilter();
  }
}
