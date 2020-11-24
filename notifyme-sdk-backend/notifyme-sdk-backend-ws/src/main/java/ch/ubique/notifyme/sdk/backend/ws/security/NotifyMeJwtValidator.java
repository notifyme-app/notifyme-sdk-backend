/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.ubique.notifyme.sdk.backend.ws.security;

import java.time.Duration;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public class NotifyMeJwtValidator implements OAuth2TokenValidator<Jwt> {

    private static final String SCOPE = "scope";

    private final String validScope;
    private final Duration maxJwtValidity;

    public NotifyMeJwtValidator(String validScope, Duration maxJwtValidity) {
        this.validScope = validScope;
        this.maxJwtValidity = maxJwtValidity;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        // make sure the token has an expiration date AND is not valid for more than maxJwtValidity
        if (token.getExpiresAt() == null
                || token.getIssuedAt().plus(maxJwtValidity).isBefore(token.getExpiresAt())) {
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST));
        }
        // validate scope
        if (Boolean.TRUE.equals(token.containsClaim(SCOPE))
                && token.getClaim(SCOPE).equals(validScope)) {
            return OAuth2TokenValidatorResult.success();
        }
        return OAuth2TokenValidatorResult.failure(new OAuth2Error(OAuth2ErrorCodes.INVALID_SCOPE));
    }
}
