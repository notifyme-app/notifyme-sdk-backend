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

import ch.ubique.notifyme.sdk.backend.data.JdbcNotifyMeDataServiceImpl;
import ch.ubique.notifyme.sdk.backend.data.NotifyMeDataService;
import ch.ubique.notifyme.sdk.backend.ws.SodiumWrapper;
import ch.ubique.notifyme.sdk.backend.ws.controller.DebugController;
import ch.ubique.notifyme.sdk.backend.ws.controller.NotifyMeController;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.hubspot.jackson.datatype.protobuf.ProtobufModule;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.TimeZone;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.converter.HttpMessageConverter;
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

    @Value("${git.commit.id}")
    private String commitId;

    @Value("${git.commit.id.abbrev}")
    private String commitIdAbbrev;

    @Value("${git.commit.time}")
    private String commitTime;

    public abstract DataSource dataSource();

    public abstract Flyway flyway();

    public abstract String getDbType();

    @Bean
    public static PropertySourcesPlaceholderConfigurer placeholderConfigurer() {
        PropertySourcesPlaceholderConfigurer propsConfig =
                new PropertySourcesPlaceholderConfigurer();
        propsConfig.setLocation(new ClassPathResource("git.properties"));
        propsConfig.setIgnoreResourceNotFound(true);
        propsConfig.setIgnoreUnresolvablePlaceholders(true);
        return propsConfig;
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
    public NotifyMeDataService notifyMeDataService() {
        return new JdbcNotifyMeDataServiceImpl(getDbType(), dataSource());
    }

    @Bean
    public NotifyMeController notifyMeController(NotifyMeDataService notifyMeDataService, String revision) {
        return new NotifyMeController(notifyMeDataService, revision);
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
    public DebugController debugController(
            NotifyMeDataService notifyMeDataService, SodiumWrapper sodiumWrapper) {
        return new DebugController(notifyMeDataService, sodiumWrapper);
    }

    @Bean
    SodiumWrapper sodiumWrapper() {
        return new SodiumWrapper(healthAuthoritySkHex, healthAuthorityPkHex);
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        // remove old trace keys
        taskRegistrar.addCronTask(
                new CronTask(
                        new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    LocalDateTime removeBefore =
                                            LocalDateTime.now(ZoneOffset.UTC)
                                                    .minusDays(removeAfterDays);
                                    logger.info(
                                            "removing trace keys with end_time before: "
                                                    + removeBefore);
                                    int removeCount =
                                            notifyMeDataService().removeTraceKeys(removeBefore);
                                    logger.info("removed " + removeCount + " trace keys from db");
                                } catch (Exception e) {
                                    logger.error("Exception removing old trace keys", e);
                                }
                            }
                        },
                        new CronTrigger(cleanCron, TimeZone.getTimeZone("UTC"))));
    }
}
