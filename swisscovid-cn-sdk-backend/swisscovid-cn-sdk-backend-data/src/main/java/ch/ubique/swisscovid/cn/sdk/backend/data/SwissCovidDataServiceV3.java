/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.ubique.swisscovid.cn.sdk.backend.data;

import ch.ubique.swisscovid.cn.sdk.backend.model.tracekey.v3.TraceKey;

import java.time.Instant;
import java.util.List;

public interface SwissCovidDataServiceV3 {

    /**
     * Inserts the given trace key into the db
     *
     * @param traceKey
     */
    public void insertTraceKey(TraceKey traceKey);

    /**
     * Wrapper around findTraceKeys where before is set to the end of the previous bucket
     *
     * @param after
     * @return
     */
    public List<TraceKey> findTraceKeys(Instant after);

    /**
     * Removes trace keys with an end time before the given date
     *
     * @param before
     * @return
     */
    public int removeTraceKeys(Instant before);

    /**
     * Inserts a list of trace keys
     * 
     * @param traceKeysToInsert
     */
    public void insertTraceKey(List<TraceKey> traceKeysToInsert);
}
