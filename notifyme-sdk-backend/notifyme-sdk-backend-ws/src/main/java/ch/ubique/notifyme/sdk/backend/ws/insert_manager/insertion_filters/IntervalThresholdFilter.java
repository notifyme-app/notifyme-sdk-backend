package ch.ubique.notifyme.sdk.backend.ws.insert_manager.insertion_filters;

import ch.ubique.notifyme.sdk.backend.model.UserUploadPayloadOuterClass;
import ch.ubique.notifyme.sdk.backend.ws.insert_manager.InsertException;
import ch.ubique.notifyme.sdk.backend.ws.insert_manager.OSType;
import ch.ubique.notifyme.sdk.backend.ws.semver.Version;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * TODO: What's the minimum value we can use here?
 * The app doesn't support store visit of longer than 24 hours (users are automatically checked out).
 * UploadVenueInfo objects that break the interval threshold of (0,24) are dropped.
 */
public class IntervalThresholdFilter implements UploadInsertionFilter {
  @Override
  public List<UserUploadPayloadOuterClass.UploadVenueInfo> filter(
      LocalDateTime now,
      List<UserUploadPayloadOuterClass.UploadVenueInfo> uploadVenueInfoList,
      OSType osType,
      Version osVersion,
      Version appVersion,
      Object principal)
      throws InsertException {
    return uploadVenueInfoList.stream().filter(uploadVenueInfo -> {
        final var start = uploadVenueInfo.getIntervalStartMs();
        final var end = uploadVenueInfo.getIntervalEndMs();
        return (end - start > 0 && end - start <= 24 * 60 * 60 * 1000);
    }).collect(Collectors.toList());
  }
}
