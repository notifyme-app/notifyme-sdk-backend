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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import javax.servlet.Filter;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"postgres", "test-config", "dev"})
// @TestPropertySource(properties = {})
@Transactional
public abstract class BaseControllerTest {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  protected ObjectMapper objectMapper;

  protected MockMvc mockMvc;

  @Autowired private WebApplicationContext webApplicationContext;

  // !!!!! IMPORTANT: Must be added to filter-chain of mockMvc, otherwise security configs are
  // ignored !!!!!
  @Autowired private Filter springSecurityFilterChain;

  private final boolean enableSecurity;

  public BaseControllerTest(boolean enableSecurity) {
    this.enableSecurity = enableSecurity;
  }

  @Before
  public void setup() throws Exception {
    DefaultMockMvcBuilder builder = MockMvcBuilders.webAppContextSetup(webApplicationContext);
    if (enableSecurity) {
      builder.addFilters(springSecurityFilterChain);
    }
    this.mockMvc = builder.build();
    this.objectMapper = new ObjectMapper(new JsonFactory());
    this.objectMapper.registerModule(new JavaTimeModule());
    // this makes sure, that the objectmapper does not fail, when no filter is provided.
    this.objectMapper.setFilterProvider(new SimpleFilterProvider().setFailOnUnknownId(false));
  }

  protected String json(Object o) throws IOException {
    return objectMapper.writeValueAsString(o);
  }
}
