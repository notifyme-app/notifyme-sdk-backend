package ch.ubique.swisscovid.cn.sdk.backend.data;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.transaction.annotation.Transactional;

public class KPIDataServiceImpl implements KPIDataService {

    private static final Logger logger = LoggerFactory.getLogger(KPIDataServiceImpl.class);

    private final NamedParameterJdbcTemplate jt;
    private final SimpleJdbcInsert pkiInsert;

    public KPIDataServiceImpl(DataSource dataSource) {
        this.jt = new NamedParameterJdbcTemplate(dataSource);
        this.pkiInsert =
                new SimpleJdbcInsert(dataSource)
                        .withTableName("t_userupload_checkin_count")
                        .usingGeneratedKeyColumns("pk_userupload_checkin_count");
    }

    @Override
    @Transactional
    public void insertCheckinCount(LocalDateTime uploadDate, int count) {
        if (count > 0) {
            var params = new MapSqlParameterSource();
            params.addValue("count", count);
            final var timestamp = uploadDate.truncatedTo(ChronoUnit.DAYS).toInstant(ZoneOffset.UTC);
            params.addValue(
                    "upload_date",
                    Date.from(timestamp));
            pkiInsert.execute(params);
        }
    }

    @Override
    @Transactional
    public int cleanDB(Duration retentionPeriod) {
        var retentionTime =
                LocalDate.now().minus(retentionPeriod.toDays(), ChronoUnit.DAYS).atStartOfDay();
        logger.info("Cleanup KPI checkin entries before: " + retentionTime);
        MapSqlParameterSource params =
                new MapSqlParameterSource(
                        "retention_time", Date.from(retentionTime.toInstant(ZoneOffset.UTC)));
        String sqlRedeem =
                "delete from t_userupload_checkin_count where upload_date < :retention_time";
        return jt.update(sqlRedeem, params);
    }
}
