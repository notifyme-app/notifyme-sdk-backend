/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.ubique.notifyme.sdk.backend.model.event;

import ch.ubique.notifyme.sdk.backend.model.VenueTypeOuterClass.VenueType;
import ch.ubique.notifyme.sdk.backend.model.util.DateUtil;
import javax.validation.constraints.NotNull;

public class WebDiaryEntry {

    private Integer id;

    @NotNull private String name;

    @NotNull private String location;

    @NotNull private String room;

    @NotNull private VenueType venueType;

    @NotNull private String checkinTime;

    @NotNull private String checkoutTime;

    public static WebDiaryEntry from(final DiaryEntry diaryEntry) {
        final var webDiaryEntry = new WebDiaryEntry();
        webDiaryEntry.setName(diaryEntry.getName());
        webDiaryEntry.setLocation(diaryEntry.getLocation());
        webDiaryEntry.setRoom(diaryEntry.getRoom());
        webDiaryEntry.setVenueType(diaryEntry.getVenueType());
        webDiaryEntry.setCheckinTime(DateUtil.formattedDateTime(diaryEntry.getCheckinTime()));
        webDiaryEntry.setCheckoutTime(DateUtil.formattedDateTime(diaryEntry.getCheckoutTime()));
        return webDiaryEntry;
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

    public String getCheckinTime() {
        return checkinTime;
    }

    public void setCheckinTime(final String checkinTime) {
        this.checkinTime = checkinTime;
    }

    public String getCheckoutTime() {
        return checkoutTime;
    }

    public void setCheckoutTime(final String checkoutTime) {
        this.checkoutTime = checkoutTime;
    }
}
