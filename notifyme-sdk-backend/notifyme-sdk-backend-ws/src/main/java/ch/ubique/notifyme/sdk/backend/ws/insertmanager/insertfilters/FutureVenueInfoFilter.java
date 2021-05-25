package ch.ubique.notifyme.sdk.backend.ws.insertmanager.insertfilters;

import ch.ubique.notifyme.sdk.backend.model.UserUploadPayloadOuterClass.UploadVenueInfo;
import ch.ubique.notifyme.sdk.backend.ws.insertmanager.InsertException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

public class FutureVenueInfoFilter implements
    UploadInsertionFilter {

  @Override
  public List<UploadVenueInfo> filter(LocalDateTime now, List<UploadVenueInfo> uploadVenueInfoList,
      Object principal)
      throws InsertException {
    return uploadVenueInfoList.stream().filter(uploadVenueInfo -> {
      final var intervalEnd = LocalDateTime.ofInstant(Instant.ofEpochMilli(uploadVenueInfo.getIntervalEndMs()), ZoneOffset.UTC);
      return intervalEnd.isBefore(now);
    }).collect(Collectors.toList());
  }
}
