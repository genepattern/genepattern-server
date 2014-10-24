create table JOB_INPUT (
    id bigint not null auto_increment,
    job_id integer,
    name varchar(255),
    user_value varchar(255),
    cmd_value varchar(255),
    kind varchar(255),
    primary key (id),
    unique (job_id, name));
create index idx_job_input_job_id on job_input (job_id);

create table JOB_INPUT_ATTRIBUTE (
    id bigint not null auto_increment,
    input_id bigint,
    name varchar(255),
    val varchar(255),
    primary key (id));
create index idx_jia_id on job_input_attribute (input_id);

create table JOB_RESULT (
    id bigint not null auto_increment,
    job_id integer,
    name varchar(255),
    path varchar(255),
    log bit not null,
    primary key (id),
    unique (job_id, name));
create index idx_job_result_job_id on job_result (job_id);

-- update schema version
insert into PROPS (`KEY`, VALUE) VALUES ('registeredVersion3.5.2', '3.5.2');

commit;
