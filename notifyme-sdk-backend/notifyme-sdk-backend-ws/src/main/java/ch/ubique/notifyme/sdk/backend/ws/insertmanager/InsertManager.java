package ch.ubique.notifyme.sdk.backend.ws.insertmanager;

import ch.ubique.notifyme.sdk.backend.data.NotifyMeDataServiceV3;
import ch.ubique.notifyme.sdk.backend.model.UserUploadPayloadOuterClass.UploadVenueInfo;
import ch.ubique.notifyme.sdk.backend.ws.insertmanager.insertfilters.UploadInsertionFilter;
import ch.ubique.notifyme.sdk.backend.ws.semver.Version;
import ch.ubique.notifyme.sdk.backend.ws.util.CryptoWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class InsertManager {

    private static final Logger logger = LoggerFactory.getLogger(InsertManager.class);

    private final List<UploadInsertionFilter> filterList = new ArrayList<>();

    private final CryptoWrapper cryptoWrapper;
    private final NotifyMeDataServiceV3 notifyMeDataServiceV3;

    public InsertManager(CryptoWrapper cryptoWrapper, NotifyMeDataServiceV3 notifyMeDataServiceV3) {
        this.cryptoWrapper = cryptoWrapper;
        this.notifyMeDataServiceV3 = notifyMeDataServiceV3;
    }

    /**
     * Adds a filter to the list of filters to be applied to a {@link UploadVenueInfo} object before inserting it
     * into the database as a {@link ch.ubique.notifyme.sdk.backend.model.tracekey.v3.TraceKey}.
     * @param filter to be added to the filter list. The filter method must return a filtered list of VenueInfo's
     */
    public void addFilter(UploadInsertionFilter filter) {
        this.filterList.add(filter);
    }

    /**
     * Applies the stored list of filters, transforms any remaining venueInfo's into trace keys, and stores them to the
     * database.
     * @param uploadVenueInfoList List of UploadVenueInfo objects to be stored to the database
     * @param header User-Agent header as included in the user-upload request
     * @param principal the authorization context which belongs to the uploaded keys. This will usually be a JWT token.
     * @param now Current timestamp
     * @throws InsertException
     */
    public void insertIntoDatabase(
            List<UploadVenueInfo> uploadVenueInfoList,
            String header,
            Object principal,
            LocalDateTime now
    ) throws InsertException {
        if (uploadVenueInfoList != null && !uploadVenueInfoList.isEmpty()) {
            final var filteredVenueInfoList = filterUpload(uploadVenueInfoList, header, principal, now);
            final var traceKeys = cryptoWrapper.getCryptoUtilV3().createTraceV3ForUserUpload(filteredVenueInfoList);
            if (!traceKeys.isEmpty()) {
                notifyMeDataServiceV3.insertTraceKey(traceKeys);
            }
        } else {
          // Invalid upload: Empty VenueInfo list
          throw new NoVenueInfosException();
        }
    }

    private List<UploadVenueInfo> filterUpload(List<UploadVenueInfo> uploadVenueInfoList, String header, Object principal, LocalDateTime now) throws InsertException {
        var headerParts = header.split(";");
        if (headerParts.length < 5) {
            headerParts =
                    List.of("org.example.notifyMe", "1.0.0", "0", "Android", "29").toArray(new String[0]);
            logger.error("We received an invalid header, setting default.");
        }

        // Map the given headers to os type, os version and app version. Examples are:
        // ch.admin.bag.dp36;1.0.7;200724.1105.215;iOS;13.6
        // ch.admin.bag.dp3t.dev;1.0.7;1595591959493;Android;29
        var osType = exctractOS(headerParts[3]);
        var osVersion = extractOsVersion(headerParts[4]);
        var appVersion = extractAppVersion(headerParts[1], headerParts[2]);

        var venueInfoList = uploadVenueInfoList;
        for (UploadInsertionFilter insertionFilter: filterList) {
            venueInfoList = insertionFilter.filter(now, venueInfoList, osType, osVersion, appVersion, principal);
        }
        return venueInfoList;
    }

  /**
   * Extracts the {@link OSType} from the osString that is given by the client request's
   * user-agent header.
   */
  private OSType exctractOS(String osString) {
        var result = OSType.ANDROID;
        switch (osString.toLowerCase()) {
            case "ios":
                result = OSType.IOS;
                break;
            case "android":
                break;
            default:
                result = OSType.ANDROID;
        }
        return result;
    }

  /**
   * Extracts the {@link Version} from the osVersionString that is given by the client request's
   * user-agent header.
   */
  private Version extractOsVersion(String osVersionString) {
        return new Version(osVersionString);
    }

    /**
     * Extracts the {@link Version} from the osAppVersionString and osMetaInfo that are given by the client request's
     * user-agent header.
     */
    private Version extractAppVersion(String osAppVersionString, String osMetaInfo) {
        return new Version(osAppVersionString + "+" + osMetaInfo);
    }

}
