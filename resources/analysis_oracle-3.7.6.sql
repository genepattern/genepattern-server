--
-- for the DrmExecutor, updated job table which keeps track of job runner specific external job id
--
create table job_runner_job (
    gp_job_no integer not null,
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
    primary key (gp_job_no),
    unique (jr_classname, jr_name, ext_job_id)
);
create index idx_jrj_ext_job_id on JOB_RUNNER_JOB (ext_job_id);

-- update schema version
update props set value='3.7.6' where key='schemaVersion';
