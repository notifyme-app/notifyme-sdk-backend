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

import ch.ubique.swisscovid.cn.sdk.backend.data.DiaryEntryDataService;
import ch.ubique.swisscovid.cn.sdk.backend.data.JdbcDiaryEntryDataServiceImpl;
import ch.ubique.swisscovid.cn.sdk.backend.data.JdbcSwissCovidDataServiceV2Impl;
import ch.ubique.swisscovid.cn.sdk.backend.data.JdbcSwissCovidDataServiceV3Impl;
import ch.ubique.swisscovid.cn.sdk.backend.data.JdbcPushRegistrationDataServiceImpl;
import ch.ubique.swisscovid.cn.sdk.backend.data.SwissCovidDataServiceV2;
import ch.ubique.swisscovid.cn.sdk.backend.data.SwissCovidDataServiceV3;
import ch.ubique.swisscovid.cn.sdk.backend.data.PushRegistrationDataService;
import ch.ubique.swisscovid.cn.sdk.backend.data.UUIDDataService;
import ch.ubique.swisscovid.cn.sdk.backend.data.UUIDDataServiceImpl;
import ch.ubique.swisscovid.cn.sdk.backend.ws.controller.ConfigController;
import ch.ubique.swisscovid.cn.sdk.backend.ws.controller.SwissCovidControllerV2;
import ch.ubique.swisscovid.cn.sdk.backend.ws.controller.SwissCovidControllerV3;
import ch.ubique.swisscovid.cn.sdk.backend.ws.controller.web.WebController;
import ch.ubique.swisscovid.cn.sdk.backend.ws.controller.web.WebCriticalEventController;
import ch.ubique.swisscovid.cn.sdk.backend.ws.insertmanager.InsertManager;
import ch.ubique.swisscovid.cn.sdk.backend.ws.insertmanager.insertfilters.BeforeOnsetFilter;
import ch.ubique.swisscovid.cn.sdk.backend.ws.insertmanager.insertfilters.FakeRequestFilter;
import ch.ubique.swisscovid.cn.sdk.backend.ws.insertmanager.insertfilters.IntervalThresholdFilter;
import ch.ubique.swisscovid.cn.sdk.backend.ws.insertmanager.insertfilters.OverlappingIntervalsFilter;
import ch.ubique.swisscovid.cn.sdk.backend.ws.insertmanager.insertmodifiers.RemoveFinalIntervalModifier;
import ch.ubique.swisscovid.cn.sdk.backend.ws.security.SwissCovidJwtRequestValidator;
import ch.ubique.swisscovid.cn.sdk.backend.ws.security.RequestValidator;
import ch.ubique.swisscovid.cn.sdk.backend.ws.service.PhoneHeartbeatSilentPush;
import ch.ubique.swisscovid.cn.sdk.backend.ws.util.CryptoWrapper;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hubspot.jackson.datatype.protobuf.ProtobufModule;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
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
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableScheduling
public abstract class WSBaseConfig implements WebMvcConfigurer {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  @Value("${healthAuthority.skHex}")
  String healthAuthoritySkHex;

  @Value("${healthAuthority.pkHex}")
  String healthAuthorityPkHex;

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

  @Bean
  public static PropertySourcesPlaceholderConfigurer placeholderConfigurer() {
    PropertySourcesPlaceholderConfigurer propsConfig = new PropertySourcesPlaceholderConfigurer();
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
  public SwissCovidDataServiceV2 notifyMeDataServiceV2() {
    return new JdbcSwissCovidDataServiceV2Impl(dataSource(), bucketSizeInMs);
  }

  @Bean
  public SwissCovidDataServiceV3 notifyMeDataServiceV3() {
    return new JdbcSwissCovidDataServiceV3Impl(dataSource(), bucketSizeInMs);
  }

  @Bean
  public DiaryEntryDataService diaryEntryDataService() {
    return new JdbcDiaryEntryDataServiceImpl(dataSource());
  }

  @Bean
  public UUIDDataService uuidDataService() {
    return new UUIDDataServiceImpl(dataSource());
  }

  @Bean
  public InsertManager insertManager(
          final CryptoWrapper cryptoWrapper,
          final SwissCovidDataServiceV3 swissCovidDataServiceV3
  ) {
    final var insertManager = new InsertManager(cryptoWrapper, swissCovidDataServiceV3);
    insertManager.addModifier(new RemoveFinalIntervalModifier());
    insertManager.addFilter(new FakeRequestFilter());
    insertManager.addFilter(new IntervalThresholdFilter());
    insertManager.addFilter(new BeforeOnsetFilter());
    insertManager.addFilter(new OverlappingIntervalsFilter());
    return insertManager;
  }

  @Bean
  public SwissCovidControllerV2 notifyMeControllerV2(
      final SwissCovidDataServiceV2 notifyMeDataService,
      final PushRegistrationDataService pushRegistrationDataService,
      final String revision) {
    return new SwissCovidControllerV2(
        notifyMeDataService,
        pushRegistrationDataService,
        revision,
        bucketSizeInMs,
        traceKeysCacheControlInMs);
  }

  @Bean
  public SwissCovidControllerV3 notifyMeControllerV3(
      SwissCovidDataServiceV3 swissCovidDataServiceV3,
      InsertManager insertManager,
      PushRegistrationDataService pushRegistrationDataService,
      UUIDDataService uuidDataService,
      RequestValidator requestValidator,
      CryptoWrapper cryptoWrapper,
      String revision) {
    return new SwissCovidControllerV3(
        swissCovidDataServiceV3,
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
  public ConfigController configController() {
    return new ConfigController();
  }

  @Bean
  public PushRegistrationDataService pushRegistrationDataService(final DataSource dataSource) {
    return new JdbcPushRegistrationDataServiceImpl(dataSource);
  }

  @Bean
  public PhoneHeartbeatSilentPush phoneHeartbeatSilentPush(
      final PushRegistrationDataService pushRegistrationDataService) {
    return new PhoneHeartbeatSilentPush(pushRegistrationDataService);
  }

  @Profile("enable-debug")
  @Bean
  public WebController webController() {
    return new WebController();
  }

  @Profile("enable-debug")
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
        zonedDateTime.withZoneSameInstant(ZoneId.of("Europe/Zurich")).format(prettyFormatter);
    return "Rev: " + commitId + "\n" + prettyTime;
  }

  @Bean
  CryptoWrapper cryptoWrapper() {
    return new CryptoWrapper(
        healthAuthoritySkHex, healthAuthorityPkHex, useruploadMskHex, useruploadMpkHex);
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
}
