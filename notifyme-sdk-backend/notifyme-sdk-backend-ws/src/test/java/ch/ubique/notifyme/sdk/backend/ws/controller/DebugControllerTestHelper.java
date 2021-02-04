/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.ubique.notifyme.sdk.backend.ws.controller;

import ch.ubique.notifyme.sdk.backend.model.ProblematicDiaryEntryWrapperOuterClass.ProblematicDiaryEntry;
import ch.ubique.notifyme.sdk.backend.model.ProblematicDiaryEntryWrapperOuterClass.ProblematicDiaryEntryWrapper;
import ch.ubique.notifyme.sdk.backend.model.VenueTypeOuterClass.VenueType;
import java.util.Calendar;

public class DebugControllerTestHelper {

    private DebugControllerTestHelper() {}

    public static ProblematicDiaryEntryWrapper getTestProblematicDiaryEntryWrapper() {
        final long startOf2020 =
                new Calendar.Builder()
                        .set(Calendar.YEAR, 2020)
                        .set(Calendar.DAY_OF_YEAR, 0)
                        .build()
                        .toInstant()
                        .toEpochMilli();

        final long startOf2021 =
                new Calendar.Builder()
                        .set(Calendar.YEAR, 2021)
                        .set(Calendar.DAY_OF_YEAR, 0)
                        .build()
                        .toInstant()
                        .toEpochMilli();

        final long startOf2022 =
                new Calendar.Builder()
                        .set(Calendar.YEAR, 2022)
                        .set(Calendar.DAY_OF_YEAR, 0)
                        .build()
                        .toInstant()
                        .toEpochMilli();

        final ProblematicDiaryEntry diaryEntry0 =
                ProblematicDiaryEntry.newBuilder()
                        .setName("name0")
                        .setLocation("location0")
                        .setRoom("room0")
                        .setVenueType(VenueType.LECTURE_ROOM)
                        .setCheckinTime(startOf2020)
                        .setCheckOutTIme(startOf2021)
                        .build();

        final ProblematicDiaryEntry diaryEntry1 =
                ProblematicDiaryEntry.newBuilder()
                        .setName("name1")
                        .setLocation("location1")
                        .setRoom("room1")
                        .setVenueType(VenueType.CAFETERIA)
                        .setCheckinTime(startOf2021)
                        .setCheckOutTIme(startOf2022)
                        .build();

        final ProblematicDiaryEntryWrapper wrapper =
                ProblematicDiaryEntryWrapper.newBuilder()
                        .addDiaryEntries(diaryEntry0)
                        .addDiaryEntries(diaryEntry1)
                        .build();

        return wrapper;
    }
}
