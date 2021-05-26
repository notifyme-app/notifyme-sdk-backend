/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.ubique.notifyme.sdk.backend.data;

import java.time.Duration;

public interface UUIDDataService {

    /**
     * Checks if the passed uuid is unique and insert it into the DB if it is
     * Note: If the uuid turns out to not be unique, the DB isn't otherwise modified
     * Insert timestamps are rounded up to the beginning of the next day so as to preserve privacy
     * @param uuid as contained in a given JWT token
     * @return return true if the uuid has been inserted. if the uuid is not valid, returns false.
     */
    boolean checkAndInsertPublishUUID(String uuid);

    /**
     * Clean up db and remove entries older than the retention days.
     * Entries should only be removed if the JWT's validity period has expired
     * (the validity period is usually pretty short, so this shouldn't be a problem)
     * @param retentionPeriod Duration describing amount of time a uuid needs to be kept in the DB
     */
    void cleanDB(Duration retentionPeriod);
}
