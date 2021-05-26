/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.ubique.swisscovid.cn.sdk.backend.model.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.util.Base64;

public class UrlBase64StringDeserializer extends StdDeserializer<byte[]> {

    public UrlBase64StringDeserializer() {
        this(null);
    }

    public UrlBase64StringDeserializer(Class t) {
        super(t);
    }

    @Override
    public byte[] deserialize(JsonParser jsonparser, DeserializationContext context)
            throws IOException {
        String base64 = jsonparser.getValueAsString();
        return Base64.getUrlDecoder().decode(base64.getBytes("UTF-8"));
    }
}
