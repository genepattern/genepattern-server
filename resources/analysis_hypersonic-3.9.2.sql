-- add Resource Requirements columns
alter table job_runner_job add column
    req_mem bigint default null
before working_dir;

alter table job_runner_job add column
    req_cpu_count integer default null
before working_dir;

alter table job_runner_job add column
    req_node_count integer default null
before working_dir;

alter table job_runner_job add column
    req_walltime  varchar(15) default null
before working_dir;

alter table job_runner_job add column
    req_queue  varchar(255) default null
before working_dir;

-- update schema version
update props set value='3.9.2' where key='schemaVersion';
