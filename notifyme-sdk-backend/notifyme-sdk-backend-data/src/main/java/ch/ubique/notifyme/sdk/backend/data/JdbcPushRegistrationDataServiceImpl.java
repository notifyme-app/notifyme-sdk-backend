package ch.ubique.notifyme.sdk.backend.data;

import ch.ubique.notifyme.sdk.backend.model.PushRegistrationOuterClass.PushRegistration;
import ch.ubique.notifyme.sdk.backend.model.PushRegistrationOuterClass.PushType;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;

public class JdbcPushRegistrationDataServiceImpl implements PushRegistrationDataService {

    private final NamedParameterJdbcTemplate jt;
    private final SimpleJdbcInsert pushRegistrationInsert;

    public JdbcPushRegistrationDataServiceImpl(final DataSource dataSource) {
        this.jt = new NamedParameterJdbcTemplate(dataSource);
        this.pushRegistrationInsert =
                new SimpleJdbcInsert(dataSource)
                        .withTableName("t_push_registration")
                        .usingGeneratedKeyColumns("pk_push_registration_id");
    }

    @Override
    public void upsertPushRegistration(final PushRegistration pushRegistration) {
        final var pushRegistrationParams = getPushRegistrationParams(pushRegistration);
        deleteDuplicates(pushRegistrationParams);
        pushRegistrationInsert.execute(pushRegistrationParams);
    }

    private void deleteDuplicates(final MapSqlParameterSource pushRegistrationParams) {
        final var sql =
                "delete from t_push_registration where device_id = :device_id or push_token = :push_token";
        jt.update(sql, pushRegistrationParams);
    }

    @Override
    public List<PushRegistration> getPushRegistrationByType(final PushType pushType) {
        final String sql = "select * from t_push_registration where push_type = :pushType";
        final var params = new MapSqlParameterSource();
        return jt.query(sql, params, new PushRegistrationRowMapper());
    }

    @Override
    public void deletePushRegistration(final PushRegistration pushRegistration) {
        final var pushRegistrationParams = getPushRegistrationParams(pushRegistration);
        final var sql =
                "delete from t_push_registration where device_id = :device_id";
        jt.update(sql, pushRegistrationParams);
    }

    private MapSqlParameterSource getPushRegistrationParams(PushRegistration pushRegistration) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("push_type", pushRegistration.getPushType().name());
        params.addValue("push_token", pushRegistration.getPushToken());
        params.addValue("device_id", pushRegistration.getDeviceId());
        return params;
    }
}
