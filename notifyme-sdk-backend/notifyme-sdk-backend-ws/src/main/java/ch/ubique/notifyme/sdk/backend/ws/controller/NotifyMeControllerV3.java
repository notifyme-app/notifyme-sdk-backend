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

import ch.ubique.notifyme.sdk.backend.data.NotifyMeDataServiceV3;
import ch.ubique.notifyme.sdk.backend.model.v3.ProblematicEventWrapperOuterClass.ProblematicEvent;
import ch.ubique.notifyme.sdk.backend.model.v3.ProblematicEventWrapperOuterClass.ProblematicEvent.Builder;
import ch.ubique.notifyme.sdk.backend.model.v3.ProblematicEventWrapperOuterClass.ProblematicEventWrapper;
import ch.ubique.notifyme.sdk.backend.model.tracekey.v3.TraceKey;
import ch.ubique.notifyme.sdk.backend.model.util.DateUtil;
import ch.ubique.openapi.docannotations.Documentation;
import com.google.protobuf.ByteString;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/v3")
@CrossOrigin(origins = {"https://notify-me.c4dt.org", "https://notify-me-dev.c4dt.org"})
public class NotifyMeControllerV3 {
  private static final String HEADER_X_KEY_BUNDLE_TAG = "x-key-bundle-tag";

  private final NotifyMeDataServiceV3 dataService;
  private final String revision;
  private final Long bucketSizeInMs;
  private final Long traceKeysCacheControlInMs;

  public NotifyMeControllerV3(
      NotifyMeDataServiceV3 dataService,
      String revision,
      Long bucketSizeInMs,
      Long traceKeysCacheControlInMs) {
    this.dataService = dataService;
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
        .body("Hello from NotifyMe WS v3.\n" + revision);
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
        .body(dataService.findTraceKeys(DateUtil.toInstant(lastKeyBundleTag)));
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
              description = "in millis since epoch. must be aligned to a full hour, and < now()")
          Long lastKeyBundleTag) {
    if (!isValidKeyBundleTag(lastKeyBundleTag)) {
      return ResponseEntity.notFound()
          .cacheControl(CacheControl.maxAge(traceKeysCacheControlInMs, TimeUnit.MILLISECONDS))
          .build();
    }
    List<TraceKey> traceKeys = dataService.findTraceKeys(DateUtil.toInstant(lastKeyBundleTag));
    ProblematicEventWrapper pew =
        ProblematicEventWrapper.newBuilder()
            .setVersion(3)
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
    return traceKeys.stream().map(this::mapTraceKeyToProblematicEvent).collect(Collectors.toList());
  }

  private ProblematicEvent mapTraceKeyToProblematicEvent(TraceKey t) {
    Builder b =
        ProblematicEvent.newBuilder()
                .setVersion(t.getVersion())
                .setIdentity(ByteString.copyFrom(t.getIdentity()))
                .setSecretKeyForIdentity(ByteString.copyFrom(t.getSecretKeyForIdentity()))
            .setStartTime(DateUtil.toEpochMilli(t.getStartTime()))
            .setEndTime(DateUtil.toEpochMilli(t.getEndTime()));
    if (t.getEncryptedAssociatedData() != null) {
      b.setEncryptedAssociatedData(ByteString.copyFrom(t.getEncryptedAssociatedData()));
    }
    if (t.getCipherTextNonce() != null) {
      b.setCipherTextNonce(ByteString.copyFrom(t.getCipherTextNonce()));
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
          @Documentation(description = "JWT token that can be verified by the backend server")
          Object principal) {
    dataService.insertTraceKey(traceKey);
    return ResponseEntity.ok().body("OK");
  }
}
