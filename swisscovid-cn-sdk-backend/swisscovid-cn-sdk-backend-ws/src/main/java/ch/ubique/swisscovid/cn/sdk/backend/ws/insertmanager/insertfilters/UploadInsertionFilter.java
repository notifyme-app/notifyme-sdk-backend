package ch.ubique.swisscovid.cn.sdk.backend.ws.insertmanager.insertfilters;

import ch.ubique.swisscovid.cn.sdk.backend.model.UserUploadPayloadOuterClass.UploadVenueInfo;
import ch.ubique.swisscovid.cn.sdk.backend.model.tracekey.TraceKey;
import ch.ubique.swisscovid.cn.sdk.backend.ws.insertmanager.InsertException;
import ch.ubique.swisscovid.cn.sdk.backend.ws.insertmanager.InsertManager;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Interface for filters that can be configured in the {@link InsertManager}
 */
public interface UploadInsertionFilter {

  /**
   * The {@link InsertManager} goes through all configured filters and calls the with a list of
   * {@link UploadVenueInfo}. The filters are applied before transforming the {@param
   * uploadVenueInfoList} into a {@link TraceKey} list to be inserted into the database.
   *
   * @param now current timestamp
   * @param uploadVenueInfoList the list of venue info objects to be inserted
   * @param principal the authorization context which belongs to the uploaded keys. This will
   *     usually be a JWT token.
   * @return Filtered list of {@link UploadVenueInfo} elements
   * @throws InsertException
   */
  public List<UploadVenueInfo> filter(
      LocalDateTime now,
      List<UploadVenueInfo> uploadVenueInfoList,
      Object principal)
      throws InsertException;
}
