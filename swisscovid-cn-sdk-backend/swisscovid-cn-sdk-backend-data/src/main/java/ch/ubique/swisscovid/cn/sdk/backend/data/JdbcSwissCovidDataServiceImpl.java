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
import ch.ubique.swisscovid.cn.sdk.backend.model.util.DateUtil;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.transaction.annotation.Transactional;

public class JdbcSwissCovidDataServiceImpl implements SwissCovidDataService {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  private final Long bucketSizeInMs;
  private final NamedParameterJdbcTemplate jt;
  private final SimpleJdbcInsert traceKeyInsert;

  public JdbcSwissCovidDataServiceImpl(DataSource dataSource, Long bucketSizeInMs) {
    this.bucketSizeInMs = bucketSizeInMs;
    this.jt = new NamedParameterJdbcTemplate(dataSource);
    this.traceKeyInsert =
        new SimpleJdbcInsert(dataSource)
            .withTableName("t_trace_key")
            .usingGeneratedKeyColumns("pk_trace_key_id");
  }

  @Override
  @Transactional
  public void insertTraceKey(TraceKey traceKey) {
    traceKeyInsert.execute(getTraceKeyParams(traceKey));
  }

  @Override
  public List<TraceKey> findTraceKeys(Instant after) {
    final var before = Instant.ofEpochMilli(DateUtil.getLastFullBucketEndEpochMilli(bucketSizeInMs));
    var sql = "select * from t_trace_key";
    MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("before", Timestamp.from(before));
    sql += " where created_at < :before";
    if (after != null) {
      sql += " and created_at >= :after";
      params.addValue("after", Timestamp.from(after));
    }
    return jt.query(sql, params, new TraceKeyRowMapper());
  }

  @Override
  @Transactional
  public int removeTraceKeys(Instant before) {
    final var sql = "delete from t_trace_key where day < :before";
    final var params = new MapSqlParameterSource();
    params.addValue("before", Timestamp.from(before));
    return jt.update(sql, params);
  }

  @Override
  @Transactional
  public void insertTraceKey(List<TraceKey> traceKeysToInsert) {
    List<SqlParameterSource> batchParams = new ArrayList<>();
    if (!traceKeysToInsert.isEmpty()) {
      for (TraceKey tk : traceKeysToInsert) {
        batchParams.add(getTraceKeyParams(tk));
      }
      traceKeyInsert.executeBatch(batchParams.toArray(new SqlParameterSource[batchParams.size()]));
    }
  }

  private MapSqlParameterSource getTraceKeyParams(TraceKey traceKey) {
    var params = new MapSqlParameterSource();
    params.addValue("version", traceKey.getVersion());
    params.addValue("identity", traceKey.getIdentity());
    params.addValue("secret_key_for_identity", traceKey.getSecretKeyForIdentity());
    params.addValue("day", Date.from(traceKey.getDay()));
    if (traceKey.getCreatedAt() != null) {
      params.addValue("created_at", Timestamp.from(traceKey.getCreatedAt()));
    } else {
      params.addValue("created_at", new Timestamp(DateUtil.getCurrentBucketEndEpochMilli(bucketSizeInMs)));
    }
    params.addValue("encrypted_associated_data", traceKey.getEncryptedAssociatedData());
    params.addValue("cipher_text_nonce", traceKey.getCipherTextNonce());
    return params;
  }
}
