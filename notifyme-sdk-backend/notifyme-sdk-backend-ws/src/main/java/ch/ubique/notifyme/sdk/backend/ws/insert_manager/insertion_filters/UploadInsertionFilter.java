package ch.ubique.notifyme.sdk.backend.ws.insert_manager.insertion_filters;

import ch.ubique.notifyme.sdk.backend.model.UserUploadPayloadOuterClass.UploadVenueInfo;
import ch.ubique.notifyme.sdk.backend.model.tracekey.v3.TraceKey;
import ch.ubique.notifyme.sdk.backend.ws.insert_manager.InsertException;
import ch.ubique.notifyme.sdk.backend.ws.insert_manager.InsertManager;
import ch.ubique.notifyme.sdk.backend.ws.insert_manager.OSType;
import ch.ubique.notifyme.sdk.backend.ws.semver.Version;

import java.time.Instant;
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
   * @param osType the os type of the client
   * @param osVersion the os version of the client
   * @param appVersion the app version of the client
   * @param principal the authorization context which belongs to the uploaded keys. This will
   *     usually be a JWT token.
   * @return Filtered list of {@link UploadVenueInfo} elements
   * @throws InsertException
   */
  public List<UploadVenueInfo> filter(
          LocalDateTime now,
          List<UploadVenueInfo> uploadVenueInfoList,
          OSType osType,
          Version osVersion,
          Version appVersion,
          Object principal)
      throws InsertException;
}
