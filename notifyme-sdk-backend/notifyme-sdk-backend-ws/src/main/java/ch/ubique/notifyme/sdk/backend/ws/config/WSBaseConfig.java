/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.ubique.notifyme.sdk.backend.ws.config;

import ch.ubique.notifyme.sdk.backend.data.DiaryEntryDataService;
import ch.ubique.notifyme.sdk.backend.data.JdbcDiaryEntryDataServiceImpl;
import ch.ubique.notifyme.sdk.backend.data.JdbcNotifyMeDataServiceImpl;
import ch.ubique.notifyme.sdk.backend.data.JdbcPushRegistrationDataServiceImpl;
import ch.ubique.notifyme.sdk.backend.data.NotifyMeDataService;
import ch.ubique.notifyme.sdk.backend.data.PushRegistrationDataService;
import ch.ubique.notifyme.sdk.backend.model.PushRegistrationOuterClass.PushType;
import ch.ubique.notifyme.sdk.backend.ws.CryptoWrapper;
import ch.ubique.notifyme.sdk.backend.ws.controller.ConfigController;
import ch.ubique.notifyme.sdk.backend.ws.controller.DebugController;
import ch.ubique.notifyme.sdk.backend.ws.controller.NotifyMeController;
import ch.ubique.notifyme.sdk.backend.ws.controller.web.WebController;
import ch.ubique.notifyme.sdk.backend.ws.controller.web.WebCriticalEventController;
import ch.ubique.notifyme.sdk.backend.ws.service.PhoneBackgroundTaskTrigger;
import ch.ubique.pushservice.pushconnector.PushConnectorServiceBuilder;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hubspot.jackson.datatype.protobuf.ProtobufModule;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import javax.sql.DataSource;
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
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableScheduling
public abstract class WSBaseConfig implements SchedulingConfigurer, WebMvcConfigurer {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${db.cleanCron:0 0 3 * * ?}")
    String cleanCron;

    @Value("${db.removeAfterDays:14}")
    Integer removeAfterDays;

    @Value("${healthAuthority.skHex}")
    String healthAuthoritySkHex;

    @Value("${healthAuthority.pkHex}")
    String healthAuthorityPkHex;

    @Value("${traceKey.bucketSizeInMs}")
    Long bucketSizeInMs;

    @Value("${traceKey.traceKeysCacheControlInMs}")
    Long traceKeysCacheControlInMs;

    @Value("${git.commit.id}")
    private String commitId;

    @Value("${git.commit.id.abbrev}")
    private String commitIdAbbrev;

    @Value("${git.commit.time}")
    private String commitTime;

    @Value("${ws.push.authToken}")
    private String pushAuthToken;

    @Value("${ws.push.serverHost}")
    private String pushServerHost;

    @Value("${ws.push.applicationId.ios}")
    private String pushApplicationIdIOS;

    @Value("${ws.push.applicationId.iod}")
    private String pushApplicationIdIOD;

    @Value("${ws.push.applicationId.and}")
    private String pushApplicationIdAND;

    @Bean
    public static PropertySourcesPlaceholderConfigurer placeholderConfigurer() {
        PropertySourcesPlaceholderConfigurer propsConfig =
                new PropertySourcesPlaceholderConfigurer();
        propsConfig.setLocation(new ClassPathResource("git.properties"));
        propsConfig.setIgnoreResourceNotFound(true);
        propsConfig.setIgnoreUnresolvablePlaceholders(true);
        return propsConfig;
    }

    public abstract DataSource dataSource();

    public abstract Flyway flyway();

    public abstract String getDbType();

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
    public NotifyMeDataService notifyMeDataService() {
        return new JdbcNotifyMeDataServiceImpl(dataSource(), bucketSizeInMs);
    }

    @Bean
    public DiaryEntryDataService diaryEntryDataService(final DataSource dataSource) {
        return new JdbcDiaryEntryDataServiceImpl(dataSource);
    }

    @Bean
    public PushRegistrationDataService pushRegistrationDataService(final DataSource dataSource) {
        return new JdbcPushRegistrationDataServiceImpl(dataSource);
    }

    @Bean
    public NotifyMeController notifyMeController(
            final NotifyMeDataService notifyMeDataService,
            final PushRegistrationDataService pushRegistrationDataService,
            final String revision) {
        return new NotifyMeController(
                notifyMeDataService,
                pushRegistrationDataService,
                revision,
                bucketSizeInMs,
                traceKeysCacheControlInMs);
    }

    @Bean
    public ConfigController configController() {
        return new ConfigController();
    }

    @Bean
    public PhoneBackgroundTaskTrigger phoneBackgroundTaskTrigger() {
        final var pushConnectorServiceBuilder =
                new PushConnectorServiceBuilder(pushAuthToken, pushServerHost);

        final var iod =
                Map.entry(
                        PushType.IOD,
                        pushConnectorServiceBuilder.withApple(pushApplicationIdIOD).build());
        final var ios =
                Map.entry(
                        PushType.IOS,
                        pushConnectorServiceBuilder.withApple(pushApplicationIdIOS).build());
        final var and =
                Map.entry(
                        PushType.AND,
                        pushConnectorServiceBuilder.withApple(pushApplicationIdAND).build());

        return new PhoneBackgroundTaskTrigger(Map.ofEntries(iod, ios, and));
    }

    @Bean
    public WebController webController() {
        return new WebController();
    }

    @Bean
    public WebCriticalEventController webCriticalEventController(
            final DiaryEntryDataService diaryEntryDataService) {
        return new WebCriticalEventController(diaryEntryDataService);
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

    @Profile("enable-debug")
    @Bean
    public DebugController debugController(
            final NotifyMeDataService notifyMeDataService,
            final DiaryEntryDataService diaryEntryDataService,
            final CryptoWrapper cryptoWrapper) {
        return new DebugController(notifyMeDataService, diaryEntryDataService, cryptoWrapper);
    }

    @Bean
    CryptoWrapper cryptoWrapper() {
        return new CryptoWrapper(healthAuthoritySkHex, healthAuthorityPkHex);
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
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        // remove old trace keys
        taskRegistrar.addCronTask(
                new CronTask(
                        () -> {
                            try {
                                Instant removeBefore =
                                        Instant.now().minus(removeAfterDays, ChronoUnit.DAYS);
                                logger.info(
                                        "removing trace keys with end_time before: {}",
                                        removeBefore);
                                int removeCount =
                                        notifyMeDataService().removeTraceKeys(removeBefore);
                                logger.info("removed {} trace keys from db", removeCount);
                            } catch (Exception e) {
                                logger.error("Exception removing old trace keys", e);
                            }
                        },
                        new CronTrigger(cleanCron, TimeZone.getTimeZone("UTC"))));
    }
}
