/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */

set database sql syntax PGS true;

CREATE TABLE t_trace_key
(
    pk_trace_key_id           integer generated always as identity,
    version                   integer                  NOT NULL,
    identity                  bytea                    NOT NULL,
    secret_key_for_identity   bytea                    NOT NULL,
    encrypted_associated_data bytea,
    cipher_text_nonce         bytea,
    created_at                timestamp with time zone NOT NULL DEFAULT now(),
    day                       timestamp with time zone NOT NULL,
    CONSTRAINT pk_t_trace_key_v3 PRIMARY KEY (pk_trace_key_id)
);

CREATE TABLE t_push_registration
(
    pk_push_registration_id integer                NOT NULL GENERATED ALWAYS AS IDENTITY,
    push_type               character varying(20)  NOT NULL,
    push_token              character varying(100) NOT NULL,
    device_id               character varying(255) NOT NULL,
    CONSTRAINT pk_t_push_registration PRIMARY KEY (pk_push_registration_id),
    CONSTRAINT u_push_token UNIQUE (push_token),
    CONSTRAINT u_device_id UNIQUE (device_id)
);

CREATE TABLE t_redeem_uuid
(
    pk_redeem_uuid_id integer generated always as identity,
    uuid              Character varying(50) NOT NULL,
    received_at       timestamp             NOT NULL,
    CONSTRAINT pk_t_redeem_uuid PRIMARY KEY (pk_redeem_uuid_id),
    CONSTRAINT ak_t_redeem_uuid UNIQUE (uuid)
);