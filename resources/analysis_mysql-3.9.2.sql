-- add Resource Requirements columns
alter table JOB_RUNNER_JOB add column
    req_mem bigint unsigned default null
before working_dir;

alter table JOB_RUNNER_JOB add column
    req_cpu_count integer default null
before working_dir;

alter table JOB_RUNNER_JOB add column
    req_node_count integer default null
before working_dir;

alter table JOB_RUNNER_JOB add column
    req_walltime  varchar(15) default null
before working_dir;

alter table JOB_RUNNER_JOB add column
    req_queue  varchar(255) default null
before working_dir;

-- update schema version
update PROPS set value='3.9.2' where `key`='schemaVersion';
