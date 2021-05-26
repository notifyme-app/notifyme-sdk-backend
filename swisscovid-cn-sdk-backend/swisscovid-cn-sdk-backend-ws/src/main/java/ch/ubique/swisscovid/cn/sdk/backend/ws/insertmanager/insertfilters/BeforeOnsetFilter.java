package ch.ubique.swisscovid.cn.sdk.backend.ws.insertmanager.insertfilters;

import ch.ubique.swisscovid.cn.sdk.backend.model.UserUploadPayloadOuterClass;
import ch.ubique.swisscovid.cn.sdk.backend.model.UserUploadPayloadOuterClass.UploadVenueInfo;
import ch.ubique.swisscovid.cn.sdk.backend.ws.insertmanager.InsertException;
import ch.ubique.swisscovid.cn.sdk.backend.ws.security.NotifyMeJwtRequestValidator;

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
      List<UploadVenueInfo> uploadVenueInfoList,
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
