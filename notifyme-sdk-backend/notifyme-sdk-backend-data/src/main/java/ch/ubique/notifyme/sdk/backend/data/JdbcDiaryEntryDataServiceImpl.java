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

import ch.ubique.notifyme.sdk.backend.model.event.CriticalEvent;
import ch.ubique.notifyme.sdk.backend.model.event.DiaryEntry;
import ch.ubique.notifyme.sdk.backend.model.util.DateUtil;
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
    public void insertDiaryEntry(final DiaryEntry diaryEntry) {
        diaryEntryInsert.execute(getDiaryEntryParams(diaryEntry));
    }

    @Override
    public void insertDiaryEntries(final List<DiaryEntry> diaryEntriesToInsert) {
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
                        + " where grouped.case_count > 1";
        return jt.query(sql, new MapSqlParameterSource(), new CriticalEventRowMapper());
    }

    private MapSqlParameterSource getDiaryEntryParams(DiaryEntry diaryEntry) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("pk_diary_entry_id", diaryEntry.getId());
        params.addValue("name", diaryEntry.getName());
        params.addValue("location", diaryEntry.getLocation());
        params.addValue("room", diaryEntry.getRoom());
        params.addValue("venue_type", diaryEntry.getVenueType().name());
        params.addValue("checkin_time", DateUtil.toDate(diaryEntry.getCheckinTime()));
        params.addValue("checkout_time", DateUtil.toDate(diaryEntry.getCheckoutTime()));
        return params;
    }
}
