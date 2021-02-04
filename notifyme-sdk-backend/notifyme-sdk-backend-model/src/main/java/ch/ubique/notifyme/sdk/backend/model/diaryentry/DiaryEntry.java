/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.ubique.notifyme.sdk.backend.model.diaryentry;

import ch.ubique.notifyme.sdk.backend.model.ProblematicDiaryEntryWrapperOuterClass.ProblematicDiaryEntry;
import ch.ubique.notifyme.sdk.backend.model.VenueTypeOuterClass.VenueType;
import ch.ubique.notifyme.sdk.backend.model.util.DateUtil;
import java.time.Instant;
import javax.validation.constraints.NotNull;

public class DiaryEntry {

    private Integer id;

    @NotNull private String name;

    @NotNull private String location;

    @NotNull private String room;

    @NotNull private VenueType venueType;

    @NotNull private Instant checkinTime;

    @NotNull private Instant checkoutTime;

    public static DiaryEntry from(final ProblematicDiaryEntry problematicDiaryEntry) {
        final var diaryEntry = new DiaryEntry();
        diaryEntry.setName(problematicDiaryEntry.getName());
        diaryEntry.setLocation(problematicDiaryEntry.getLocation());
        diaryEntry.setRoom(problematicDiaryEntry.getRoom());
        diaryEntry.setVenueType(problematicDiaryEntry.getVenueType());
        diaryEntry.setCheckinTime(DateUtil.toInstant(problematicDiaryEntry.getCheckinTime()));
        diaryEntry.setCheckoutTime(DateUtil.toInstant(problematicDiaryEntry.getCheckOutTIme()));
        return diaryEntry;
    }

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(final String location) {
        this.location = location;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(final String room) {
        this.room = room;
    }

    public VenueType getVenueType() {
        return venueType;
    }

    public void setVenueType(final VenueType venueType) {
        this.venueType = venueType;
    }

    public Instant getCheckinTime() {
        return checkinTime;
    }

    public void setCheckinTime(final Instant checkinTime) {
        this.checkinTime = checkinTime;
    }

    public Instant getCheckoutTime() {
        return checkoutTime;
    }

    public void setCheckoutTime(final Instant checkoutTime) {
        this.checkoutTime = checkoutTime;
    }
}
