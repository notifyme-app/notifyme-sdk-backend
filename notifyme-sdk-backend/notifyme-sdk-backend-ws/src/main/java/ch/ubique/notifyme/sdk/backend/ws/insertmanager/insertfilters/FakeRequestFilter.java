package ch.ubique.notifyme.sdk.backend.ws.insertmanager.insertfilters;

import ch.ubique.notifyme.sdk.backend.model.UserUploadPayloadOuterClass;
import ch.ubique.notifyme.sdk.backend.ws.insertmanager.InsertException;
import ch.ubique.notifyme.sdk.backend.ws.insertmanager.OSType;
import ch.ubique.notifyme.sdk.backend.ws.semver.Version;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * To prevent the possibility of traffic analysis, fake requests are sent at semi-regular intervals.
 * Fake uploads don't actually need to be inserted into the database and can therefore be dropped.
 */
public class FakeRequestFilter implements UploadInsertionFilter {
  @Override
  public List<UserUploadPayloadOuterClass.UploadVenueInfo> filter(
      LocalDateTime now,
      List<UserUploadPayloadOuterClass.UploadVenueInfo> uploadVenueInfoList,
      OSType osType,
      Version osVersion,
      Version appVersion,
      Object principal)
      throws InsertException {
    return uploadVenueInfoList.stream()
        .filter(uploadVenueInfo -> !uploadVenueInfo.getFake())
        .collect(Collectors.toList());
  }
}
