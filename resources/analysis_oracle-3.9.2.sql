-- add Resource Requirements columns
alter table job_runner_job add (
    req_mem number(19,0) default null,
    req_cpu_count integer default null,
    req_node_count integer default null,
    req_walltime  varchar(15 char) default null,
    req_queue  varchar(255) default null
);

-- add installedPatchLSIDs table
create table patch_info (
    id number(19,0) not null,
    lsid varchar(255 char) not null,
    user_id varchar(255 char) default null,
    url varchar(255 char) default null,
    status_date timestamp not null,
    primary key (id),
    unique (lsid)
);
create sequence patch_info_SEQ;

-- update schema version
UPDATE PROPS SET VALUE = '3.9.2' where KEY = 'schemaVersion';
commit;
