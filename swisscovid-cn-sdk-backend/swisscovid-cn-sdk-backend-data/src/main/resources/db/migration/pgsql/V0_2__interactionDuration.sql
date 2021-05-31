/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */

CREATE TABLE t_interaction_duration
(
    pk_interaction_duration_id integer generated always as identity,
    duration                   int  NOT NULL,
    day                        date NOT NULL,
    CONSTRAINT pk_t_interaction_duration PRIMARY KEY (pk_interaction_duration_id)
);