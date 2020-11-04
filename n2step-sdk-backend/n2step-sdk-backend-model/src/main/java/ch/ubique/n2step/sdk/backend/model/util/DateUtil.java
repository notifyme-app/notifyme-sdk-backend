package ch.ubique.n2step.sdk.backend.model.util;

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
}
