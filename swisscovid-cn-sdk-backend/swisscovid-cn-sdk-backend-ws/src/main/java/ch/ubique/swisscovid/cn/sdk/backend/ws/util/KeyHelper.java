/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.ubique.swisscovid.cn.sdk.backend.ws.util;

import ch.ubique.notifyme.sdk.backend.model.keycloak.KeyCloakPublicKey;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URL;

public class KeyHelper {

  private static ObjectMapper objectMapper = new ObjectMapper();

  private KeyHelper() {}

  public static String getPublicKeyFromKeycloak(String url) throws IOException {
    URL jsonUrl = new URL(url);
    KeyCloakPublicKey publicKey = objectMapper.readValue(jsonUrl, KeyCloakPublicKey.class);
    return publicKey.getPublicKey();
  }
}
