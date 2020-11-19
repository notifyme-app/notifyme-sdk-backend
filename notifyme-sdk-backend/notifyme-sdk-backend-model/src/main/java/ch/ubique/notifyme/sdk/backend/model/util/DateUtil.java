package ch.ubique.notifyme.sdk.backend.model.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

public class DateUtil {
    public static Date toDate(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return new Date(dateTime.toInstant(ZoneOffset.UTC).toEpochMilli());
    }

    public static Long toEpochMilli(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    public static LocalDateTime toLocalDateTime(Long epochMilli) {
        if (epochMilli == null) {
            return null;
        } else {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), ZoneOffset.UTC);
        }
    }

    private static long bucketSizeInMs = 3600000; // buckets aligned at full hour

    public static boolean isBucketAligned(Long epochMilli) {
        return epochMilli % bucketSizeInMs == 0;
    }

    public static boolean isInThePast(Long lastKeyBundleTag) {
        return lastKeyBundleTag < System.currentTimeMillis();
    }

    public static long getLastFullBucketEndEpochMilli() {
        return (System.currentTimeMillis() / bucketSizeInMs) * bucketSizeInMs;
    }
}
