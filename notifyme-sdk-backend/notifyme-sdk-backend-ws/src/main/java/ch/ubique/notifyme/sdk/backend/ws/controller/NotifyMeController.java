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
import ch.ubique.notifyme.sdk.backend.model.ProblematicEventWrapperOuterClass.ProblematicEvent;
import ch.ubique.notifyme.sdk.backend.model.ProblematicEventWrapperOuterClass.ProblematicEventWrapper;
import ch.ubique.notifyme.sdk.backend.model.tracekey.TraceKey;
import ch.ubique.notifyme.sdk.backend.model.tracekey.TraceKeyUploadPayload;
import ch.ubique.notifyme.sdk.backend.model.util.DateUtil;
import ch.ubique.openapi.docannotations.Documentation;
import com.google.protobuf.ByteString;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/v1")
public class NotifyMeController {
    private static final String HEADER_X_KEY_BUNDLE_TAG = "x-key-bundle-tag";

    private final NotifyMeDataService dataService;
    private final String revision;
    private final Long bucketSizeInMs;

    public NotifyMeController(
            NotifyMeDataService dataService, String revision, Long bucketSizeInMs) {
        this.dataService = dataService;
        this.revision = revision;
        this.bucketSizeInMs = bucketSizeInMs;
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
                .header(
                        HEADER_X_KEY_BUNDLE_TAG,
                        Long.toString(DateUtil.getLastFullBucketEndEpochMilli(bucketSizeInMs)))
                .body(dataService.findTraceKeys(DateUtil.toLocalDateTime(lastKeyBundleTag)));
    }

    private boolean isValidKeyBundleTag(Long lastKeyBundleTag) {
        return lastKeyBundleTag == null
                || ((DateUtil.isBucketAligned(lastKeyBundleTag, bucketSizeInMs))
                        && (DateUtil.isInThePast(lastKeyBundleTag)));
    }

    @GetMapping(
            value = "/traceKeys",
            produces = {"application/protobuf"})
    @Documentation(
            description =
                    "Requests trace keys uploaded after _lastKeyBundleTag_. If _lastKeyBundleTag_ is ommited, all uploaded trace keys are returned",
            responses = {
                "200 => protobuf/json of all keys in that interval. response header _x-key-bundle-tag_ contains _lastKeyBundleTag_ for next request",
                "404 => Invalid _lastKeyBundleTag_"
            })
    public @ResponseBody ResponseEntity<byte[]> getTraceKeys(
            @RequestParam(required = false)
                    @Documentation(
                            description =
                                    "in millis since epoch. must be aligned to a full hour, and < now()")
                    Long lastKeyBundleTag) {
        if (!isValidKeyBundleTag(lastKeyBundleTag)) {
            return ResponseEntity.notFound().build();
        }
        List<TraceKey> traceKeys =
                dataService.findTraceKeys(DateUtil.toLocalDateTime(lastKeyBundleTag));
        ProblematicEventWrapper pew =
                ProblematicEventWrapper.newBuilder()
                        .setVersion(1)
                        .addAllEvents(mapToProblematicEvents(traceKeys))
                        .build();
        return ResponseEntity.ok()
                .header(
                        HEADER_X_KEY_BUNDLE_TAG,
                        Long.toString(DateUtil.getLastFullBucketEndEpochMilli(bucketSizeInMs)))
                .body(pew.toByteArray());
    }

    private List<ProblematicEvent> mapToProblematicEvents(List<TraceKey> traceKeys) {
        return traceKeys.stream()
                .map(
                        t ->
                                ProblematicEvent.newBuilder()
                                        .setSecretKey(ByteString.copyFrom(t.getSecretKey()))
                                        .setStartTime(DateUtil.toEpochMilli(t.getStartTime()))
                                        .setEndTime(DateUtil.toEpochMilli(t.getEndTime()))
                                        .build())
                .collect(Collectors.toList());
    }

    @PostMapping("/traceKeys")
    @Documentation(
            description = "Endpoint used to upload trace keys to the backend",
            responses = {
                "200=>The trace keys have been stored in the database",
                "403=>Authentication failed"
            })
    public @ResponseBody ResponseEntity<String> uploadTraceKeys(
            @Documentation(description = "JSON Object containing all keys.") @Valid @RequestBody
                    TraceKeyUploadPayload payload) {
        dataService.insertTraceKeys(payload.getTraceKeys());
        return ResponseEntity.ok().body("OK");
    }
}
