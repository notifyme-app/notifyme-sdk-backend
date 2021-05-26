/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2021. All rights reserved.
 */

CREATE TABLE t_redeem_uuid
(
    pk_redeem_uuid_id integer generated always as identity,
    uuid              Character varying(50) NOT NULL,
    received_at       timestamp             NOT NULL,
    CONSTRAINT pk_t_redeem_uuid PRIMARY KEY (pk_redeem_uuid_id),
    CONSTRAINT ak_t_redeem_uuid UNIQUE (uuid)
)