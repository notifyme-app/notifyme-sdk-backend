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

import ch.ubique.swisscovid.cn.sdk.backend.model.PushRegistrationOuterClass.PushRegistration;
import ch.ubique.swisscovid.cn.sdk.backend.model.PushRegistrationOuterClass.PushType;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.RowMapper;

public class PushRegistrationRowMapper implements RowMapper<PushRegistration> {

    @Override
    public PushRegistration mapRow(ResultSet rs, int rowNum) throws SQLException {
        return PushRegistration.newBuilder()
                .setPushType(PushType.valueOf(rs.getString("push_type")))
                .setPushToken(rs.getString("push_token"))
                .setDeviceId(rs.getString("device_id"))
                .build();
    }
}
