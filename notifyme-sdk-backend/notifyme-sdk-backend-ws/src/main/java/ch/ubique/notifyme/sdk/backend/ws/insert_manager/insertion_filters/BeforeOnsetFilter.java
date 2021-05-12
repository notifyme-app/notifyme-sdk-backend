package ch.ubique.notifyme.sdk.backend.ws.insert_manager.insertion_filters;

import ch.ubique.notifyme.sdk.backend.model.UserUploadPayloadOuterClass;
import ch.ubique.notifyme.sdk.backend.ws.insert_manager.InsertException;
import ch.ubique.notifyme.sdk.backend.ws.insert_manager.OSType;
import ch.ubique.notifyme.sdk.backend.ws.security.NotifyMeJwtRequestValidator;
import ch.ubique.notifyme.sdk.backend.ws.semver.Version;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

/**
 * When a person tests positive, the "onset" date since when they are infectious is estimated.
 * Visits which occurred before the onset are dropped.
 */
public class BeforeOnsetFilter implements UploadInsertionFilter {
  @Override
  public List<UserUploadPayloadOuterClass.UploadVenueInfo> filter(
      LocalDateTime now,
      List<UserUploadPayloadOuterClass.UploadVenueInfo> uploadVenueInfoList,
      OSType osType,
      Version osVersion,
      Version appVersion,
      Object principal)
      throws InsertException {
    final var notifyMeJwtRequestValidator = new NotifyMeJwtRequestValidator();
    return uploadVenueInfoList.stream()
        .filter(
            uploadVenueInfo -> {
              final var epochMilli = Instant.ofEpochMilli(uploadVenueInfo.getIntervalEndMs());
              return notifyMeJwtRequestValidator.isOnsetBefore(
                  principal, LocalDateTime.ofInstant(epochMilli, ZoneOffset.UTC));
            })
        .collect(Collectors.toList());
  }
}
