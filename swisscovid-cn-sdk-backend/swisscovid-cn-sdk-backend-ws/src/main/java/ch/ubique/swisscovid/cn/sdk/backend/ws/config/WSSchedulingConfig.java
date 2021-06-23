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
import ch.ubique.swisscovid.cn.sdk.backend.data.KPIDataService;
import ch.ubique.swisscovid.cn.sdk.backend.data.SwissCovidDataService;
import ch.ubique.swisscovid.cn.sdk.backend.data.UUIDDataService;
import ch.ubique.swisscovid.cn.sdk.backend.ws.service.IOSHeartbeatSilentPush;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import javax.annotation.Nullable;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT15M")
public class WSSchedulingConfig {

    private static final Logger logger = LoggerFactory.getLogger(WSSchedulingConfig.class);

    private final SwissCovidDataService swissCovidDataService;
    private final InteractionDurationDataService interactionDurationDataService;
    private final UUIDDataService uuidDataService;
    private final IOSHeartbeatSilentPush phoneHeartbeatSilentPush;
    private final KPIDataService kpiDataService;

    @Value("${traceKey.retentionDays:14}")
    private Integer retentionDays;

    protected WSSchedulingConfig(
            final SwissCovidDataService swissCovidDataService,
            InteractionDurationDataService interactionDurationDataService,
            UUIDDataService uuidDataService,
            @Nullable IOSHeartbeatSilentPush phoneHeartbeatSilentPush,
            KPIDataService kpiDataService) {
        this.swissCovidDataService = swissCovidDataService;
        this.interactionDurationDataService = interactionDurationDataService;
        this.uuidDataService = uuidDataService;
        this.phoneHeartbeatSilentPush = phoneHeartbeatSilentPush;
        this.kpiDataService = kpiDataService;
    }

    @Scheduled(cron = "${db.cleanCron:0 0 * * * ?}", zone = "UTC")
    public void cleanTraceKeys() {
        try {
            Instant removeBefore = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
            logger.info("removing trace keys with end_time before: {}", removeBefore);
            int removeCount = swissCovidDataService.removeTraceKeys(removeBefore);
            logger.info("removed {} trace keys from db", removeCount);
        } catch (Exception e) {
            logger.error("Exception removing old trace keys", e);
        }
    }

    @Scheduled(cron = "${db.cleanCron:0 0 * * * ?}", zone = "UTC")
    public void cleanUUIDs() {
        try {
            logger.info("removing UUIDs older than {} days", retentionDays);
            uuidDataService.cleanDB(Duration.ofDays(retentionDays));
        } catch (Exception e) {
            logger.error("Exception removing old UUIDs", e);
        }
    }

    @Scheduled(cron = "${db.cleanCron:0 0 * * * ?}", zone = "UTC")
    public void cleanInteractionDurations() {
        try {
            logger.info("removing interaction duration entries older than {} days", retentionDays);
            interactionDurationDataService.removeDurations(Duration.ofDays(retentionDays));
        } catch (Exception e) {
            logger.error("Exception removing old interaction duration entries", e);
        }
    }

    @Scheduled(cron = "${db.cleanCron:0 0 * * * ?}", zone = "UTC")
    public void cleanKPI() {
        try {
            logger.info("removing checkin count entries older than {} days", retentionDays);
            kpiDataService.cleanDB(Duration.ofDays(retentionDays));
        } catch (Exception e) {
            logger.error("Exception removing old checkin count entries", e);
        }
    }

    @Scheduled(cron = "${ws.heartBeatSilentPushCron:-}", zone = "UTC")
    @SchedulerLock(name = "silent_push", lockAtLeastFor = "PT15S")
    public void silentPush() {
        if (phoneHeartbeatSilentPush != null) {
            phoneHeartbeatSilentPush.sendHeartbeats();
        }
    }
}
