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

import ch.ubique.swisscovid.cn.sdk.backend.model.tracekey.TraceKey;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.RowMapper;

public class TraceKeyRowMapper implements RowMapper<TraceKey> {

  @Override
  public TraceKey mapRow(ResultSet rs, int rowNum) throws SQLException {
    TraceKey traceKey = new TraceKey();
    traceKey.setId(rs.getInt("pk_trace_key_id"));
    traceKey.setVersion(rs.getInt("version"));
    traceKey.setIdentity(rs.getBytes("identity"));
    traceKey.setSecretKeyForIdentity(rs.getBytes("secret_key_for_identity"));
    traceKey.setDay(rs.getTimestamp("day").toInstant());
    traceKey.setCreatedAt(rs.getTimestamp("created_at").toInstant());
    traceKey.setEncryptedAssociatedData(rs.getBytes("encrypted_associated_data"));
    traceKey.setCipherTextNonce(rs.getBytes("cipher_text_nonce"));
    return traceKey;
  }
}
