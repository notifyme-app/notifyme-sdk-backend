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

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"dev", "enable-debug"})
@Ignore("run manually with server running locally on #port")
public class LiveDebugControllerTest extends BaseControllerTest {
    private String liveDiaryEntriesEndPoint;

    @Before
    public void setUp() {
        final String liveDebugControllerEndPoint = "http://localhost:" + 8080 + "/v1/debug";
        liveDiaryEntriesEndPoint = liveDebugControllerEndPoint + "/diaryEntries";
    }

    @Test
    public void uploadDiaryEntryProtobufShouldReturnOk() {
        final var wrapper = DebugControllerTestHelper.getTestDiaryEntryWrapper();

        final var headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/x-protobuf"));
        final HttpEntity<byte[]> entity = new HttpEntity<>(wrapper.toByteArray(), headers);
        final ResponseEntity<String> response =
                new RestTemplate()
                        .postForEntity(URI.create(liveDiaryEntriesEndPoint), entity, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        logger.info("responseBody: {}", response.getBody());
        assertThat(response.getBody()).isEqualTo("OK");
    }
}
