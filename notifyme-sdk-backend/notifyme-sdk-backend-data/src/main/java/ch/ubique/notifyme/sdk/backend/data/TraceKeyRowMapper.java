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

import ch.ubique.notifyme.sdk.backend.model.tracekey.TraceKey;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneOffset;
import org.springframework.jdbc.core.RowMapper;

public class TraceKeyRowMapper implements RowMapper<TraceKey> {

    @Override
    public TraceKey mapRow(ResultSet rs, int rowNum) throws SQLException {
        TraceKey traceKey = new TraceKey();
        traceKey.setId(rs.getInt("pk_trace_key"));
        traceKey.setSecretKey(rs.getBytes("secret_key"));
        traceKey.setStartTime(
                rs.getTimestamp("start_time").toInstant().atZone(ZoneOffset.UTC).toLocalDateTime());
        traceKey.setEndTime(
                rs.getTimestamp("end_time").toInstant().atZone(ZoneOffset.UTC).toLocalDateTime());
        traceKey.setCreatedAt(
                rs.getTimestamp("created_at").toInstant().atZone(ZoneOffset.UTC).toLocalDateTime());
        traceKey.setMessage(rs.getBytes("message"));
        return traceKey;
    }
}
