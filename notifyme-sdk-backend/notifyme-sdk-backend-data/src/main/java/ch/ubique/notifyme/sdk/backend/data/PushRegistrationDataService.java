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

import ch.ubique.notifyme.sdk.backend.model.PushRegistrationOuterClass.PushRegistration;
import ch.ubique.notifyme.sdk.backend.model.PushRegistrationOuterClass.PushType;
import java.util.List;

public interface PushRegistrationDataService {

    /**
     * Inserts the given pushRegistration into the db
     *
     * @param pushRegistration
     */
    void upsertPushRegistration(final PushRegistration pushRegistration);

    /**
     * retrieves all pushRegistrations for a given pushType
     *
     * @param pushType
     */
    List<PushRegistration> getPushRegistrationByType(final PushType pushType);
}
