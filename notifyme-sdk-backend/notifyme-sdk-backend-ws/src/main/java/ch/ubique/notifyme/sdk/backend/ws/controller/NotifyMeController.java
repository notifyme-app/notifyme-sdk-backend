/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.ubique.notifyme.sdk.backend.ws.controller;

import ch.ubique.notifyme.sdk.backend.data.NotifyMeDataService;
import ch.ubique.notifyme.sdk.backend.data.PushRegistrationDataService;
import ch.ubique.notifyme.sdk.backend.model.ProblematicEventWrapperOuterClass.ProblematicEvent;
import ch.ubique.notifyme.sdk.backend.model.ProblematicEventWrapperOuterClass.ProblematicEvent.Builder;
import ch.ubique.notifyme.sdk.backend.model.ProblematicEventWrapperOuterClass.ProblematicEventWrapper;
import ch.ubique.notifyme.sdk.backend.model.PushRegistrationOuterClass.PushRegistration;
import ch.ubique.notifyme.sdk.backend.model.tracekey.TraceKey;
import ch.ubique.notifyme.sdk.backend.model.util.DateUtil;
import ch.ubique.openapi.docannotations.Documentation;
import com.google.protobuf.ByteString;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/v1")
@CrossOrigin(origins = {"https://notify-me.c4dt.org", "https://notify-me-dev.c4dt.org"})
public class NotifyMeController {
    private static final String HEADER_X_KEY_BUNDLE_TAG = "x-key-bundle-tag";

    private final NotifyMeDataService notifyMeDataService;
    private final PushRegistrationDataService pushRegistrationDataService;
    private final String revision;
    private final Long bucketSizeInMs;
    private final Long traceKeysCacheControlInMs;

    public NotifyMeController(
            final NotifyMeDataService notifyMeDataService,
            final PushRegistrationDataService pushRegistrationDataService,
            final String revision,
            final Long bucketSizeInMs,
            final Long traceKeysCacheControlInMs) {
        this.notifyMeDataService = notifyMeDataService;
        this.pushRegistrationDataService = pushRegistrationDataService;
        this.revision = revision;
        this.bucketSizeInMs = bucketSizeInMs;
        this.traceKeysCacheControlInMs = traceKeysCacheControlInMs;
    }

    @GetMapping(value = "")
    @Documentation(
            description = "Hello return",
            responses = {"200=>server live"})
    public @ResponseBody ResponseEntity<String> hello() {
        return ResponseEntity.ok()
                .header("X-HELLO", "notifyme")
                .body("Hello from NotifyMe WS v1.\n" + revision);
    }

    @GetMapping(
            value = "/traceKeys",
            produces = {"application/json"})
    public @ResponseBody ResponseEntity<List<TraceKey>> getTraceKeysJson(
            @RequestParam(required = false) Long lastKeyBundleTag) {
        if (!isValidKeyBundleTag(lastKeyBundleTag)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(traceKeysCacheControlInMs, TimeUnit.MILLISECONDS))
                .header(
                        HEADER_X_KEY_BUNDLE_TAG,
                        Long.toString(DateUtil.getLastFullBucketEndEpochMilli(bucketSizeInMs)))
                .body(notifyMeDataService.findTraceKeys(DateUtil.toInstant(lastKeyBundleTag)));
    }

    private boolean isValidKeyBundleTag(Long lastKeyBundleTag) {
        return lastKeyBundleTag == null
                || ((DateUtil.isBucketAligned(lastKeyBundleTag, bucketSizeInMs))
                        && (DateUtil.isInThePast(lastKeyBundleTag)));
    }

    @GetMapping(
            value = "/traceKeys",
            produces = {"application/x-protobuf", "application/protobuf"})
    @Documentation(
            description =
                    "Requests trace keys uploaded after _lastKeyBundleTag_. If _lastKeyBundleTag_ is ommited, all uploaded trace keys are returned",
            responses = {
                "200 => protobuf/json of all keys in that interval. response header _x-key-bundle-tag_ contains _lastKeyBundleTag_ for next request",
                "404 => Invalid _lastKeyBundleTag_"
            },
            responseHeaders = {
                HEADER_X_KEY_BUNDLE_TAG + ":_lastKeyBundleTag_ to send with next request:string"
            })
    public @ResponseBody ResponseEntity<ProblematicEventWrapper> getTraceKeys(
            @RequestParam(required = false)
                    @Documentation(
                            description =
                                    "in millis since epoch. must be aligned to a full hour, and < now()")
                    Long lastKeyBundleTag) {
        if (!isValidKeyBundleTag(lastKeyBundleTag)) {
            return ResponseEntity.notFound()
                    .cacheControl(
                            CacheControl.maxAge(traceKeysCacheControlInMs, TimeUnit.MILLISECONDS))
                    .build();
        }
        List<TraceKey> traceKeys =
                notifyMeDataService.findTraceKeys(DateUtil.toInstant(lastKeyBundleTag));
        ProblematicEventWrapper pew =
                ProblematicEventWrapper.newBuilder()
                        .setVersion(1)
                        .addAllEvents(mapToProblematicEvents(traceKeys))
                        .build();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(traceKeysCacheControlInMs, TimeUnit.MILLISECONDS))
                .header("content-type", "application/x-protobuf")
                .header(
                        HEADER_X_KEY_BUNDLE_TAG,
                        Long.toString(DateUtil.getLastFullBucketEndEpochMilli(bucketSizeInMs)))
                .body(pew);
    }

    private List<ProblematicEvent> mapToProblematicEvents(List<TraceKey> traceKeys) {
        return traceKeys.stream()
                .map(this::mapTraceKeyToProblematicEvent)
                .collect(Collectors.toList());
    }

    private ProblematicEvent mapTraceKeyToProblematicEvent(TraceKey t) {
        Builder b =
                ProblematicEvent.newBuilder()
                        .setSecretKeyForIdentity(ByteString.copyFrom(t.getSecretKeyForIdentity()))
                        .setIdentity(ByteString.copyFrom(t.getIdentity()))
                        .setStartTime(DateUtil.toEpochMilli(t.getStartTime()))
                        .setEndTime(DateUtil.toEpochMilli(t.getEndTime()));
        if (t.getMessage() != null) {
            b.setMessage(ByteString.copyFrom(t.getMessage()));
        }
        if (t.getNonce() != null) {
            b.setNonce(ByteString.copyFrom(t.getNonce()));
        }
        return b.build();
    }

    @PostMapping("/traceKeys")
    @Documentation(
            description = "Endpoint used to upload trace keys to the backend",
            responses = {
                "200=>The trace keys have been stored in the database",
                "403=>Authentication failed"
            })
    public @ResponseBody ResponseEntity<String> uploadTraceKeys(
            @Documentation(description = "Trace key to upload as JSON") @Valid @RequestBody
                    TraceKey traceKey,
            @AuthenticationPrincipal
                    @Documentation(
                            description = "JWT token that can be verified by the backend server")
                    Object principal) {
        notifyMeDataService.insertTraceKey(traceKey);
        return ResponseEntity.ok().body("OK");
    }

    @PostMapping(
            value = "/register",
            consumes = {"application/x-protobuf", "application/protobuf"})
    @Documentation(
            description = "Push registration",
            responses = {"200 => success", "400 => Error"})
    public @ResponseBody ResponseEntity<Void> registerPush(
            @RequestBody final PushRegistration pushRegistration) {
        pushRegistrationDataService.upsertPushRegistration(pushRegistration);
        return ResponseEntity.ok().build();
    }
}
