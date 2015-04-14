--
-- for the DrmExecutor, updated job table which keeps track of job runner specific external job id
--
create table job_runner_job (
    gp_job_no bigint primary key references ANALYSIS_JOB(job_no) on delete cascade,
    jr_classname text not null,
    jr_name text not null,
    ext_job_id text,
    job_state text,
    status_date timestamp not null,
    status_message text,
    exit_code decimal(38),
    terminating_signal text,
    working_dir text not null,
    stdin_file text,
    stdout_file text,
    log_file text,
    stderr_file text,
    lsid text,
    queue_id TEXT default null,
    submit_time timestamp default null,
    start_time timestamp default null,
    end_time timestamp default null,
    cpu_time bigint default null,
    max_mem bigint default null,
    max_swap bigint default null,
    max_processes integer default null,
    max_threads integer default null,
    req_mem bigint default null,
    req_cpu_count integer default null,
    req_node_count integer default null,
    req_walltime  text default null,
    req_queue  text default null
);
create index idx_jrj_ext_job_id on JOB_RUNNER_JOB (ext_job_id);


-- update schema version
UPDATE PROPS SET VALUE = '3.7.6' where KEY = 'schemaVersion';

commit;
