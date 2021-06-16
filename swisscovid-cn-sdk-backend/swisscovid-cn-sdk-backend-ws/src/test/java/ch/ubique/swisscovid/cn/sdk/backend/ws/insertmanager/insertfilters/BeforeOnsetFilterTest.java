package ch.ubique.swisscovid.cn.sdk.backend.ws.insertmanager.insertfilters;

import ch.ubique.swisscovid.cn.sdk.backend.model.UserUploadPayloadOuterClass.UploadVenueInfo;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.springframework.security.oauth2.jwt.Jwt;

public class BeforeOnsetFilterTest extends UploadInsertionFilterTest {

    // We don't care about the current time in the filter, we just need a common timestamp for all
    // methods below
    private static final LocalDateTime currentTime =
            LocalDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT);

    @Override
    List<List<UploadVenueInfo>> getValidVenueInfo() {
        final List<UploadVenueInfo> venueInfoList = new ArrayList<>();
        LocalDateTime start = currentTime.minusDays(1);
        LocalDateTime end = start.plusHours(1);
        // venue visit one day after onset
        final var venueInfoCase1 = getVenueInfo(start, end);
        venueInfoList.add(venueInfoCase1);
        start = currentTime.minusDays(2);
        end = start.plusHours(1);
        // venue visit same day as onset
        final var venueInfoCase2 = getVenueInfo(start, end);
        venueInfoList.add(venueInfoCase2);
        return List.of(venueInfoList);
    }

    @Override
    List<List<UploadVenueInfo>> getInvalidVenueInfo() {
        final List<UploadVenueInfo> venueInfoList = new ArrayList<>();
        LocalDateTime start = currentTime.minusDays(3);
        LocalDateTime end = start.plusHours(1);
        // venue visit one day before onset
        final var venueInfo = getVenueInfo(start, end);
        venueInfoList.add(venueInfo);
        return List.of(venueInfoList);
    }

    @Override
    UploadInsertionFilter insertionFilter() {
        return new BeforeOnsetFilter();
    }

    @Override
    public Jwt getToken(LocalDateTime now) throws Exception {
        final var onset =
                currentTime.minusDays(2).truncatedTo(ChronoUnit.DAYS).format(DATE_FORMATTER);
        final var expiry = currentTime.plusMinutes(5).toInstant(ZoneOffset.UTC);
        return jwtDecoder.decode(
                tokenHelper.createToken(
                        onset,
                        "0",
                        "notifyMe",
                        "userupload",
                        Date.from(expiry),
                        true,
                        currentTime.toInstant(ZoneOffset.UTC)));
    }
}
