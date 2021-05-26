package ch.ubique.swisscovid.cn.sdk.backend.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public class UUIDDataServiceImpl implements UUIDDataService {

    private static final Logger logger = LoggerFactory.getLogger(UUIDDataServiceImpl.class);

    private final NamedParameterJdbcTemplate jt;
    private final SimpleJdbcInsert redeemUUIDInsert;

    public UUIDDataServiceImpl(DataSource dataSource) {
        this.jt = new NamedParameterJdbcTemplate(dataSource);
        this.redeemUUIDInsert =
                new SimpleJdbcInsert(dataSource)
                        .withTableName("t_redeem_uuid")
                        .usingGeneratedKeyColumns("pk_redeem_uuid_id");
    }

    @Override
    public boolean checkAndInsertPublishUUID(String uuid) {
        String sql = "select count(1) from t_redeem_uuid where uuid = :uuid";
        MapSqlParameterSource params = new MapSqlParameterSource("uuid", uuid);
        Integer count = jt.queryForObject(sql, params, Integer.class);
        if (count > 0) {
            return false;
        } else {
            // set the received_at to the next day, with no time information
            // it will stay longer in the DB but we mitigate the risk that the JWT
            // can be used twice (c.f. testTokensArentDeletedBeforeExpire).
            var startOfTomorrow = LocalDate.now().atStartOfDay().plusDays(1);
            params.addValue("received_at", Date.from(startOfTomorrow.toInstant(ZoneOffset.UTC)));
            redeemUUIDInsert.execute(params);
            return true;
        }
    }

    @Override
    @Transactional(readOnly = false)
    public void cleanDB(Duration retentionPeriod) {
        var retentionTime = LocalDate.now().minus(retentionPeriod.toDays(), ChronoUnit.DAYS).atStartOfDay();
        logger.info("Cleanup DB entries before: " + retentionTime);
        MapSqlParameterSource params =
                new MapSqlParameterSource("retention_time", Date.from(retentionTime.toInstant(ZoneOffset.UTC)));
        String sqlRedeem = "delete from t_redeem_uuid where received_at < :retention_time";
        jt.update(sqlRedeem, params);
    }
}
