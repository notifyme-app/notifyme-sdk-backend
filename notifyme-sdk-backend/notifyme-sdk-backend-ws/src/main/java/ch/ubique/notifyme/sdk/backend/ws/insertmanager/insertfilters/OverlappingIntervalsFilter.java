package ch.ubique.notifyme.sdk.backend.ws.insertmanager.insertfilters;

import ch.ubique.notifyme.sdk.backend.model.UserUploadPayloadOuterClass;
import ch.ubique.notifyme.sdk.backend.model.UserUploadPayloadOuterClass.UploadVenueInfo;
import ch.ubique.notifyme.sdk.backend.ws.insertmanager.InsertException;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A person can't be in two places at the same time. UploadVenueInfo objects whose time windows
 * overlap are both removed.
 */
public class OverlappingIntervalsFilter implements UploadInsertionFilter {
  @Override
  public List<UserUploadPayloadOuterClass.UploadVenueInfo> filter(
      LocalDateTime now,
      List<UploadVenueInfo> uploadVenueInfoList,
      Object principal)
      throws InsertException {
    for(var i = 0; i < uploadVenueInfoList.size() - 1; i++) {
      var hasOverlap = false;
      final var visit = uploadVenueInfoList.get(i);
      for(var j = i + 1; j < uploadVenueInfoList.size(); j++) {
        if(doOverlap(visit, uploadVenueInfoList.get(j))) {
          hasOverlap = true;
          uploadVenueInfoList.remove(j);
          j--;
        }
      }
      if(hasOverlap) {
        uploadVenueInfoList.remove(i);
        i--;
      }
    }
    return uploadVenueInfoList;
  }

  private boolean doOverlap(
      UserUploadPayloadOuterClass.UploadVenueInfo visit1,
      UserUploadPayloadOuterClass.UploadVenueInfo visit2) {
    final var validInterval1 = visit1.getIntervalStartMs() <= visit1.getIntervalEndMs();
    final var validInterval2 = visit2.getIntervalStartMs() <= visit2.getIntervalEndMs();
    return validInterval1 && validInterval2 && !(visit1.getIntervalEndMs() < visit2.getIntervalStartMs() || visit2.getIntervalEndMs() < visit1.getIntervalStartMs());
  }
}
