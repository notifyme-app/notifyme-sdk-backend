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
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"dev", "enable-debug"})
public class DebugControllerV3Test extends BaseControllerTest {
    private String traceKeysEndpoint;
    @Autowired private NotifyMeDataServiceV3 notifyMeDataServiceV3;

    @Before
    public void setUp() {
        final String debugControllerEndPoint = "/v3/debug";
        traceKeysEndpoint = debugControllerEndPoint + "/traceKey";
    }

    @Test
    public void helloTest() throws Exception {
        String hello =
                mockMvc
                        .perform(get("/v3/debug"))
                        .andExpect(status().is2xxSuccessful())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        assertTrue(hello.contains("Hello from NotifyMe Debug WS v3"));
    }

    @Test
    @Ignore("Not implemented")
    public void uploadTraceKeyTest() throws Exception {
        // TODO: Create pretrace keys and upload (use cryptoWrapper)
    }
}
