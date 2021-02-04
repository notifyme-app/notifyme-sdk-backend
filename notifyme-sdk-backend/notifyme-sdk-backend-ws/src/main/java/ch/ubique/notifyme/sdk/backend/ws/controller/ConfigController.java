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

import ch.ubique.notifyme.sdk.backend.model.config.ConfigResponse;
import ch.ubique.notifyme.sdk.backend.ws.semver.InvalidAppVersionFormatException;
import ch.ubique.notifyme.sdk.backend.ws.semver.InvalidUserAgentException;
import ch.ubique.notifyme.sdk.backend.ws.semver.UserAgent;
import ch.ubique.openapi.docannotations.Documentation;
import java.time.Duration;
import javax.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
public class ConfigController {

    @GetMapping("/config")
    @Documentation(
            description =
                    "Read latest configuration, depending on the version of the phone and the app.",
            responses = {
                "200 => ConfigResponse with config parameters",
                "400 => Invalid or improperly formatted user-agent or app-version"
            })
    public ResponseEntity<ConfigResponse> getConfig(
            @RequestHeader(value = "User-Agent")
                    @Documentation(
                            description =
                                    "App Identifier (PackageName/BundleIdentifier) + App-Version +"
                                            + " OS (Android/iOS) + OS-Version",
                            example = "ch.ubique.ios.notifyme;1.0.0;iOS;13.3")
                    String userAgent) {
        UserAgent nmUserAgent = new UserAgent(userAgent);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)))
                .body(new ConfigResponse());
    }

    @ExceptionHandler({InvalidUserAgentException.class, InvalidAppVersionFormatException.class})
    public ResponseEntity<String> invalidUserAgent(HttpServletRequest req, Exception ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
