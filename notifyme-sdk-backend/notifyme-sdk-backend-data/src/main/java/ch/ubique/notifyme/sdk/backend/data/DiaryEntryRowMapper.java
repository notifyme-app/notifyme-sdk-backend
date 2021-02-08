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

import ch.ubique.notifyme.sdk.backend.model.VenueTypeOuterClass.VenueType;
import ch.ubique.notifyme.sdk.backend.model.event.DiaryEntry;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.RowMapper;

public class DiaryEntryRowMapper implements RowMapper<DiaryEntry> {

    @Override
    public DiaryEntry mapRow(ResultSet rs, int rowNum) throws SQLException {
        DiaryEntry diaryEntry = new DiaryEntry();
        diaryEntry.setName(rs.getString("name"));
        diaryEntry.setLocation(rs.getString("location"));
        diaryEntry.setRoom(rs.getString("room"));
        diaryEntry.setVenueType(VenueType.valueOf(rs.getString("venue_type")));
        diaryEntry.setCheckinTime(rs.getTimestamp("checkin_time").toInstant());
        diaryEntry.setCheckoutTime(rs.getTimestamp("checkout_time").toInstant());
        return diaryEntry;
    }
}
