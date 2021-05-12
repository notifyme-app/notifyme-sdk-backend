package ch.ubique.notifyme.sdk.backend.ws.insert_manager;

import ch.ubique.notifyme.sdk.backend.data.NotifyMeDataServiceV3;
import ch.ubique.notifyme.sdk.backend.model.UserUploadPayloadOuterClass.UploadVenueInfo;
import ch.ubique.notifyme.sdk.backend.ws.insert_manager.insertion_filters.UploadInsertionFilter;
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

    public void addFilter(UploadInsertionFilter filter) {
        this.filterList.add(filter);
    }

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
        }
    }

    private List<UploadVenueInfo> filterUpload(List<UploadVenueInfo> uploadVenueInfoList, String header, Object principal, LocalDateTime now) throws InsertException {
        var headerParts = header.split(";");
        if (headerParts.length < 5) {
            headerParts =
                    List.of("org.example.dp3t", "1.0.0", "0", "Android", "29").toArray(new String[0]);
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
     * Extracts the {@link OSType} from the osString that is given by the client request.
     *
     * @param osString
     * @return
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

    private Version extractOsVersion(String osVersionString) {
        return new Version(osVersionString);
    }

    private Version extractAppVersion(String osAppVersionString, String osMetaInfo) {
        return new Version(osAppVersionString + "+" + osMetaInfo);
    }

}
