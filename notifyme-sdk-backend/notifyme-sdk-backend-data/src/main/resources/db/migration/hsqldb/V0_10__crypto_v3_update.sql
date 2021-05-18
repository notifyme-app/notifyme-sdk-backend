/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2021. All rights reserved.
 */
delete from t_trace_key_v3;
alter table t_trace_key_v3 drop column end_time;
alter table t_trace_key_v3 drop column start_time;
alter table t_trace_key_v3 add column day timestamp with time zone NOT NULL;