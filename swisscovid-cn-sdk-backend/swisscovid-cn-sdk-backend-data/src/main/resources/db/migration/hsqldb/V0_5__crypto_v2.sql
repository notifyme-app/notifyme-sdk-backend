/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */

drop table t_trace_key;

CREATE TABLE t_trace_key (
	pk_trace_key_id serial NOT NULL,
	identity bytea NOT NULL,
    secret_key_for_identity bytea NOT NULL,
    start_time   timestamp with time zone NOT NULL,
    end_time     timestamp with time zone NOT NULL,
    message bytea,
    message_nonce bytea,
    created_at   timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT pk_t_trace_key PRIMARY KEY ( pk_trace_key_id )
);