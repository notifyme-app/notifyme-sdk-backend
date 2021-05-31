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

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public interface InteractionDurationDataService {

    /**
     * Inserts an interaction duration into the database
     *
     * @param interactionDuration duration of interaction in ms
     */
    public void insertInteraction(Integer interactionDuration);

    /**
     * Finds a list of durations since the given timestamp
     *
     * @param after minimum timestamp - is rounded down to the nearest full day
     * @return list of interaction durations in ms
     */
    public List<Integer> findInteractions(Instant after);

    /**
     * Removes all stored interaction durations with earlier day values
     *
     * @param retentionPeriod the date before which all stored durations are removed
     */
    public void removeDurations(Duration retentionPeriod);

}
