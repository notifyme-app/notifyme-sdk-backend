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

import ch.ubique.notifyme.sdk.backend.model.ProblematicDiaryEntryWrapperOuterClass.ProblematicDiaryEntry;
import ch.ubique.notifyme.sdk.backend.model.VenueTypeOuterClass.VenueType;
import ch.ubique.notifyme.sdk.backend.model.event.DiaryEntry;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DataServiceTestHelper {

    private DataServiceTestHelper() {}

    public static List<DiaryEntry> getDiaryEntries() {
        final long day0Of2020 =
                new Calendar.Builder()
                        .set(Calendar.YEAR, 2020)
                        .set(Calendar.DAY_OF_YEAR, 0)
                        .build()
                        .toInstant()
                        .toEpochMilli();
        final long day1Of2020 =
                new Calendar.Builder()
                        .set(Calendar.YEAR, 2020)
                        .set(Calendar.DAY_OF_YEAR, 1)
                        .build()
                        .toInstant()
                        .toEpochMilli();
        final long day2Of2020 =
                new Calendar.Builder()
                        .set(Calendar.YEAR, 2020)
                        .set(Calendar.DAY_OF_YEAR, 2)
                        .build()
                        .toInstant()
                        .toEpochMilli();
        final long day3Of2020 =
                new Calendar.Builder()
                        .set(Calendar.YEAR, 2020)
                        .set(Calendar.DAY_OF_YEAR, 3)
                        .build()
                        .toInstant()
                        .toEpochMilli();
        final long day4Of2020 =
                new Calendar.Builder()
                        .set(Calendar.YEAR, 2020)
                        .set(Calendar.DAY_OF_YEAR, 4)
                        .build()
                        .toInstant()
                        .toEpochMilli();

        final ProblematicDiaryEntry diaryEntry0 =
                ProblematicDiaryEntry.newBuilder()
                        .setName("lecture0")
                        .setLocation("location0")
                        .setRoom("room0")
                        .setVenueType(VenueType.LECTURE_ROOM)
                        .setCheckinTime(day0Of2020)
                        .setCheckOutTIme(day2Of2020)
                        .build();

        final ProblematicDiaryEntry diaryEntry1 =
                ProblematicDiaryEntry.newBuilder()
                        .setName("cafeteria1")
                        .setLocation("location1")
                        .setRoom("room1")
                        .setVenueType(VenueType.CAFETERIA)
                        .setCheckinTime(day1Of2020)
                        .setCheckOutTIme(day2Of2020)
                        .build();

        final ProblematicDiaryEntry diaryEntry2 =
                ProblematicDiaryEntry.newBuilder()
                        .setName("gym2")
                        .setLocation("location2")
                        .setRoom("room2")
                        .setVenueType(VenueType.GYM)
                        .setCheckinTime(day1Of2020)
                        .setCheckOutTIme(day3Of2020)
                        .build();

        final ProblematicDiaryEntry diaryEntry3 =
                ProblematicDiaryEntry.newBuilder()
                        .setName("meeting3")
                        .setLocation("location3")
                        .setRoom("room3")
                        .setVenueType(VenueType.MEETING_ROOM)
                        .setCheckinTime(day3Of2020)
                        .setCheckOutTIme(day4Of2020)
                        .build();

        final ProblematicDiaryEntry diaryEntry4 =
                ProblematicDiaryEntry.newBuilder()
                        .setName("library4")
                        .setLocation("location4")
                        .setRoom("room4")
                        .setVenueType(VenueType.LIBRARY)
                        .setCheckinTime(day2Of2020)
                        .setCheckOutTIme(day4Of2020)
                        .build();

        final ProblematicDiaryEntry diaryEntry5 =
                ProblematicDiaryEntry.newBuilder()
                        .setName("lecture0")
                        .setLocation("location0")
                        .setRoom("room0")
                        .setVenueType(VenueType.LECTURE_ROOM)
                        .setCheckinTime(day0Of2020)
                        .setCheckOutTIme(day1Of2020)
                        .build();

        final ProblematicDiaryEntry diaryEntry6 =
                ProblematicDiaryEntry.newBuilder()
                        .setName("lecture0")
                        .setLocation("location0")
                        .setRoom("room0")
                        .setVenueType(VenueType.LECTURE_ROOM)
                        .setCheckinTime(day1Of2020)
                        .setCheckOutTIme(day2Of2020)
                        .build();

        final ProblematicDiaryEntry diaryEntry7 =
                ProblematicDiaryEntry.newBuilder()
                        .setName("lecture0")
                        .setLocation("location0")
                        .setRoom("room0")
                        .setVenueType(VenueType.LECTURE_ROOM)
                        .setCheckinTime(day2Of2020)
                        .setCheckOutTIme(day3Of2020)
                        .build();

        final ProblematicDiaryEntry diaryEntry8 =
                ProblematicDiaryEntry.newBuilder()
                        .setName("meeting3")
                        .setLocation("location3")
                        .setRoom("room3")
                        .setVenueType(VenueType.MEETING_ROOM)
                        .setCheckinTime(day2Of2020)
                        .setCheckOutTIme(day4Of2020)
                        .build();

        final ProblematicDiaryEntry diaryEntry9 =
                ProblematicDiaryEntry.newBuilder()
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
                .map(DiaryEntry::from)
                .collect(Collectors.toList());
    }
}
