
/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */

set database sql syntax PGS true;

CREATE TABLE t_trace_key (
    secret_key   bytea NOT NULL,
    pk_trace_key serial NOT NULL,
    start_time   timestamp with time zone NOT NULL,
    end_time     timestamp with time zone NOT NULL,
    created_at   timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT pk_t_trace_key PRIMARY KEY ( pk_trace_key )
);
