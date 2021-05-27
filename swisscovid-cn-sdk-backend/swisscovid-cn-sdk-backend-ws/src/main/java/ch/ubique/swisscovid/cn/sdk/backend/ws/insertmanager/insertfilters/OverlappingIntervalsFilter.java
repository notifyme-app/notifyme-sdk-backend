package ch.ubique.swisscovid.cn.sdk.backend.ws.insertmanager.insertfilters;

import ch.ubique.swisscovid.cn.sdk.backend.model.UserUploadPayloadOuterClass;
import ch.ubique.swisscovid.cn.sdk.backend.model.UserUploadPayloadOuterClass.UploadVenueInfo;
import ch.ubique.swisscovid.cn.sdk.backend.ws.insertmanager.InsertException;

import ch.ubique.swisscovid.cn.sdk.backend.ws.insertmanager.OverlappingIntervalsException;
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
      final var visit = uploadVenueInfoList.get(i);
      for(var j = i + 1; j < uploadVenueInfoList.size(); j++) {
        if(doOverlap(visit, uploadVenueInfoList.get(j))) {
          throw new OverlappingIntervalsException();
        }
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
