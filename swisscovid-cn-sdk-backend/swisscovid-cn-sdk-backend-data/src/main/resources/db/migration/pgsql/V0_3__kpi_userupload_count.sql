/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */

CREATE TABLE t_userupload_checkin_count
(
    pk_userupload_checkin_count integer generated always as identity,
    upload_date                         date NOT NULL,
    count                       int  NOT NULL,
    CONSTRAINT pk_t_userupload_checkin_count PRIMARY KEY (pk_userupload_checkin_count)
);