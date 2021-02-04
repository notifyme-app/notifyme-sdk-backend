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

import ch.ubique.notifyme.sdk.backend.data.DiaryEntryDataService;
import ch.ubique.notifyme.sdk.backend.data.NotifyMeDataService;
import ch.ubique.notifyme.sdk.backend.model.PreTraceWithProofOuterClass.PreTraceWithProof;
import ch.ubique.notifyme.sdk.backend.model.ProblematicDiaryEntryWrapperOuterClass.ProblematicDiaryEntryWrapper;
import ch.ubique.notifyme.sdk.backend.model.diaryentry.DiaryEntry;
import ch.ubique.notifyme.sdk.backend.model.tracekey.TraceKey;
import ch.ubique.notifyme.sdk.backend.model.util.DateUtil;
import ch.ubique.notifyme.sdk.backend.ws.CryptoWrapper;
import ch.ubique.openapi.docannotations.Documentation;
import com.google.protobuf.InvalidProtocolBufferException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/v1/debug")
@CrossOrigin({
    "https://upload-dev.notify-me.ch",
    "https://upload.notify-me.ch",
    "http://localhost:1313",
    "https://notify-me.c4dt.org"
})
public class DebugController {
    private static final Logger logger = LoggerFactory.getLogger(DebugController.class);

    private final NotifyMeDataService notifyMeDataService;
    private final DiaryEntryDataService diaryEntryDataService;
    private final CryptoWrapper cryptoWrapper;

    public DebugController(NotifyMeDataService notifyMeDataService,
            final DiaryEntryDataService diaryEntryDataService,
            CryptoWrapper cryptoWrapper) {
        this.notifyMeDataService = notifyMeDataService;
        this.diaryEntryDataService = diaryEntryDataService;
        this.cryptoWrapper = cryptoWrapper;
    }

    @GetMapping("")
    @Documentation(
            description = "Hello return",
            responses = {"200=>server live"})
    public @ResponseBody ResponseEntity<String> hello() {
        return ResponseEntity.ok()
                .header("X-HELLO", "notifyme")
                .body("Hello from NotifyMe Debug WS v1");
    }

    @PostMapping("/traceKey")
    public @ResponseBody ResponseEntity<String> uploadTraceKey(
            @RequestParam Long startTime,
            @RequestParam Long endTime,
            @RequestParam @Documentation(description = "list of url base64 encoded pre trace keys")
                    List<String> preTraces,
            @RequestParam
                    @Documentation(description = "list of the affected hours for the trace keys")
                    List<Integer> affectedHours,
            @RequestParam String message) {

        List<TraceKey> traceKeysToInsert = new ArrayList<>();
        for (int i = 0; i < affectedHours.size(); i++) {
            String preTraceKeyBase64 = preTraces.get(i);
            Integer affectedHour = affectedHours.get(i);

            TraceKey traceKey = new TraceKey();
            traceKey.setStartTime(DateUtil.toInstant(startTime));
            traceKey.setEndTime(DateUtil.toInstant(endTime));
            try {
                byte[] preTraceKeyBytes =
                        Base64.getUrlDecoder()
                                .decode(preTraceKeyBase64.getBytes(StandardCharsets.UTF_8));
                PreTraceWithProof preTraceWithProofProto =
                        PreTraceWithProof.parseFrom(preTraceKeyBytes);

                cryptoWrapper.calculateSecretKeyForIdentityAndIdentity(
                        preTraceWithProofProto, affectedHour, traceKey);

                byte[] nonce = cryptoWrapper.createNonceForMessageEncytion();
                byte[] encryptedMessage =
                        cryptoWrapper.encryptMessage(
                                preTraceWithProofProto
                                        .getPreTrace()
                                        .getNotificationKey()
                                        .toByteArray(),
                                nonce,
                                message);
                traceKey.setMessage(encryptedMessage);
                traceKey.setNonce(nonce);
                traceKeysToInsert.add(traceKey);
            } catch (InvalidProtocolBufferException e) {
                logger.error("unable to parse protobuf", e);
            }
        }
        notifyMeDataService.insertTraceKey(traceKeysToInsert);
        return ResponseEntity.ok("OK");
    }

    @PostMapping(
            value = "/diaryEntries",
            consumes = {"application/x-protobuf", "application/protobuf"})
    @Documentation(
            description = "Requests upload of all problematic diary entries",
            responses = {"200 => success"})
    public @ResponseBody ResponseEntity<String> postDiaryEntries(
            @RequestBody ProblematicDiaryEntryWrapper problematicDiaryEntryWrapper) {
        logger.debug(
                "received {} problematicDiaryEntries",
                problematicDiaryEntryWrapper.getDiaryEntriesCount());

        final var diaryEntries = problematicDiaryEntryWrapper.getDiaryEntriesList().stream()
                .map(DiaryEntry::from)
                .collect(Collectors.toList());
        diaryEntryDataService.insertDiaryEntries(diaryEntries);

        return ResponseEntity.ok("OK");
    }
}
