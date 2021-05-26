package ch.ubique.swisscovid.cn.sdk.backend.ws.insertmanager.insertmodifiers;

import ch.ubique.notifyme.sdk.backend.model.UserUploadPayloadOuterClass.UploadVenueInfo;
import ch.ubique.notifyme.sdk.backend.model.tracekey.v3.TraceKey;
import ch.ubique.swisscovid.cn.sdk.backend.ws.insertmanager.InsertException;
import ch.ubique.swisscovid.cn.sdk.backend.ws.insertmanager.InsertManager;
import java.time.LocalDateTime;
import java.util.List;

public interface UploadInsertionModifier {

  /**
   * The {@link InsertManager} goes through all configured modifiers and calls each modifier with a list of
   * {@link UploadVenueInfo}. The modifiers are applied before filtering and transforming the {@param
   * uploadVenueInfoList} into a {@link TraceKey} list to be inserted into the database.
   *
   * @param now current timestamp
   * @param uploadVenueInfoList the list of venue info objects to be inserted
   * @param principal the authorization context which belongs to the uploaded keys. This will
   *     usually be a JWT token.
   * @return Filtered list of {@link UploadVenueInfo} elements
   * @throws InsertException
   */
  public List<UploadVenueInfo> modify(LocalDateTime now,
      List<UploadVenueInfo> uploadVenueInfoList,
      Object principal) throws InsertException;

}
