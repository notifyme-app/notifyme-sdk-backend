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
    public void insertPushRegistration(final PushRegistration pushRegistration) {
        pushRegistrationInsert.execute(getPushRegistrationParams(pushRegistration));
    }

    @Override
    public List<PushRegistration> getPushRegistrationByType(final PushType pushType) {
        final String sql = "select * from t_push_registration where push_type = :pushType";
        final var params = new MapSqlParameterSource();
        return jt.query(sql, params, new PushRegistrationRowMapper());
    }

    private MapSqlParameterSource getPushRegistrationParams(PushRegistration pushRegistration) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("push_type", pushRegistration.getPushType().name());
        params.addValue("push_token", pushRegistration.getPushToken());
        params.addValue("device_id", pushRegistration.getDeviceId());
        return params;
    }
}
