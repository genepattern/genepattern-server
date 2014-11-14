--
-- for the DrmExecutor, updated job table which keeps track of job runner specific external job id
--
create table JOB_RUNNER_JOB (
    gp_job_no integer not null references ANALYSIS_JOB(job_no) on delete cascade,
    lsid varchar(255),
    jr_classname varchar(255) not null,
    jr_name varchar(255) not null,
    ext_job_id varchar(255),
    job_state varchar(255),
    status_date timestamp not null,
    status_message varchar(255),
    exit_code integer,
    terminating_signal varchar(255),
    working_dir varchar(255) not null,
    stdin_file varchar(255),
    stdout_file varchar(255),
    log_file varchar(255),
    stderr_file varchar(255),
    queue_id varchar(511) default null,
    submit_time timestamp null default null,
    start_time timestamp null default null,
    end_time timestamp null default null,
    cpu_time bigint unsigned default null,
    max_mem bigint unsigned default null,
    max_swap bigint unsigned default null,
    max_processes integer default null,
    max_threads integer default null,
    primary key (gp_job_no)
);
create index idx_jrj_ext_job_id on JOB_RUNNER_JOB (ext_job_id);

-- update schema version
UPDATE PROPS SET VALUE = '3.7.6' where `KEY` = 'schemaVersion';

commit;
