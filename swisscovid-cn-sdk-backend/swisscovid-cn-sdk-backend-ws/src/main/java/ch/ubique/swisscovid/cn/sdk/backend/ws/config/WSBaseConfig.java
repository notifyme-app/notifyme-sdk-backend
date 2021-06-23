/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.ubique.swisscovid.cn.sdk.backend.ws.config;

import ch.ubique.swisscovid.cn.sdk.backend.data.InteractionDurationDataService;
import ch.ubique.swisscovid.cn.sdk.backend.data.JDBCInteractionDurationDataServiceImpl;
import ch.ubique.swisscovid.cn.sdk.backend.data.JdbcPushRegistrationDataServiceImpl;
import ch.ubique.swisscovid.cn.sdk.backend.data.JdbcSwissCovidDataServiceImpl;
import ch.ubique.swisscovid.cn.sdk.backend.data.KPIDataService;
import ch.ubique.swisscovid.cn.sdk.backend.data.KPIDataServiceImpl;
import ch.ubique.swisscovid.cn.sdk.backend.data.PushRegistrationDataService;
import ch.ubique.swisscovid.cn.sdk.backend.data.SwissCovidDataService;
import ch.ubique.swisscovid.cn.sdk.backend.data.UUIDDataService;
import ch.ubique.swisscovid.cn.sdk.backend.data.UUIDDataServiceImpl;
import ch.ubique.swisscovid.cn.sdk.backend.ws.controller.SwissCovidControllerV3;
import ch.ubique.swisscovid.cn.sdk.backend.ws.filter.ResponseWrapperFilter;
import ch.ubique.swisscovid.cn.sdk.backend.ws.insertmanager.InsertManager;
import ch.ubique.swisscovid.cn.sdk.backend.ws.insertmanager.insertfilters.BeforeOnsetFilter;
import ch.ubique.swisscovid.cn.sdk.backend.ws.insertmanager.insertfilters.FakeRequestFilter;
import ch.ubique.swisscovid.cn.sdk.backend.ws.insertmanager.insertfilters.IntervalThresholdFilter;
import ch.ubique.swisscovid.cn.sdk.backend.ws.insertmanager.insertfilters.OverlappingIntervalsFilter;
import ch.ubique.swisscovid.cn.sdk.backend.ws.insertmanager.insertmodifiers.RemoveFinalIntervalModifier;
import ch.ubique.swisscovid.cn.sdk.backend.ws.interceptor.HeaderInjector;
import ch.ubique.swisscovid.cn.sdk.backend.ws.security.RequestValidator;
import ch.ubique.swisscovid.cn.sdk.backend.ws.security.SwissCovidJwtRequestValidator;
import ch.ubique.swisscovid.cn.sdk.backend.ws.service.IOSHeartbeatSilentPush;
import ch.ubique.swisscovid.cn.sdk.backend.ws.util.CryptoWrapper;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hubspot.jackson.datatype.protobuf.ProtobufModule;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import javax.net.ssl.SSLException;
import javax.sql.DataSource;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.protobuf.ProtobufHttpMessageConverter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableScheduling
public abstract class WSBaseConfig implements WebMvcConfigurer {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    final SignatureAlgorithm algorithm = SignatureAlgorithm.ES256;

    @Value("${userupload.mpkHex}")
    String useruploadMpkHex;

    @Value("${userupload.mskHex}")
    String useruploadMskHex;

    @Value("${traceKey.bucketSizeInMs}")
    Long bucketSizeInMs;

    @Value("${userupload.requestTime}")
    Long requestTime;

    @Value("${traceKey.traceKeysCacheControlInMs}")
    Long traceKeysCacheControlInMs;

    @Value("${ws.headers.protected:}")
    List<String> protectedHeaders;

    @Value("${ws.headers.debug: false}")
    boolean setDebugHeaders;

    @Value(
            "#{${ws.security.headers: {'X-Content-Type-Options':'nosniff', 'X-Frame-Options':'DENY','X-Xss-Protection':'1; mode=block'}}}")
    Map<String, String> additionalHeaders;

    @Value("${git.commit.id}")
    private String commitId;

    @Value("${git.commit.id.abbrev}")
    private String commitIdAbbrev;

    @Value("${git.commit.time}")
    private String commitTime;
    // base64 encoded p8 file
    @Value("${push.ios.signingkey}")
    private String iosPushSigningKey;

    @Value("${push.ios.teamid}")
    private String iosPushTeamId;

    @Value("${push.ios.keyid}")
    private String iosPushKeyId;

    @Value("${push.ios.topic}")
    private String iosPushTopic;

    @Value("${traceKey.retentionDays:14}")
    private Integer retentionDays;

    @Bean
    public static PropertySourcesPlaceholderConfigurer placeholderConfigurer() {
        PropertySourcesPlaceholderConfigurer propsConfig =
                new PropertySourcesPlaceholderConfigurer();
        propsConfig.setLocation(new ClassPathResource("git.properties"));
        propsConfig.setIgnoreResourceNotFound(true);
        propsConfig.setIgnoreUnresolvablePlaceholders(true);
        return propsConfig;
    }

    public KeyPair getKeyPair(SignatureAlgorithm algorithm) {
        logger.warn(
                "USING FALLBACK KEYPAIR. WONT'T PERSIST APP RESTART AND PROBABLY DOES NOT HAVE ENOUGH"
                        + " ENTROPY.");

        return Keys.keyPairFor(algorithm);
    }

    public abstract DataSource dataSource();

    public abstract Flyway flyway();

    public abstract String getDbType();

    @Bean
    public ResponseWrapperFilter hashFilter()
            throws CertificateException, IOException, NoSuchAlgorithmException,
                    InvalidKeySpecException, NoSuchProviderException {
        return new ResponseWrapperFilter(
                getSignatureKeyPair(), retentionDays, protectedHeaders, setDebugHeaders);
    }

    /**
     * Get keypair for the response signature.
     *
     * @return
     */
    protected abstract KeyPair getSignatureKeyPair()
            throws CertificateException, IOException, NoSuchAlgorithmException,
                    InvalidKeySpecException, NoSuchProviderException;

    @Bean
    public HeaderInjector securityHeaderInjector() {
        return new HeaderInjector(additionalHeaders);
    }

    @Bean
    public MappingJackson2HttpMessageConverter converter() {
        ObjectMapper mapper =
                new ObjectMapper()
                        .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                        .setSerializationInclusion(JsonInclude.Include.NON_ABSENT)
                        .registerModules(new ProtobufModule(), new Jdk8Module());
        return new MappingJackson2HttpMessageConverter(mapper);
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(new ProtobufHttpMessageConverter());
        WebMvcConfigurer.super.extendMessageConverters(converters);
    }

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setTaskExecutor(mvcTaskExecutor());
        configurer.setDefaultTimeout(5_000);
    }

    @Bean
    public ThreadPoolTaskExecutor mvcTaskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setThreadNamePrefix("mvc-task-");
        taskExecutor.setMaxPoolSize(1000);
        return taskExecutor;
    }

    @Bean
    public SwissCovidDataService swissCovidDataService() {
        return new JdbcSwissCovidDataServiceImpl(dataSource(), bucketSizeInMs);
    }

    @Bean
    public UUIDDataService uuidDataService() {
        return new UUIDDataServiceImpl(dataSource());
    }

    @Bean
    public InteractionDurationDataService interactionDurationDataService(DataSource dataSource) {
        return new JDBCInteractionDurationDataServiceImpl(dataSource);
    }

    @Bean
    public InsertManager insertManager(
            final CryptoWrapper cryptoWrapper,
            final SwissCovidDataService swissCovidDataService,
            final InteractionDurationDataService interactionDurationDataService,
            final KPIDataService kpiDataService) {
        final var insertManager =
                new InsertManager(
                        cryptoWrapper,
                        swissCovidDataService,
                        interactionDurationDataService,
                        kpiDataService);
        insertManager.addModifier(new RemoveFinalIntervalModifier());
        insertManager.addFilter(new FakeRequestFilter());
        insertManager.addFilter(new IntervalThresholdFilter());
        insertManager.addFilter(new BeforeOnsetFilter());
        insertManager.addFilter(new OverlappingIntervalsFilter());
        return insertManager;
    }

    @Bean
    public SwissCovidControllerV3 notifyMeControllerV3(
            SwissCovidDataService swissCovidDataService,
            InsertManager insertManager,
            PushRegistrationDataService pushRegistrationDataService,
            UUIDDataService uuidDataService,
            RequestValidator requestValidator,
            CryptoWrapper cryptoWrapper,
            String revision) {
        return new SwissCovidControllerV3(
                swissCovidDataService,
                insertManager,
                pushRegistrationDataService,
                uuidDataService,
                requestValidator,
                cryptoWrapper,
                revision,
                bucketSizeInMs,
                traceKeysCacheControlInMs,
                Duration.ofMillis(requestTime));
    }

    @Bean
    public PushRegistrationDataService pushRegistrationDataService(final DataSource dataSource) {
        return new JdbcPushRegistrationDataServiceImpl(dataSource);
    }

    @Bean
    public KPIDataService pkiDataService(final DataSource dataSource) {
        return new KPIDataServiceImpl(dataSource);
    }

    @Bean
    @Profile("push")
    public IOSHeartbeatSilentPush phoneHeartbeatSilentPush(
            final PushRegistrationDataService pushRegistrationDataService)
            throws InvalidKeyException, SSLException, NoSuchAlgorithmException, IOException,
                    URISyntaxException {
        byte[] pushSigningKey = Base64.getDecoder().decode(iosPushSigningKey);
        return new IOSHeartbeatSilentPush(
                pushRegistrationDataService,
                new ByteArrayInputStream(pushSigningKey),
                iosPushTeamId,
                iosPushKeyId,
                iosPushTopic);
    }

    @Bean
    public String revision() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        ZonedDateTime zonedDateTime =
                LocalDateTime.parse(commitTime, formatter).atZone(ZoneId.of("UTC"));
        DateTimeFormatter prettyFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
        String prettyTime =
                zonedDateTime
                        .withZoneSameInstant(ZoneId.of("Europe/Zurich"))
                        .format(prettyFormatter);
        return "Rev: " + commitId + "\n" + prettyTime;
    }

    @Bean
    CryptoWrapper cryptoWrapper() {
        return new CryptoWrapper(useruploadMskHex, useruploadMpkHex);
    }

    @Bean
    public RequestValidator requestValidator() {
        return new SwissCovidJwtRequestValidator();
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(customJacksonJsonConverter());
        converters.add(new StringHttpMessageConverter());
    }

    @Bean
    public MappingJackson2HttpMessageConverter customJacksonJsonConverter() {
        ObjectMapper mapper =
                new ObjectMapper()
                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                        .setSerializationInclusion(JsonInclude.Include.NON_ABSENT)
                        .registerModule(new JavaTimeModule())
                        .disable(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
                        .enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                        .disable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS);
        return new MappingJackson2HttpMessageConverter(mapper);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(securityHeaderInjector());
    }

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(
                JdbcTemplateLockProvider.Configuration.builder()
                        .withTableName("t_shedlock")
                        .withJdbcTemplate(new JdbcTemplate(dataSource))
                        .usingDbTime()
                        .build());
    }
}
