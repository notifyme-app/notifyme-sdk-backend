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

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import ch.ubique.notifyme.sdk.backend.model.tracekey.v2.TraceKey;

public class TraceKeyV2RowMapper implements RowMapper<TraceKey> {

    @Override
    public TraceKey mapRow(ResultSet rs, int rowNum) throws SQLException {
        TraceKey traceKey = new TraceKey();
        traceKey.setId(rs.getInt("pk_trace_key_id"));
        traceKey.setSecretKeyForIdentity(rs.getBytes("secret_key_for_identity"));
        traceKey.setIdentity(rs.getBytes("identity"));
        traceKey.setStartTime(rs.getTimestamp("start_time").toInstant());
        traceKey.setEndTime(rs.getTimestamp("end_time").toInstant());
        traceKey.setCreatedAt(rs.getTimestamp("created_at").toInstant());
        traceKey.setMessage(rs.getBytes("message"));
        traceKey.setNonce(rs.getBytes("message_nonce"));
        return traceKey;
    }
}
