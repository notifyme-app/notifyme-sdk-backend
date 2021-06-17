package ch.ubique.swisscovid.cn.sdk.backend.ws.util;

import ch.ubique.swisscovid.cn.sdk.backend.model.UserUploadPayloadOuterClass.UploadVenueInfo;
import ch.ubique.swisscovid.cn.sdk.backend.ws.util.CryptoUtil.NoncesAndNotificationKey;
import com.google.protobuf.ByteString;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

public class VenueInfoHelper {

    private static final int INTERVAL_LENGTH = 3600; // interval length seconds
    private final CryptoWrapper cryptoWrapper;

    public VenueInfoHelper(CryptoWrapper cryptoWrapper) {
        this.cryptoWrapper = cryptoWrapper;
    }

    public List<UploadVenueInfo> getVenueInfo(
            LocalDateTime start,
            LocalDateTime end,
            boolean fake,
            NoncesAndNotificationKey noncesAndNotificationKey) {

        final var startTimestamp = start.toInstant(ZoneOffset.UTC).toEpochMilli();
        final var endTimestamp = end.toInstant(ZoneOffset.UTC).toEpochMilli();
        final var crypto = cryptoWrapper.getCryptoUtil();
        final var uploadVenueInfoList = new ArrayList<UploadVenueInfo>();

        if (noncesAndNotificationKey == null) {
            noncesAndNotificationKey = crypto.getNoncesAndNotificationKey(crypto.createNonce(256));
        }

        byte[] preid =
                crypto.cryptoHashSHA256(
                        crypto.concatenate(
                                "CN-PREID".getBytes(StandardCharsets.US_ASCII),
                                "payload".getBytes(StandardCharsets.US_ASCII),
                                noncesAndNotificationKey.noncePreId));

        List<Long> intervalStarts =
                getAffectedIntervalStarts(startTimestamp / 1000, endTimestamp / 1000);
        for (Long intervalStart : intervalStarts) {
            byte[] timekey =
                    crypto.cryptoHashSHA256(
                            crypto.concatenate(
                                    "CN-TIMEKEY".getBytes(StandardCharsets.US_ASCII),
                                    crypto.longToBytes(INTERVAL_LENGTH),
                                    crypto.longToBytes(intervalStart),
                                    noncesAndNotificationKey.nonceTimekey));
            uploadVenueInfoList.add(
                    UploadVenueInfo.newBuilder()
                            .setPreId(ByteString.copyFrom(preid))
                            .setTimeKey(ByteString.copyFrom(timekey))
                            .setIntervalStartMs(Math.max(intervalStart * 1000, startTimestamp))
                            .setIntervalEndMs(
                                    Math.min(
                                            (intervalStart + INTERVAL_LENGTH) * 1000, endTimestamp))
                            .setNotificationKey(
                                    ByteString.copyFrom(noncesAndNotificationKey.notificationKey))
                            .setFake(fake)
                            .build());
        }
        return uploadVenueInfoList;
    }

    public List<Long> getAffectedIntervalStarts(long arrivalTime, long departureTime) {
        long start = arrivalTime / INTERVAL_LENGTH;
        long end = departureTime / INTERVAL_LENGTH;
        List<Long> result = new ArrayList<>();
        for (long i = start; i <= end; i += 1) {
            result.add(i * INTERVAL_LENGTH);
        }
        return result;
    }
}
