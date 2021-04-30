/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.ubique.notifyme.sdk.backend.model.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.util.Base64;

public class UrlBase64StringSerializer extends StdSerializer<byte[]> {

    public UrlBase64StringSerializer() {
        this(null);
    }

    public UrlBase64StringSerializer(Class<byte[]> t) {
        super(t);
    }

    @Override
    public void serialize(byte[] value, JsonGenerator gen, SerializerProvider arg2)
            throws IOException {
        gen.writeString(new String(Base64.getUrlEncoder().encode(value)));
    }
}
