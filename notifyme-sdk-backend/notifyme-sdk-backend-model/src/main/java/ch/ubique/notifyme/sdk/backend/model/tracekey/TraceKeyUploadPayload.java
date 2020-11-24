/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.ubique.notifyme.sdk.backend.model.tracekey;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

public class TraceKeyUploadPayload {

    @NotNull
    @Valid
    @NotEmpty
    private List<TraceKey> traceKeys;

    public List<TraceKey> getTraceKeys() {
        return traceKeys;
    }

    public void setTraceKeys(List<TraceKey> traceKeys) {
        this.traceKeys = traceKeys;
    }
}
