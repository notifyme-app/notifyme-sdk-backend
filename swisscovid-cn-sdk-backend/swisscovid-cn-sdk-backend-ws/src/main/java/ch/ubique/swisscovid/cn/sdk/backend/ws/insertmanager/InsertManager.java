package ch.ubique.swisscovid.cn.sdk.backend.ws.insertmanager;

import ch.ubique.swisscovid.cn.sdk.backend.data.InteractionDurationDataService;
import ch.ubique.swisscovid.cn.sdk.backend.data.KPIDataService;
import ch.ubique.swisscovid.cn.sdk.backend.data.SwissCovidDataService;
import ch.ubique.swisscovid.cn.sdk.backend.model.UserUploadPayloadOuterClass.UploadVenueInfo;
import ch.ubique.swisscovid.cn.sdk.backend.model.UserUploadPayloadOuterClass.UserUploadPayload;
import ch.ubique.swisscovid.cn.sdk.backend.model.tracekey.TraceKey;
import ch.ubique.swisscovid.cn.sdk.backend.ws.insertmanager.insertfilters.UploadInsertionFilter;
import ch.ubique.swisscovid.cn.sdk.backend.ws.insertmanager.insertmodifiers.UploadInsertionModifier;
import ch.ubique.swisscovid.cn.sdk.backend.ws.security.RequestValidator;
import ch.ubique.swisscovid.cn.sdk.backend.ws.security.SwissCovidJwtRequestValidator;
import ch.ubique.swisscovid.cn.sdk.backend.ws.util.CryptoWrapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InsertManager {

    private static final Logger logger = LoggerFactory.getLogger(InsertManager.class);

    private final List<UploadInsertionFilter> filterList = new ArrayList<>();
    private final List<UploadInsertionModifier> modifierList = new ArrayList<>();

    private final CryptoWrapper cryptoWrapper;
    private final SwissCovidDataService swissCovidDataService;
    private final InteractionDurationDataService interactionDurationDataService;
    private final KPIDataService kpiDataService;

    private final RequestValidator requestValidator = new SwissCovidJwtRequestValidator();

    public InsertManager(
            CryptoWrapper cryptoWrapper,
            SwissCovidDataService swissCovidDataService,
            InteractionDurationDataService interactionDurationDataService,
            KPIDataService kpiDataService) {
        this.cryptoWrapper = cryptoWrapper;
        this.swissCovidDataService = swissCovidDataService;
        this.interactionDurationDataService = interactionDurationDataService;
        this.kpiDataService = kpiDataService;
    }

    /**
     * Adds a filter to the list of filters to be applied to a {@link UploadVenueInfo} object before
     * inserting it into the database as a {@link TraceKey}.
     *
     * @param filter to be added to the filter list. The filter method must return a filtered list
     *     of VenueInfo's
     */
    public void addFilter(UploadInsertionFilter filter) {
        this.filterList.add(filter);
    }

    /**
     * Adds a modifer to the list of modifiers to be applied to a {@link UploadVenueInfo} object
     * before filtering and inserting it into the database as a {@link TraceKey}.
     *
     * @param modifier to be added to the modifier list. The modifier method must return the a list
     *     of modified VenueInfo's
     */
    public void addModifier(UploadInsertionModifier modifier) {
        this.modifierList.add(modifier);
    }

    /**
     * Applies the stored list of filters, transforms any remaining venueInfo's into trace keys, and
     * stores them to the database.
     *
     * @param uploadPayload upload payload containing list of VenueInfo objects to be stored to the
     *     database
     * @param principal the authorization context which belongs to the uploaded keys. This will
     *     usually be a JWT token.
     * @param now Current timestamp
     * @throws InsertException
     */
    public void insertIntoDatabase(
            UserUploadPayload uploadPayload, Object principal, LocalDateTime now)
            throws InsertException {
        final var uploadVenueInfoList = uploadPayload.getVenueInfosList();
        if (uploadVenueInfoList != null && !uploadVenueInfoList.isEmpty()) {
            final var modifiedVenueInfoList = modifyUpload(uploadVenueInfoList, principal, now);
            final var filteredVenueInfoList = filterUpload(modifiedVenueInfoList, principal, now);
            final var traceKeys =
                    cryptoWrapper.getCryptoUtil().createTraceV3ForUserUpload(filteredVenueInfoList);
            if (!requestValidator.isFakeRequest(principal)) {
                interactionDurationDataService.insertInteraction(
                        uploadPayload.getUserInteractionDurationMs());
                kpiDataService.insertCheckinCount(now, getNoOfCheckins(filteredVenueInfoList));
                if (!traceKeys.isEmpty()) {
                    swissCovidDataService.insertTraceKey(traceKeys);
                }
            }
        } else {
            // Invalid upload: Empty VenueInfo list
            throw new NoVenueInfosException();
        }
    }

    private List<UploadVenueInfo> modifyUpload(
            List<UploadVenueInfo> uploadVenueInfoList, Object principal, LocalDateTime now)
            throws InsertException {
        var venueInfoList = uploadVenueInfoList;
        for (UploadInsertionModifier insertionModifier : modifierList) {
            venueInfoList = insertionModifier.modify(now, venueInfoList, principal);
        }
        return venueInfoList;
    }

    private List<UploadVenueInfo> filterUpload(
            List<UploadVenueInfo> uploadVenueInfoList, Object principal, LocalDateTime now)
            throws InsertException {
        var venueInfoList = uploadVenueInfoList;
        for (UploadInsertionFilter insertionFilter : filterList) {
            venueInfoList = insertionFilter.filter(now, venueInfoList, principal);
        }
        return venueInfoList;
    }

    private int getNoOfCheckins(List<UploadVenueInfo> uploadVenueInfoList) {
        var checkins = 0;
        for (var i = 0; i < uploadVenueInfoList.size(); i++) {
            final var venueInfo = uploadVenueInfoList.get(i);
            if (i == uploadVenueInfoList.size() - 1) {
                checkins++;
            } else {
                final var nextVenueInfo = uploadVenueInfoList.get(i + 1);
                if (!venueInfo.getPreId().equals(nextVenueInfo.getPreId())
                        || venueInfo.getIntervalEndMs() != nextVenueInfo.getIntervalStartMs()) {
                    checkins++;
                }
            }
        }
        return checkins;
    }
}
