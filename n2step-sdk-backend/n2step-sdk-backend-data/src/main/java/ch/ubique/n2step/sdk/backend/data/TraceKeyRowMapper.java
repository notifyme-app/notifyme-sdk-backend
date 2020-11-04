package ch.ubique.n2step.sdk.backend.data;

import ch.ubique.n2step.sdk.backend.model.TraceKey;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneOffset;
import org.springframework.jdbc.core.RowMapper;

public class TraceKeyRowMapper implements RowMapper<TraceKey> {

    @Override
    public TraceKey mapRow(ResultSet rs, int rowNum) throws SQLException {
        TraceKey traceKey = new TraceKey();
        traceKey.setId(rs.getInt("pk_trace_key"));
        traceKey.setSecretKey(rs.getBytes("secret_key"));
        traceKey.setStartTime(
                rs.getTimestamp("start_time").toInstant().atZone(ZoneOffset.UTC).toLocalDateTime());
        traceKey.setEndTime(
                rs.getTimestamp("end_time").toInstant().atZone(ZoneOffset.UTC).toLocalDateTime());
        traceKey.setCreatedAt(
                rs.getTimestamp("created_at").toInstant().atZone(ZoneOffset.UTC).toLocalDateTime());
        return traceKey;
    }
}
