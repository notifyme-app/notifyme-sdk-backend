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
import ch.ubique.notifyme.sdk.backend.model.util.DateUtil;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.transaction.annotation.Transactional;

public class JdbcNotifyMeDataServiceImpl implements NotifyMeDataService {

    private static final Logger logger = LoggerFactory.getLogger(JdbcNotifyMeDataServiceImpl.class);

    private final Long bucketSizeInMs;
    private final String dbType;
    private final NamedParameterJdbcTemplate jt;
    private final SimpleJdbcInsert traceKeyInsert;

    public JdbcNotifyMeDataServiceImpl(String dbType, DataSource dataSource, Long bucketSizeInMs) {
        this.bucketSizeInMs = bucketSizeInMs;
        this.dbType = dbType;
        this.jt = new NamedParameterJdbcTemplate(dataSource);
        this.traceKeyInsert =
                new SimpleJdbcInsert(dataSource)
                        .withTableName("t_trace_key")
                        .usingGeneratedKeyColumns("pk_trace_key_id");
    }

    @Override
    @Transactional(readOnly = false)
    public void insertTraceKey(TraceKey traceKey) {
        traceKeyInsert.execute(getTraceKeyParams(traceKey));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TraceKey> findTraceKeys(Instant after) {
        String sql = "select * from t_trace_key";
        MapSqlParameterSource params =
                new MapSqlParameterSource(
                        "before",
                        new Date(DateUtil.getLastFullBucketEndEpochMilli(bucketSizeInMs)));
        sql += " where created_at < :before";
        if (after != null) {
            sql += " and created_at >= :after";
            params.addValue("after", DateUtil.toDate(after));
        }
        return jt.query(sql, params, new TraceKeyRowMapper());
    }

    @Override
    @Transactional(readOnly = false)
    public int removeTraceKeys(Instant before) {
        String sql = "delete from t_trace_key where end_time < :before";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("before", DateUtil.toDate(before));
        return jt.update(sql, params);
    }

    @Override
    @Transactional(readOnly = false)
    public void insertTraceKey(List<TraceKey> traceKeysToInsert) {
        List<SqlParameterSource> batchParams = new ArrayList<>();
        if (!traceKeysToInsert.isEmpty()) {
            for (TraceKey tk : traceKeysToInsert) {
                batchParams.add(getTraceKeyParams(tk));
            }
            traceKeyInsert.executeBatch(batchParams.toArray(new SqlParameterSource[0]));
        }
    }

    private MapSqlParameterSource getTraceKeyParams(TraceKey traceKey) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("pk_trace_key_id", traceKey.getId());
        params.addValue("secret_key_for_identity", traceKey.getSecretKeyForIdentity());
        params.addValue("identity", traceKey.getIdentity());
        params.addValue("start_time", DateUtil.toDate(traceKey.getStartTime()));
        params.addValue("end_time", DateUtil.toDate(traceKey.getEndTime()));
        params.addValue("created_at", new Date());
        params.addValue("message", traceKey.getMessage());
        params.addValue("message_nonce", traceKey.getNonce());
        return params;
    }
}
