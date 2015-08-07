create table job_input (
    id bigint primary key,
    job_id decimal(38),
    name text,
    user_value text,
    cmd_value text,
    type text,
    unique (job_id, name)
);
create index idx_job_input_job_id on job_input (job_id);

create table job_input_attribute (
    id bigint primary key,
    input_id bigint,
    name text,
    val text
);
create index idx_jia_id on job_input_attribute (input_id);

create table job_result (
    id bigint primary key,
    job_id decimal(38),
    name text,
    path text,
    log boolean default false not null,
    unique (job_id, name)
);
create index idx_job_result_job_id on job_result (job_id);

commit;
