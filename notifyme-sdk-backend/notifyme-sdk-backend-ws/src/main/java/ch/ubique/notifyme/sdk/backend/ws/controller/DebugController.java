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

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.protobuf.InvalidProtocolBufferException;

import ch.ubique.notifyme.sdk.backend.data.NotifyMeDataService;
import ch.ubique.notifyme.sdk.backend.model.PreTraceWithProofOuterClass.PreTraceWithProof;
import ch.ubique.notifyme.sdk.backend.model.tracekey.TraceKey;
import ch.ubique.notifyme.sdk.backend.model.util.DateUtil;
import ch.ubique.notifyme.sdk.backend.ws.CryptoWrapper;
import ch.ubique.openapi.docannotations.Documentation;

@Controller
@RequestMapping("/v1/debug")
@CrossOrigin(origins = { "https://upload-dev.notify-me.ch", "https://upload.notify-me.ch", "http://localhost:1313", "https://notify-me.c4dt.org", "https://notify-me-dev.c4dt.org" })
public class DebugController {
    private static final Logger logger = LoggerFactory.getLogger(DebugController.class);

    private final NotifyMeDataService dataService;
    private final CryptoWrapper cryptoWrapper;

    public DebugController(NotifyMeDataService dataService, CryptoWrapper cryptoWrapper) {
        this.dataService = dataService;
        this.cryptoWrapper = cryptoWrapper;
    }

    @GetMapping(value = "")
    @Documentation(description = "Hello return", responses = { "200=>server live" })
    public @ResponseBody ResponseEntity<String> hello() {
        return ResponseEntity.ok().header("X-HELLO", "notifyme").body("Hello from NotifyMe Debug WS v1");
    }

    @PostMapping(value = "/traceKey")
    public @ResponseBody ResponseEntity<String> uploadTraceKey(
            @RequestParam Long startTime,
            @RequestParam Long endTime,
            @RequestParam @Documentation(description = "list of url base64 encoded pre trace keys")
                    List<String> preTraces,
            @RequestParam @Documentation(description = "list of the affected hours for the trace keys")
                    List<Integer> affectedHours,                    
            @RequestParam String message)
            throws UnsupportedEncodingException {
        
        List<TraceKey> traceKeysToInsert = new ArrayList<>();
        for (int i = 0; i < affectedHours.size(); i++) {
            String preTraceKeyBase64 = preTraces.get(i);
            Integer affectedHour = affectedHours.get(i);
            
            TraceKey traceKey = new TraceKey();
            traceKey.setStartTime(DateUtil.toInstant(startTime));
            traceKey.setEndTime(DateUtil.toInstant(endTime));
            try {
                byte[] preTraceKeyBytes = Base64.getUrlDecoder().decode(preTraceKeyBase64.getBytes("UTF-8"));
                PreTraceWithProof preTraceWithProofProto = PreTraceWithProof.parseFrom(preTraceKeyBytes);
                
                cryptoWrapper.calculateSecretKeyForIdentityAndIdentity(preTraceWithProofProto, affectedHour, traceKey);
                
                byte[] nonce = cryptoWrapper.createNonceForMessageEncytion();
                byte[] encryptedMessage = cryptoWrapper.encryptMessage(preTraceWithProofProto.getPreTrace().getNotificationKey().toByteArray(), nonce, message);
                traceKey.setMessage(encryptedMessage);
                traceKey.setNonce(nonce);
                logger.info("secretKeyForIdentity in mapper: {}",
                        Hex.encodeHexString(traceKey.getSecretKeyForIdentity()));
                traceKeysToInsert.add(traceKey); 
            } catch (InvalidProtocolBufferException e) {
                logger.error("unable to parse protobuf", e);
            }
        }
        dataService.insertTraceKey(traceKeysToInsert);
        return ResponseEntity.ok().body("OK");
    }
}
