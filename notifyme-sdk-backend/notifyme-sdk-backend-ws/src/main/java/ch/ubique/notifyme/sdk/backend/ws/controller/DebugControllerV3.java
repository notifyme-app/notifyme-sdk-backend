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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.protobuf.InvalidProtocolBufferException;

import ch.ubique.notifyme.sdk.backend.data.NotifyMeDataServiceV3;
import ch.ubique.notifyme.sdk.backend.model.tracekey.v3.TraceKey;
import ch.ubique.notifyme.sdk.backend.model.v3.NotifyMeAssociatedDataOuterClass.EventCriticality;
import ch.ubique.notifyme.sdk.backend.model.v3.NotifyMeAssociatedDataOuterClass.NotifyMeAssociatedData;
import ch.ubique.notifyme.sdk.backend.model.v3.PreTraceWithProofOuterClass.PreTraceWithProof;
import ch.ubique.notifyme.sdk.backend.ws.util.CryptoWrapper;
import ch.ubique.openapi.docannotations.Documentation;

@RestController
@RequestMapping("/v3/debug")
@CrossOrigin({
    "https://upload-dev.notify-me.ch",
    "https://upload.notify-me.ch",
    "http://localhost:1313",
    "https://notify-me.c4dt.org",
    "https://notify-me-dev.c4dt.org"
})
public class DebugControllerV3 {
    private static final Logger logger = LoggerFactory.getLogger(DebugControllerV3.class);

    private final NotifyMeDataServiceV3 notifyMeDataServiceV3;
    private final CryptoWrapper cryptoWrapper;

    public DebugControllerV3(
            final NotifyMeDataServiceV3 notifyMeDataServiceV3,
            final CryptoWrapper cryptoWrapper) {
        this.notifyMeDataServiceV3 = notifyMeDataServiceV3;
        this.cryptoWrapper = cryptoWrapper;
    }

    @GetMapping("")
    @Documentation(
            description = "Hello return",
            responses = {"200=>server live"})
    public ResponseEntity<String> hello() {
        return ResponseEntity.ok()
                .header("X-HELLO", "notifyme")
                .body("Hello from NotifyMe Debug WS v3");
    }

    @PostMapping("/traceKey")
    public ResponseEntity<String> uploadTraceKey(
            @RequestParam @Documentation(description = "list of url base64 encoded pre trace keys")
                    List<String> preTraces, @RequestParam String message, @RequestParam Integer criticality) {

        List<TraceKey> traceKeysToInsert = new ArrayList<>();
        
        NotifyMeAssociatedData countryData = NotifyMeAssociatedData.newBuilder().setCriticality(EventCriticality.forNumber(criticality)).setVersion(1).build();
        
        for (int i = 0; i < preTraces.size(); i++) {
            String preTraceKeyBase64 = preTraces.get(i);
            try {
                byte[] preTraceKeyBytes =
                        Base64.getUrlDecoder()
                                .decode(preTraceKeyBase64.getBytes(StandardCharsets.UTF_8));
                PreTraceWithProof preTraceWithProofProto =
                        PreTraceWithProof.parseFrom(preTraceKeyBytes);
                TraceKey traceKey = cryptoWrapper.getCryptoUtilV3().createTraceV3(preTraceWithProofProto, message, countryData.toByteArray());
                traceKeysToInsert.add(traceKey);
            } catch (InvalidProtocolBufferException e) {
                logger.error("unable to parse protobuf", e);
            }
        }
        notifyMeDataServiceV3.insertTraceKey(traceKeysToInsert);
        return ResponseEntity.ok("OK");
    }

}
