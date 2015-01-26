create table JOB_INPUT (
    id bigint not null auto_increment,
    job_id integer,
    name varchar(255),
    user_value varchar(255),
    cmd_value varchar(255),
    kind varchar(255),
    primary key (id),
    unique (job_id, name));
create index IDX_JOB_INPUT_JOB_ID on JOB_INPUT (job_id);

create table JOB_INPUT_ATTRIBUTE (
    id bigint not null auto_increment,
    input_id bigint,
    name varchar(255),
    val varchar(255),
    primary key (id));
create index IDX_JIA_ID on JOB_INPUT_attribute (input_id);

create table JOB_RESULT (
    id bigint not null auto_increment,
    job_id integer,
    name varchar(255),
    path varchar(255),
    log bit not null,
    primary key (id),
    unique (job_id, name));
create index IDX_JOB_RESULT_JOB_ID on JOB_RESULT (job_id);

-- update schema version
UPDATE PROPS SET VALUE = '3.5.2' where `KEY` = 'schemaVersion';

commit;
