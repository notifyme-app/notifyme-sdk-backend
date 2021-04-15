/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2021. All rights reserved.
 */

CREATE TABLE t_trace_key_v3
(
    pk_trace_key_id           integer generated always as identity,
    version                   integer                  NOT NULL,
    identity                  bytea                    NOT NULL,
    secret_key_for_identity   bytea                    NOT NULL,
    start_time                timestamp with time zone NOT NULL,
    end_time                  timestamp with time zone NOT NULL,
    encrypted_associated_data bytea,
    cipher_text_nonce         bytea,
    created_at                timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT pk_t_trace_key_v3 PRIMARY KEY (pk_trace_key_id)
);