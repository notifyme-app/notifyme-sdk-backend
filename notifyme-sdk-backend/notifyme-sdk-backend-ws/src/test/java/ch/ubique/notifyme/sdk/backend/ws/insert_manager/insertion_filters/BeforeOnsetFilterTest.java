package ch.ubique.notifyme.sdk.backend.ws.insert_manager.insertion_filters;

import ch.ubique.notifyme.sdk.backend.model.UserUploadPayloadOuterClass.UploadVenueInfo;
import com.google.protobuf.ByteString;
import org.springframework.security.oauth2.jwt.Jwt;

import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BeforeOnsetFilterTest extends UploadInsertionFilterTest {

    // We don't care about the current time in the filter, we just need a common timestamp for all methods below
    private static final LocalDateTime currentTime = LocalDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT);


    @Override
    List<UploadVenueInfo> getValidVenueInfo() {
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
        return venueInfoList;
    }

    @Override
    List<UploadVenueInfo> getInvalidVenueInfo() {
        final List<UploadVenueInfo> venueInfoList = new ArrayList<>();
        LocalDateTime start = currentTime.minusDays(3);
        LocalDateTime end = start.plusHours(1);
        // venue visit one day before onset
        final var venueInfo = getVenueInfo(start, end);
        venueInfoList.add(venueInfo);
        return venueInfoList;
    }

    @Override
    UploadInsertionFilter insertionFilter() {
        return new BeforeOnsetFilter();
    }

    @Override
    public Jwt getToken(LocalDateTime now) throws Exception {
        final var onset = currentTime.minusDays(2).truncatedTo(ChronoUnit.DAYS).format(DATE_FORMATTER);
        final var expiry = currentTime.plusMinutes(5).toInstant(ZoneOffset.UTC);
        return jwtDecoder.decode(tokenHelper.createToken(
                onset, "0", "notifyMe", "userupload", Date.from(expiry), true, currentTime.toInstant(ZoneOffset.UTC)));
    }

    private UploadVenueInfo getVenueInfo(
            LocalDateTime start, LocalDateTime end) {
        final var crypto = cryptoWrapper.getCryptoUtilV3();
        final var noncesAndNotificationKey =
                crypto.getNoncesAndNotificationKey(crypto.createNonce(256));
        byte[] preid =
                crypto.cryptoHashSHA256(
                        crypto.concatenate(
                                "CN-PREID".getBytes(StandardCharsets.US_ASCII),
                                "payload".getBytes(StandardCharsets.US_ASCII),
                                noncesAndNotificationKey.noncePreId));
        byte[] timekey =
                crypto.cryptoHashSHA256(
                        crypto.concatenate(
                                "CN-TIMEKEY".getBytes(StandardCharsets.US_ASCII),
                                crypto.longToBytes(3600L),
                                crypto.longToBytes(start.toInstant(ZoneOffset.UTC).getEpochSecond()),
                                noncesAndNotificationKey.nonceTimekey));
        return UploadVenueInfo.newBuilder()
                .setPreId(ByteString.copyFrom(preid))
                .setTimeKey(ByteString.copyFrom(timekey))
                .setIntervalStartMs(start.toInstant(ZoneOffset.UTC).toEpochMilli())
                .setIntervalEndMs(end.toInstant(ZoneOffset.UTC).toEpochMilli())
                .setNotificationKey(ByteString.copyFrom(noncesAndNotificationKey.notificationKey))
                .setFake(false)
                .build();
    }

}

