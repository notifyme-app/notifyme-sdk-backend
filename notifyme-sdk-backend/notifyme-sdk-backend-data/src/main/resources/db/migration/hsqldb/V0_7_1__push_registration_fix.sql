/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */

ALTER TABLE t_push_registration
    ALTER COLUMN push_token TYPE character varying(100);