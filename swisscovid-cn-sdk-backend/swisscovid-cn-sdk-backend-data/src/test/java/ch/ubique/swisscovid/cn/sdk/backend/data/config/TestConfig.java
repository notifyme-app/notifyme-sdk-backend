package ch.ubique.swisscovid.cn.sdk.backend.data.config;

import ch.ubique.swisscovid.cn.sdk.backend.data.JdbcSwissCovidDataServiceV3Impl;
import ch.ubique.swisscovid.cn.sdk.backend.data.SwissCovidDataServiceV3;
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
    public SwissCovidDataServiceV3 notifyMeDataServiceV3() {
        return new JdbcSwissCovidDataServiceV3Impl(dataSource, 60000L);
    }

    @Bean
    public UUIDDataService uuidDataService() {
        return new UUIDDataServiceImpl(dataSource);
    }
}
