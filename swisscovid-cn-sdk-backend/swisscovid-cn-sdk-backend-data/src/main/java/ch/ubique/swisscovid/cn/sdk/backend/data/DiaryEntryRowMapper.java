/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.ubique.swisscovid.cn.sdk.backend.data;

import ch.ubique.swisscovid.cn.sdk.backend.model.VenueTypeOuterClass.VenueType;
import ch.ubique.swisscovid.cn.sdk.backend.model.event.JavaDiaryEntry;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.RowMapper;

public class DiaryEntryRowMapper implements RowMapper<JavaDiaryEntry> {

    @Override
    public JavaDiaryEntry mapRow(ResultSet rs, int rowNum) throws SQLException {
        JavaDiaryEntry javaDiaryEntry = new JavaDiaryEntry();
        javaDiaryEntry.setName(rs.getString("name"));
        javaDiaryEntry.setLocation(rs.getString("location"));
        javaDiaryEntry.setRoom(rs.getString("room"));
        javaDiaryEntry.setVenueType(VenueType.valueOf(rs.getString("venue_type")));
        javaDiaryEntry.setCheckinTime(rs.getTimestamp("checkin_time").toInstant());
        javaDiaryEntry.setCheckoutTime(rs.getTimestamp("checkout_time").toInstant());
        return javaDiaryEntry;
    }
}
