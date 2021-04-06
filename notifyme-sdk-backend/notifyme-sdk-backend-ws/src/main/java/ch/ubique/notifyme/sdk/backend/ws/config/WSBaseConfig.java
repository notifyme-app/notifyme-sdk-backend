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
import ch.ubique.notifyme.sdk.backend.data.NotifyMeDataService;
import ch.ubique.notifyme.sdk.backend.ws.CryptoWrapper;
import ch.ubique.notifyme.sdk.backend.ws.controller.ConfigController;
import ch.ubique.notifyme.sdk.backend.ws.controller.DebugController;
import ch.ubique.notifyme.sdk.backend.ws.controller.NotifyMeController;
import ch.ubique.notifyme.sdk.backend.ws.controller.web.WebController;
import ch.ubique.notifyme.sdk.backend.ws.controller.web.WebCriticalEventController;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hubspot.jackson.datatype.protobuf.ProtobufModule;

import static net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider.Configuration.builder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT30S")
public abstract class WSBaseConfig implements WebMvcConfigurer {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    
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
    
    @Value("${datasource.schema:}")
    String dataSourceSchema;

    @Value("${git.commit.id}")
    private String commitId;

    @Value("${git.commit.id.abbrev}")
    private String commitIdAbbrev;

    @Value("${git.commit.time}")
    private String commitTime;

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
    public NotifyMeController notifyMeController(
            NotifyMeDataService notifyMeDataService, String revision) {
        return new NotifyMeController(
                notifyMeDataService, revision, bucketSizeInMs, traceKeysCacheControlInMs);
    }

    @Bean
    public ConfigController configController() {
        return new ConfigController();
    }

    @Bean
    public WebController webController() {
        return new WebController();
    }

    @Bean
    public WebCriticalEventController webCriticalEventController(final DiaryEntryDataService diaryEntryDataService) {
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

    /**
     * Creates a LockProvider for ShedLock.
     *
     * @param dataSource JPA datasource
     * @return LockProvider
     */
    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
      String tableName = StringUtils.isEmpty(dataSourceSchema) ? "shedlock" : dataSourceSchema + ".shedlock";
      return new JdbcTemplateLockProvider(builder()
                                                  .withTableName(tableName)
                                                  .withJdbcTemplate(new JdbcTemplate(dataSource))
                                                  .usingDbTime()
                                                  .build()
      );
    }
    
    @Scheduled(cron = "${db.cleanCron:0 0 3 * * ?}")
    @SchedulerLock(name = "cleanData", lockAtLeastFor = "PT0S", lockAtMostFor = "1800000")
    public void scheduleCleanData() {
      try {
          Instant removeBefore = Instant.now().minus(removeAfterDays, ChronoUnit.DAYS);
          logger.info("removing trace keys with end_time before: {}", removeBefore);
          int removeCount = notifyMeDataService().removeTraceKeys(removeBefore);
          logger.info("removed {} trace keys from db", removeCount);
      } catch (Exception e) {
          logger.error("Exception removing old trace keys", e);
      }
    }
}
