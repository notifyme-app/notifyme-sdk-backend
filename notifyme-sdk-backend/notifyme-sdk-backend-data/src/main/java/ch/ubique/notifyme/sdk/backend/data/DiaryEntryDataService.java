/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.ubique.notifyme.sdk.backend.data;

import ch.ubique.notifyme.sdk.backend.model.event.CriticalEvent;
import ch.ubique.notifyme.sdk.backend.model.event.JavaDiaryEntry;
import java.util.List;

public interface DiaryEntryDataService {

    /**
     * Inserts the given diaryEntry into the db
     *
     * @param javaDiaryEntry
     */
    void insertDiaryEntry(JavaDiaryEntry javaDiaryEntry);

    /**
     * Inserts a list of diaryEntries
     *
     * @param diaryEntriesToInsert
     */
    void insertDiaryEntries(List<JavaDiaryEntry> diaryEntriesToInsert);

    List<CriticalEvent> getCriticalEvents();

    List<JavaDiaryEntry> getDiaryEntriesForEvent(CriticalEvent criticalEvent);
}
