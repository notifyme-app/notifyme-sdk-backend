package ch.ubique.notifyme.sdk.backend.data;

import ch.ubique.notifyme.sdk.backend.model.TraceKey;
import ch.ubique.notifyme.sdk.backend.model.util.DateUtil;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;

public class JdbcNotifyMeDataServiceImpl implements NotifyMeDataService {

    private static final Logger logger = LoggerFactory.getLogger(JdbcNotifyMeDataServiceImpl.class);

    private final String dbType;
    private final NamedParameterJdbcTemplate jt;
    private final SimpleJdbcInsert traceKeyInsert;

    public JdbcNotifyMeDataServiceImpl(String dbType, DataSource dataSource) {
        this.dbType = dbType;
        this.jt = new NamedParameterJdbcTemplate(dataSource);
        this.traceKeyInsert =
                new SimpleJdbcInsert(dataSource)
                        .withTableName("t_trace_key")
                        .usingGeneratedKeyColumns("pk_trace_key");
    }

    @Override
    public void insertTraceKey(TraceKey traceKey) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("pk_trace_key", traceKey.getId());
        params.addValue("secret_key", traceKey.getSecretKey());
        params.addValue("start_time", DateUtil.toDate(traceKey.getStartTime()));
        params.addValue("end_time", DateUtil.toDate(traceKey.getEndTime()));
        params.addValue("created_at", new Date());
        traceKeyInsert.execute(params);
    }

    @Override
    public List<TraceKey> findTraceKeys(LocalDateTime after) {
        String sql = "select * from t_trace_key";
        MapSqlParameterSource params = new MapSqlParameterSource();
        if (after != null) {
            sql += " where created_at > :after";
            params.addValue("after", DateUtil.toDate(after));
        }
        return jt.query(sql, params, new TraceKeyRowMapper());
    }

    @Override
    public int removeTraceKeys(LocalDateTime before) {
        String sql = "delete from t_trace_key where end_time < :before";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("before", DateUtil.toDate(before));
        return jt.update(sql, params);
    }
}
