/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.ubique.notifyme.sdk.backend.ws.config;

import ch.ubique.notifyme.sdk.backend.ws.security.KeyVault;
import ch.ubique.notifyme.sdk.backend.ws.security.KeyVault.PublicKeyNoSuitableEncodingFoundException;
import ch.ubique.notifyme.sdk.backend.ws.security.NotifyMeJwtDecoder;
import ch.ubique.notifyme.sdk.backend.ws.security.NotifyMeJwtValidator;
import ch.ubique.notifyme.sdk.backend.ws.util.KeyHelper;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.Duration;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;

@Configuration
@EnableWebSecurity
@Profile(value = "jwt")
public class JwtConfig {

    public static class CommonJWTBase extends WebSecurityConfigurerAdapter {
        @Value("${ws.app.jwt.publickey}")
        String publicKey;

        @Value("${ws.app.jwt.maxValidityMinutes: 60}")
        int maxValidityMinutes;

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http.sessionManagement()
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                    .and()
                    .csrf()
                    .disable()
                    .cors()
                    .and()
                    .authorizeRequests()
                    .antMatchers(HttpMethod.POST, "/v1/traceKeys")
                    .authenticated()
                    .anyRequest()
                    .permitAll()
                    .and()
                    .oauth2ResourceServer()
                    .jwt();
        }

        @Bean
        public NotifyMeJwtValidator jwtValidator() {
            return new NotifyMeJwtValidator("traceKey", Duration.ofMinutes(maxValidityMinutes));
        }

        @Bean
        public JwtDecoder jwtDecoder()
                throws InvalidKeySpecException, NoSuchAlgorithmException, IOException,
                        PublicKeyNoSuitableEncodingFoundException {
            NotifyMeJwtDecoder jwtDecoder =
                    new NotifyMeJwtDecoder(KeyVault.loadPublicKey(loadPublicKey(), "RSA"));

            OAuth2TokenValidator<Jwt> defaultValidators = JwtValidators.createDefault();
            jwtDecoder.setJwtValidator(
                    new DelegatingOAuth2TokenValidator<>(defaultValidators, jwtValidator()));
            return jwtDecoder;
        }

        private String loadPublicKey() throws IOException {
            if (publicKey.startsWith("keycloak:")) {
                String url = publicKey.replace("keycloak:/", "");
                return KeyHelper.getPublicKeyFromKeycloak(url);
            }
            InputStream in = null;
            if (publicKey.startsWith("classpath:/")) {
                in = new ClassPathResource(publicKey.substring(11)).getInputStream();
                return readAsStringFromInputStreamAndClose(in);
            } else if (publicKey.startsWith("file:/")) {
                in = new FileInputStream(publicKey);
                return readAsStringFromInputStreamAndClose(in);
            }
            return publicKey;
        }

        private String readAsStringFromInputStreamAndClose(InputStream in) throws IOException {
            String result = IOUtils.toString(in, "UTF-8");
            in.close();
            return result;
        }
    }
}
