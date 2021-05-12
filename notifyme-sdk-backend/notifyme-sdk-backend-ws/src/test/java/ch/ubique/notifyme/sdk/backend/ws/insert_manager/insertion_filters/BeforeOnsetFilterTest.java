package ch.ubique.notifyme.sdk.backend.ws.insert_manager.insertion_filters;

import ch.ubique.notifyme.sdk.backend.model.UserUploadPayloadOuterClass;
import com.google.protobuf.ByteString;
import org.springframework.security.oauth2.jwt.Jwt;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public class BeforeOnsetFilterTest extends UploadInsertionFilterTest {

    // We don't care about the current time in the filter, we just need a common timestamp for all methods below
    private static final LocalDateTime currentTime = LocalDateTime.now();


    @Override
    UserUploadPayloadOuterClass.UploadVenueInfo getValidVenueInfo() {
        LocalDateTime start = currentTime.minusDays(1);
        LocalDateTime end = start.plusHours(1);
        return getVenueInfo(start, end);
    }

    @Override
    UserUploadPayloadOuterClass.UploadVenueInfo getInvalidVenueInfo() {
        LocalDateTime start = currentTime.minusDays(3);
        LocalDateTime end = start.plusHours(1);
        return getVenueInfo(start, end);
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
                onset, "0", "notifyMe", "userupload", Date.from(expiry), true));
    }

    private UserUploadPayloadOuterClass.UploadVenueInfo getVenueInfo(
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
        final var startEpochSecond = start.toInstant(ZoneOffset.UTC).toEpochMilli();
        return UserUploadPayloadOuterClass.UploadVenueInfo.newBuilder()
                .setPreId(ByteString.copyFrom(preid))
                .setTimeKey(ByteString.copyFrom(timekey))
                .setIntervalStartMs(startEpochSecond)
                .setIntervalEndMs(end.toInstant(ZoneOffset.UTC).toEpochMilli())
                .setNotificationKey(ByteString.copyFrom(noncesAndNotificationKey.notificationKey))
                .setFake(false)
                .build();
    }

}

