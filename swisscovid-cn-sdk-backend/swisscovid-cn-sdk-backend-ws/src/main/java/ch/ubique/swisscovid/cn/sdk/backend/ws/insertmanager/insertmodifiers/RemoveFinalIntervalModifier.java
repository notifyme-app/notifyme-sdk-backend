package ch.ubique.swisscovid.cn.sdk.backend.ws.insertmanager.insertmodifiers;

import ch.ubique.notifyme.sdk.backend.model.UserUploadPayloadOuterClass.UploadVenueInfo;
import ch.ubique.swisscovid.cn.sdk.backend.ws.insertmanager.InsertException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * TODO: Implement once implemented in frontend
 */
public class RemoveFinalIntervalModifier implements
    UploadInsertionModifier {

  @Override
  public List<UploadVenueInfo> modify(LocalDateTime now, List<UploadVenueInfo> uploadVenueInfoList,
      Object principal) throws InsertException {
    return uploadVenueInfoList;
  }
}
