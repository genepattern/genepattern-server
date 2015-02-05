-- add Resource Requirements columns
alter table job_runner_job add (
    req_mem number(19,0) default null,
    req_cpu_count integer default null,
    req_node_count integer default null,
    req_walltime  varchar(15 char) default null,
    req_queue  varchar(255) default null
);

-- update schema version
UPDATE PROPS SET VALUE = '3.9.2' where KEY = 'schemaVersion';
commit;
