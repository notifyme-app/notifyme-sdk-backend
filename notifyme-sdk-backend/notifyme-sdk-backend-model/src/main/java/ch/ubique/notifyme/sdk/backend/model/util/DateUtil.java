/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.ubique.notifyme.sdk.backend.model.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class DateUtil {

    private static final DateTimeFormatter DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm:ss")
                    .withZone(ZoneId.of("Europe/Zurich"));

    private DateUtil() {}

    public static Date toDate(Instant instant) {
        if (instant == null) {
            return null;
        }
        return Date.from(instant);
    }

    public static Long toEpochMilli(Instant instant) {
        if (instant == null) {
            return null;
        }
        return instant.toEpochMilli();
    }

    public static Instant toInstant(Long epochMilli) {
        if (epochMilli == null) {
            return null;
        } else {
            return Instant.ofEpochMilli(epochMilli);
        }
    }

    public static boolean isBucketAligned(Long epochMilli, Long bucketSizeInMs) {
        return epochMilli % bucketSizeInMs == 0;
    }

    public static boolean isInThePast(Long lastKeyBundleTag) {
        return lastKeyBundleTag < System.currentTimeMillis();
    }

    public static long getLastFullBucketEndEpochMilli(Long bucketSizeInMs) {
        return (Instant.now().toEpochMilli() / bucketSizeInMs) * bucketSizeInMs;
    }

    public static String formattedDateTime(final Instant time) {
        return DATETIME_FORMATTER.format(time);
    }
}
