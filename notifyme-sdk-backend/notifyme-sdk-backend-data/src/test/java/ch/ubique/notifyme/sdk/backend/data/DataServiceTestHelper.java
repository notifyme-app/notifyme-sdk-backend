/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.ubique.notifyme.sdk.backend.data;

import ch.ubique.notifyme.sdk.backend.model.DiaryEntryWrapperOuterClass.DiaryEntry;
import ch.ubique.notifyme.sdk.backend.model.VenueTypeOuterClass.VenueType;
import ch.ubique.notifyme.sdk.backend.model.event.JavaDiaryEntry;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DataServiceTestHelper {

    private static final TimeZone TZ_EUROPE_ZURICH = TimeZone.getTimeZone("Europe/Zurich");

    private DataServiceTestHelper() {}

    private static long calendarEuropeZurichAsEpochMilli(int year, int dayOfYear) {
        return new Calendar.Builder()
                .set(Calendar.YEAR, year)
                .set(Calendar.DAY_OF_YEAR, dayOfYear)
                .setTimeZone(TZ_EUROPE_ZURICH)
                .build()
                .toInstant()
                .toEpochMilli();
    }

    public static List<JavaDiaryEntry> getDiaryEntries() {
        final var day0Of2020 = calendarEuropeZurichAsEpochMilli(2020, 0);
        final var day1Of2020 = calendarEuropeZurichAsEpochMilli(2020, 1);
        final var day2Of2020 = calendarEuropeZurichAsEpochMilli(2020, 2);
        final var day3Of2020 = calendarEuropeZurichAsEpochMilli(2020, 3);
        final var day4Of2020 = calendarEuropeZurichAsEpochMilli(2020, 4);

        final var diaryEntry0 =
                DiaryEntry.newBuilder()
                        .setName("lecture0")
                        .setLocation("location0")
                        .setRoom("room0")
                        .setVenueType(VenueType.LECTURE_ROOM)
                        .setCheckinTime(day0Of2020)
                        .setCheckOutTIme(day2Of2020)
                        .build();

        final var diaryEntry1 =
                DiaryEntry.newBuilder()
                        .setName("cafeteria1")
                        .setLocation("location1")
                        .setRoom("room1")
                        .setVenueType(VenueType.CAFETERIA)
                        .setCheckinTime(day1Of2020)
                        .setCheckOutTIme(day2Of2020)
                        .build();

        final var diaryEntry2 =
                DiaryEntry.newBuilder()
                        .setName("gym2")
                        .setLocation("location2")
                        .setRoom("room2")
                        .setVenueType(VenueType.GYM)
                        .setCheckinTime(day1Of2020)
                        .setCheckOutTIme(day3Of2020)
                        .build();

        final var diaryEntry3 =
                DiaryEntry.newBuilder()
                        .setName("meeting3")
                        .setLocation("location3")
                        .setRoom("room3")
                        .setVenueType(VenueType.MEETING_ROOM)
                        .setCheckinTime(day3Of2020)
                        .setCheckOutTIme(day4Of2020)
                        .build();

        final var diaryEntry4 =
                DiaryEntry.newBuilder()
                        .setName("library4")
                        .setLocation("location4")
                        .setRoom("room4")
                        .setVenueType(VenueType.LIBRARY)
                        .setCheckinTime(day2Of2020)
                        .setCheckOutTIme(day4Of2020)
                        .build();

        final var diaryEntry5 =
                DiaryEntry.newBuilder()
                        .setName("lecture0")
                        .setLocation("location0")
                        .setRoom("room0")
                        .setVenueType(VenueType.LECTURE_ROOM)
                        .setCheckinTime(day0Of2020)
                        .setCheckOutTIme(day1Of2020)
                        .build();

        final var diaryEntry6 =
                DiaryEntry.newBuilder()
                        .setName("lecture0")
                        .setLocation("location0")
                        .setRoom("room0")
                        .setVenueType(VenueType.LECTURE_ROOM)
                        .setCheckinTime(day1Of2020)
                        .setCheckOutTIme(day2Of2020)
                        .build();

        final var diaryEntry7 =
                DiaryEntry.newBuilder()
                        .setName("lecture0")
                        .setLocation("location0")
                        .setRoom("room0")
                        .setVenueType(VenueType.LECTURE_ROOM)
                        .setCheckinTime(day2Of2020)
                        .setCheckOutTIme(day3Of2020)
                        .build();

        final var diaryEntry8 =
                DiaryEntry.newBuilder()
                        .setName("meeting3")
                        .setLocation("location3")
                        .setRoom("room3")
                        .setVenueType(VenueType.MEETING_ROOM)
                        .setCheckinTime(day2Of2020)
                        .setCheckOutTIme(day4Of2020)
                        .build();

        final var diaryEntry9 =
                DiaryEntry.newBuilder()
                        .setName("meeting3")
                        .setLocation("location3")
                        .setRoom("room3")
                        .setVenueType(VenueType.MEETING_ROOM)
                        .setCheckinTime(day0Of2020)
                        .setCheckOutTIme(day1Of2020)
                        .build();

        return Stream.of(
                        diaryEntry0,
                        diaryEntry1,
                        diaryEntry2,
                        diaryEntry3,
                        diaryEntry4,
                        diaryEntry5,
                        diaryEntry6,
                        diaryEntry7,
                        diaryEntry8,
                        diaryEntry9)
                .map(JavaDiaryEntry::from)
                .collect(Collectors.toList());
    }
}
