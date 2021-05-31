package ch.ubique.swisscovid.cn.sdk.backend.data.config;

import ch.ubique.swisscovid.cn.sdk.backend.data.InteractionDurationDataService;
import ch.ubique.swisscovid.cn.sdk.backend.data.JDBCInteractionDurationDataServiceImpl;
import ch.ubique.swisscovid.cn.sdk.backend.data.JdbcSwissCovidDataServiceImpl;
import ch.ubique.swisscovid.cn.sdk.backend.data.PushRegistrationDataService;
import ch.ubique.swisscovid.cn.sdk.backend.data.SwissCovidDataService;
import ch.ubique.swisscovid.cn.sdk.backend.data.JdbcPushRegistrationDataServiceImpl;
import ch.ubique.swisscovid.cn.sdk.backend.data.UUIDDataService;
import ch.ubique.swisscovid.cn.sdk.backend.data.UUIDDataServiceImpl;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

@Profile("test-config")
@Configuration
public class TestConfig {
    @Autowired DataSource dataSource;

    @Bean
    public PlatformTransactionManager testTransactionManager() {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    public SwissCovidDataService swissCovidDataService() {
        return new JdbcSwissCovidDataServiceImpl(dataSource, 60000L);
    }

    @Bean
    public UUIDDataService uuidDataService() {
        return new UUIDDataServiceImpl(dataSource);
    }

    @Bean
    public InteractionDurationDataService interactionDurationDataService() {
        return new JDBCInteractionDurationDataServiceImpl(dataSource);
    }

    @Bean
    public PushRegistrationDataService pushRegistrationDataService() {
        return new JdbcPushRegistrationDataServiceImpl(dataSource);
    }
}
