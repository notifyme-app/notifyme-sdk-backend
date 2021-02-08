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
import ch.ubique.notifyme.sdk.backend.model.event.CriticalEvent;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.RowMapper;

public class CriticalEventRowMapper implements RowMapper<CriticalEvent> {

    @Override
    public CriticalEvent mapRow(ResultSet rs, int rowNum) throws SQLException {
        CriticalEvent criticalEvent = new CriticalEvent();
        criticalEvent.setName(rs.getString("name"));
        criticalEvent.setLocation(rs.getString("location"));
        criticalEvent.setRoom(rs.getString("room"));
        criticalEvent.setVenueType(VenueType.valueOf(rs.getString("venue_type")));
        criticalEvent.setCaseCount(rs.getInt("case_count"));
        return criticalEvent;
    }
}
