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
import ch.ubique.notifyme.sdk.backend.data.NotifyMeDataServiceV2;
import ch.ubique.notifyme.sdk.backend.data.NotifyMeDataServiceV3;
import ch.ubique.notifyme.sdk.backend.model.DiaryEntryWrapperOuterClass.DiaryEntryWrapper;
import ch.ubique.notifyme.sdk.backend.model.PreTraceWithProofOuterClass.PreTraceWithProof;
import ch.ubique.notifyme.sdk.backend.model.event.JavaDiaryEntry;
import ch.ubique.notifyme.sdk.backend.model.tracekey.v2.TraceKey;
import ch.ubique.notifyme.sdk.backend.model.util.DateUtil;
import ch.ubique.notifyme.sdk.backend.model.v3.NotifyMeAssociatedDataOuterClass.EventCriticality;
import ch.ubique.notifyme.sdk.backend.model.v3.NotifyMeAssociatedDataOuterClass.NotifyMeAssociatedData;
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
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/debug")
@CrossOrigin({ "https://upload-dev.notify-me.ch", "https://upload.notify-me.ch", "http://localhost:1313",
                "https://notify-me.c4dt.org", "https://notify-me-dev.c4dt.org" })
public class DebugControllerV2 {
    private static final Logger logger = LoggerFactory.getLogger(DebugControllerV2.class);

    private final NotifyMeDataServiceV2 notifyMeDataServiceV2;
    private final NotifyMeDataServiceV3 notifyMeDataServiceV3;
    private final DiaryEntryDataService diaryEntryDataService;
    private final CryptoWrapper cryptoWrapper;

    public DebugControllerV2(final NotifyMeDataServiceV2 notifyMeDataServiceV2,
                    final NotifyMeDataServiceV3 notifyMeDataServiceV3,
                    final DiaryEntryDataService diaryEntryDataService, final CryptoWrapper cryptoWrapper) {
        this.notifyMeDataServiceV2 = notifyMeDataServiceV2;
        this.notifyMeDataServiceV3 = notifyMeDataServiceV3;
        this.diaryEntryDataService = diaryEntryDataService;
        this.cryptoWrapper = cryptoWrapper;
    }

    @GetMapping("")
    @Documentation(description = "Hello return", responses = { "200=>server live" })
    public ResponseEntity<String> hello() {
        return ResponseEntity.ok().header("X-HELLO", "notifyme").body("Hello from NotifyMe Debug WS v1");
    }

    @PostMapping("/traceKey")
    public ResponseEntity<String> uploadTraceKey(@RequestParam Long startTime, @RequestParam Long endTime,
                    @RequestParam @Documentation(description = "list of url base64 encoded pre trace keys") List<String> preTraces,
                    @RequestParam @Documentation(description = "list of the affected hours for the trace keys") List<Integer> affectedHours,
                    @RequestParam String message, @RequestParam Integer criticality) {

        List<TraceKey> traceKeysToInsert = new ArrayList<>();
        List<ch.ubique.notifyme.sdk.backend.model.tracekey.v3.TraceKey> traceKeysV3ToInsert = new ArrayList<>();
        for (int i = 0; i < affectedHours.size(); i++) {
            String preTraceKeyBase64 = preTraces.get(i);
            Integer affectedHour = affectedHours.get(i);

            TraceKey traceKey = new TraceKey();
            traceKey.setStartTime(DateUtil.toInstant(startTime));
            traceKey.setEndTime(DateUtil.toInstant(endTime));
            try {
                byte[] preTraceKeyBytes = Base64.getUrlDecoder()
                                .decode(preTraceKeyBase64.getBytes(StandardCharsets.UTF_8));
                PreTraceWithProof preTraceWithProofProto = PreTraceWithProof.parseFrom(preTraceKeyBytes);

                cryptoWrapper.calculateSecretKeyForIdentityAndIdentity(preTraceWithProofProto, affectedHour, traceKey);

                byte[] nonce = cryptoWrapper.createNonceForMessageEncytion();
                byte[] encryptedMessage = cryptoWrapper.encryptMessage(
                                preTraceWithProofProto.getPreTrace().getNotificationKey().toByteArray(), nonce,
                                message);
                traceKey.setMessage(encryptedMessage);
                traceKey.setNonce(nonce);
                traceKeysToInsert.add(traceKey);

                // insert v2 into v3
                NotifyMeAssociatedData countryData = NotifyMeAssociatedData.newBuilder()
                                .setCriticality(EventCriticality.forNumber(criticality)).setVersion(1).build();
                byte[] encryptedAssociatedData = cryptoWrapper.encryptAssociatedData(
                                preTraceWithProofProto.getPreTrace().getNotificationKey().toByteArray(), message,
                                countryData.toByteArray(), nonce);
                var traceKeyV3 = new ch.ubique.notifyme.sdk.backend.model.tracekey.v3.TraceKey();
                traceKeyV3.setCipherTextNonce(traceKey.getNonce());
                traceKeyV3.setEncryptedAssociatedData(encryptedAssociatedData);
                traceKeyV3.setEndTime(traceKey.getEndTime());
                traceKeyV3.setStartTime(traceKey.getStartTime());
                traceKeyV3.setIdentity(traceKey.getIdentity());
                traceKeyV3.setSecretKeyForIdentity(traceKey.getSecretKeyForIdentity());
                traceKeysV3ToInsert.add(traceKeyV3);
            } catch (InvalidProtocolBufferException e) {
                logger.error("unable to parse protobuf", e);
            }
        }
        notifyMeDataServiceV2.insertTraceKey(traceKeysToInsert);
        notifyMeDataServiceV3.insertTraceKey(traceKeysV3ToInsert);
        return ResponseEntity.ok("OK");
    }

    @PostMapping(value = "/diaryEntries", consumes = { "application/x-protobuf", "application/protobuf" })
    @Documentation(description = "Requests upload of all diary entries", responses = { "200 => success" })
    public ResponseEntity<String> postDiaryEntries(@RequestBody final DiaryEntryWrapper diaryEntryWrapper) {
        logger.debug("received {} diaryEntries", diaryEntryWrapper.getDiaryEntriesCount());

        final var diaryEntries = diaryEntryWrapper.getDiaryEntriesList().stream().map(JavaDiaryEntry::from)
                        .collect(Collectors.toList());
        diaryEntryDataService.insertDiaryEntries(diaryEntries);

        return ResponseEntity.ok("OK");
    }
}
