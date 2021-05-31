/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.ubique.swisscovid.cn.sdk.backend.ws.controller;

import ch.ubique.swisscovid.cn.sdk.backend.data.SwissCovidDataService;
import ch.ubique.swisscovid.cn.sdk.backend.data.PushRegistrationDataService;
import ch.ubique.swisscovid.cn.sdk.backend.data.UUIDDataService;
import ch.ubique.swisscovid.cn.sdk.backend.model.PushRegistrationOuterClass.PushRegistration;
import ch.ubique.swisscovid.cn.sdk.backend.model.UserUploadPayloadOuterClass.UserUploadPayload;
import ch.ubique.swisscovid.cn.sdk.backend.model.tracekey.TraceKey;
import ch.ubique.swisscovid.cn.sdk.backend.model.util.DateUtil;
import ch.ubique.swisscovid.cn.sdk.backend.model.v3.ProblematicEventWrapperOuterClass.ProblematicEvent;
import ch.ubique.swisscovid.cn.sdk.backend.model.v3.ProblematicEventWrapperOuterClass.ProblematicEvent.Builder;
import ch.ubique.swisscovid.cn.sdk.backend.model.v3.ProblematicEventWrapperOuterClass.ProblematicEventWrapper;
import ch.ubique.swisscovid.cn.sdk.backend.ws.insertmanager.InsertException;
import ch.ubique.swisscovid.cn.sdk.backend.ws.insertmanager.InsertManager;
import ch.ubique.swisscovid.cn.sdk.backend.ws.security.RequestValidator;
import ch.ubique.swisscovid.cn.sdk.backend.ws.security.RequestValidator.InvalidOnsetException;
import ch.ubique.swisscovid.cn.sdk.backend.ws.security.RequestValidator.NotAJwtException;
import ch.ubique.swisscovid.cn.sdk.backend.ws.security.RequestValidator.WrongAudienceException;
import ch.ubique.swisscovid.cn.sdk.backend.ws.security.RequestValidator.WrongScopeException;
import ch.ubique.swisscovid.cn.sdk.backend.ws.util.CryptoWrapper;
import ch.ubique.swisscovid.cn.sdk.backend.ws.util.DateTimeUtil;
import ch.ubique.openapi.docannotations.Documentation;
import com.google.protobuf.ByteString;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/v3")
@CrossOrigin(origins = {"https://www.pt-d.bfs.admin.ch", "https://www.pt1-d.bfs.admin.ch"})
public class SwissCovidControllerV3 {
  private static final String HEADER_X_KEY_BUNDLE_TAG = "x-key-bundle-tag";
  private static final Logger logger = LoggerFactory.getLogger(SwissCovidControllerV3.class);

  private final SwissCovidDataService dataService;
  private final InsertManager insertManager;
  private final PushRegistrationDataService pushRegistrationDataService;
  private final UUIDDataService uuidDataService;
  private final RequestValidator requestValidator;
  private final CryptoWrapper cryptoWrapper;

  private final String revision;
  private final Long bucketSizeInMs;
  private final Long traceKeysCacheControlInMs;
  private final Duration requestTime;

  public SwissCovidControllerV3(
      SwissCovidDataService dataService,
      InsertManager insertManager,
      PushRegistrationDataService pushRegistrationDataService,
      UUIDDataService uuidDataService,
      RequestValidator requestValidator,
      CryptoWrapper cryptoWrapper,
      String revision,
      Long bucketSizeInMs,
      Long traceKeysCacheControlInMs,
      Duration requestTime) {
    this.dataService = dataService;
    this.insertManager = insertManager;
    this.pushRegistrationDataService = pushRegistrationDataService;
    this.uuidDataService = uuidDataService;
    this.requestValidator = requestValidator;
    this.revision = revision;
    this.bucketSizeInMs = bucketSizeInMs;
    this.traceKeysCacheControlInMs = traceKeysCacheControlInMs;
    this.requestTime = requestTime;
    this.cryptoWrapper = cryptoWrapper;
  }

  @GetMapping(value = "")
  @Documentation(
      description = "Hello return",
      responses = {"200=>server live"})
  public @ResponseBody ResponseEntity<String> hello() {
    return ResponseEntity.ok()
        .header("X-HELLO", "notifyme")
        .body("Hello from SwissCovid WS v3.\n" + revision);
  }

  @GetMapping(
      value = "/traceKeys",
      produces = {"application/json"})
  public @ResponseBody ResponseEntity<List<TraceKey>> getTraceKeysJson(
      @RequestParam(required = false) Long lastKeyBundleTag) {
    if (!isValidKeyBundleTag(lastKeyBundleTag)) {
      return ResponseEntity.notFound().build();
    }
    final var traceKeys = dataService.findTraceKeys(DateUtil.toInstant(lastKeyBundleTag));
    Collections.shuffle(traceKeys);
    return ResponseEntity.ok()
        .cacheControl(CacheControl.maxAge(traceKeysCacheControlInMs, TimeUnit.MILLISECONDS))
        .header(
            HEADER_X_KEY_BUNDLE_TAG,
            Long.toString(DateUtil.getLastFullBucketEndEpochMilli(bucketSizeInMs)))
        .body(traceKeys);
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
          "Requests trace keys uploaded after _lastKeyBundleTag_. If _lastKeyBundleTag_ is"
              + " ommited, all uploaded trace keys are returned",
      responses = {
        "200 => protobuf/json of all keys in that interval. response header _x-key-bundle-tag_"
            + " contains _lastKeyBundleTag_ for next request",
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
    final var traceKeys = dataService.findTraceKeys(DateUtil.toInstant(lastKeyBundleTag));
    Collections.shuffle(traceKeys);
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
            .setDay(t.getDay().getEpochSecond());
    if (t.getEncryptedAssociatedData() != null) {
      b.setEncryptedAssociatedData(ByteString.copyFrom(t.getEncryptedAssociatedData()));
    }
    if (t.getCipherTextNonce() != null) {
      b.setCipherTextNonce(ByteString.copyFrom(t.getCipherTextNonce()));
    }
    return b.build();
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

  /**
   * Upload of stored identities if user tested positive and wishes to notify other visitors: -
   * Sanity check on stored (e.g. no overlapping timestamps) - Generate traceKey = secretKey_I using
   * an identity and the master secretkey - Store tracekey such that other user can poll for
   * possible exposure events
   *
   * @param userUploadPayload Protobuf containing the identities stored locally in the app and a
   *     version number
   * @return Status ok if sanity check passed and trace keys successfully uploaded
   */
  @PostMapping(
      value = "/userupload",
      consumes = {"application/x-protobuf", "application/protobuf"})
  @Documentation(
      description = "User upload of stored identities",
      responses = {
              "200 => success",
              "400 => Bad Upload Data: List of VenueInfo objects was null or empty",
              "403 => Authentication failed: Invalid JWT Data"})
  public @ResponseBody Callable<ResponseEntity<String>> userUpload(
      @Documentation(description = "Identities to upload as protobuf") @Valid @RequestBody
          final UserUploadPayload userUploadPayload,
      @RequestHeader(value = "User-Agent")
      @Documentation(
              description =
                      "App Identifier (PackageName/BundleIdentifier) + App-Version +"
                              + " OS (Android/iOS) + OS-Version",
              example = "ch.ubique.android.dp3t;1.0;iOS;13.3")
              String userAgent,
      @AuthenticationPrincipal
          @Documentation(description = "JWT token that can be verified by the backend server")
          Object principal)
      throws WrongScopeException, WrongAudienceException, NotAJwtException, InvalidOnsetException, InsertException {

    final var now = LocalDateTime.now();

    requestValidator.isValid(principal);

    insertManager.insertIntoDatabase(userUploadPayload, principal, now);

    return () -> {
      try {
        DateTimeUtil.normalizeDuration(now, requestTime);
      } catch (DateTimeUtil.DurationExpiredException e) {
        logger.error("Total time spent in endpoint is longer than requestTime");
      }
      return ResponseEntity.ok().build();
    };
  }

  @ExceptionHandler({
          InsertException.class
  })
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ResponseEntity<Object> invalidArguments() {
    return ResponseEntity.badRequest().build();
  }

  @ExceptionHandler({
          WrongScopeException.class,
          WrongAudienceException.class,
          NotAJwtException.class,
          InvalidOnsetException.class
  })
  @ResponseStatus(HttpStatus.FORBIDDEN)
  public ResponseEntity<Object> forbidden() {
    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
  }
}
