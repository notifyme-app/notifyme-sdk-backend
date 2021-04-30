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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;

import ch.ubique.notifyme.sdk.backend.data.NotifyMeDataServiceV2;
import ch.ubique.notifyme.sdk.backend.data.NotifyMeDataServiceV3;
import ch.ubique.notifyme.sdk.backend.ws.service.PhoneHeartbeatSilentPush;

@Configuration
@EnableScheduling
public class WSSchedulingConfig implements SchedulingConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(WSSchedulingConfig.class);
    
    private final NotifyMeDataServiceV2 notifyMeDataServiceV2;
    private final NotifyMeDataServiceV3 notifyMeDataServiceV3;
    private final PhoneHeartbeatSilentPush phoneHeartbeatSilentPush;

    @Value("${db.cleanCron:0 0 3 * * ?}")
    private String cleanCron;

    @Value("${db.removeAfterDays:14}")
    private Integer removeAfterDays;

    @Value("${ws.heartBeatSilentPushCron}")
    private String heartBeatSilentPushCron;

    protected WSSchedulingConfig(
            final NotifyMeDataServiceV2 notifyMeDataServiceV2,
            final NotifyMeDataServiceV3 notifyMeDataServiceV3,
            final PhoneHeartbeatSilentPush phoneHeartbeatSilentPush) {
        this.notifyMeDataServiceV2 = notifyMeDataServiceV2;
        this.notifyMeDataServiceV3 = notifyMeDataServiceV3;
        this.phoneHeartbeatSilentPush = phoneHeartbeatSilentPush;
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
                                        "removing trace keys v2 with end_time before: {}",
                                        removeBefore);
                                int removeCount = notifyMeDataServiceV2.removeTraceKeys(removeBefore);
                                logger.info("removed {} trace keys from db", removeCount);
                            } catch (Exception e) {
                                logger.error("Exception removing old trace keys", e);
                            }
                        },
                        new CronTrigger(cleanCron, TimeZone.getTimeZone("UTC"))));
        
        taskRegistrar.addCronTask(
                        new CronTask(
                                () -> {
                                    try {
                                        Instant removeBefore =
                                                Instant.now().minus(removeAfterDays, ChronoUnit.DAYS);
                                        logger.info(
                                                "removing trace keys v3 with end_time before: {}",
                                                removeBefore);
                                        int removeCount =
                                                notifyMeDataServiceV3.removeTraceKeys(removeBefore);
                                        logger.info("removed {} trace keys v3 from db", removeCount);
                                    } catch (Exception e) {
                                        logger.error("Exception removing old trace keys v3", e);
                                    }
                                },
                                new CronTrigger(cleanCron, TimeZone.getTimeZone("UTC"))));

        taskRegistrar.addCronTask(
                new CronTask(
                        phoneHeartbeatSilentPush::sendHeartbeats,
                        new CronTrigger(heartBeatSilentPushCron, TimeZone.getTimeZone("UTC"))));
        

    }
}
