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

import ch.ubique.swisscovid.cn.sdk.backend.model.event.CriticalEvent;
import ch.ubique.swisscovid.cn.sdk.backend.model.event.JavaDiaryEntry;
import ch.ubique.swisscovid.cn.sdk.backend.model.util.DateUtil;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;

public class JdbcDiaryEntryDataServiceImpl implements DiaryEntryDataService {

    private final NamedParameterJdbcTemplate jt;
    private final SimpleJdbcInsert diaryEntryInsert;

    public JdbcDiaryEntryDataServiceImpl(final DataSource dataSource) {
        this.jt = new NamedParameterJdbcTemplate(dataSource);
        this.diaryEntryInsert =
                new SimpleJdbcInsert(dataSource)
                        .withTableName("t_diary_entry")
                        .usingGeneratedKeyColumns("pk_diary_entry_id");
    }

    @Override
    public void insertDiaryEntry(final JavaDiaryEntry javaDiaryEntry) {
        diaryEntryInsert.execute(getDiaryEntryParams(javaDiaryEntry));
    }

    @Override
    public void insertDiaryEntries(final List<JavaDiaryEntry> diaryEntriesToInsert) {
        final var diaryEntryParams =
                diaryEntriesToInsert.stream()
                        .map(this::getDiaryEntryParams)
                        .toArray(MapSqlParameterSource[]::new);
        diaryEntryInsert.executeBatch(diaryEntryParams);
    }

    @Override
    public List<CriticalEvent> getCriticalEvents() {
        final String sql =
                "select *"
                        + " from (select name, location, room, venue_type, count(*) as case_count"
                        + "       from t_diary_entry"
                        + "       group by name, location, room, venue_type"
                        + "      ) as grouped"
                        + " where grouped.case_count > 1"
                        + " order by grouped.case_count desc, grouped.name, grouped.location, grouped.location";
        return jt.query(sql, new MapSqlParameterSource(), new CriticalEventRowMapper());
    }

    @Override
    public List<JavaDiaryEntry> getDiaryEntriesForEvent(final CriticalEvent criticalEvent) {
        final String sql =
                "select * from t_diary_entry"
                        + " where name = :name and location = :location and room = :room and venue_type = :venueType"
                        + " order by checkin_time, checkout_time, pk_diary_entry_id";
        final var params = new MapSqlParameterSource();
        params.addValue("name", criticalEvent.getName());
        params.addValue("location", criticalEvent.getLocation());
        params.addValue("room", criticalEvent.getRoom());
        params.addValue("venueType", criticalEvent.getVenueType().name());
        return jt.query(sql, params, new DiaryEntryRowMapper());
    }

    private MapSqlParameterSource getDiaryEntryParams(JavaDiaryEntry javaDiaryEntry) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("pk_diary_entry_id", javaDiaryEntry.getId());
        params.addValue("name", javaDiaryEntry.getName());
        params.addValue("location", javaDiaryEntry.getLocation());
        params.addValue("room", javaDiaryEntry.getRoom());
        params.addValue("venue_type", javaDiaryEntry.getVenueType().name());
        params.addValue("checkin_time", DateUtil.toDate(javaDiaryEntry.getCheckinTime()));
        params.addValue("checkout_time", DateUtil.toDate(javaDiaryEntry.getCheckoutTime()));
        return params;
    }
}
