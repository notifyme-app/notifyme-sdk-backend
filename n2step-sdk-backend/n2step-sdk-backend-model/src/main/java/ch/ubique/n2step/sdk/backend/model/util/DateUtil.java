package ch.ubique.n2step.sdk.backend.model.util;

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
}
