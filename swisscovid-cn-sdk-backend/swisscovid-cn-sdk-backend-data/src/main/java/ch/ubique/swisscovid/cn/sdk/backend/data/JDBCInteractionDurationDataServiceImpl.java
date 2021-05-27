package ch.ubique.swisscovid.cn.sdk.backend.data;

import java.sql.Date;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;

public class JDBCInteractionDurationDataServiceImpl implements
    InteractionDurationDataService {

  private final NamedParameterJdbcTemplate jt;
  private final SimpleJdbcInsert interactionDurationInsert;

  public JDBCInteractionDurationDataServiceImpl(DataSource dataSource) {
    this.jt = new NamedParameterJdbcTemplate(dataSource);
    this.interactionDurationInsert =
        new SimpleJdbcInsert(dataSource)
            .withTableName("t_interaction_duration")
            .usingGeneratedKeyColumns("pk_interaction_duration_id");
  }

  @Override
  public void insertInteraction(Integer interactionDuration) {
    interactionDurationInsert.execute(getDurationParams(interactionDuration));
  }

  @Override
  public List<Integer> findInteractions(Instant after) {
    var sql = "select duration from t_interaction_duration";
    final var params = new MapSqlParameterSource();
    final var before = new Date(Instant.now().toEpochMilli());
    params.addValue("before", before);
    sql += " where day < :before";
    if(after != null) {
      params.addValue("after", new Date(after.toEpochMilli()));
      sql += " and day >= :after";
    }
    return jt.queryForList(sql, params, Integer.class);
  }

  @Override
  public void removeDurations(Duration retentionPeriod) {
    if(retentionPeriod != null) {
      var retentionTime = new Date(Instant.now().minus(retentionPeriod.toDays(), ChronoUnit.DAYS).toEpochMilli());
      MapSqlParameterSource params =
          new MapSqlParameterSource("retention_time", retentionTime);
      var sqlRedeem = "delete from t_interaction_duration where day < :retention_time";
      jt.update(sqlRedeem, params);
    }
  }

  public MapSqlParameterSource getDurationParams(int duration) {
    var params = new MapSqlParameterSource();
    params.addValue("duration", duration);
    params.addValue("day", new Date(Instant.now().toEpochMilli()));
    return params;
  }
}
