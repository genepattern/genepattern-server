-- add Resource Requirements columns
alter table JOB_RUNNER_JOB add 
    req_mem bigint unsigned default null
after terminating_signal;

alter table JOB_RUNNER_JOB add column
    req_cpu_count integer default null
after req_mem;

alter table JOB_RUNNER_JOB add column
    req_node_count integer default null
after req_cpu_count;

alter table JOB_RUNNER_JOB add column
    req_walltime  varchar(15) default null
after req_node_count;

alter table JOB_RUNNER_JOB add column
    req_queue  varchar(255) default null
after req_walltime;

-- add installedPatchLSIDs table
create table PATCH_INFO (
    id bigint not null auto_increment, 
    lsid varchar(255) not null,
    user_id varchar(255) default null,
    url varchar(255) default null,
    patch_dir varchar(255) default null,
    status_date timestamp not null,
    primary key (id),
    unique (lsid)
);

-- initialize with default values, previously set in genepattern.properties file
insert into patch_info (lsid) values ('urn:lsid:broad.mit.edu:cancer.software.genepattern.server.patch:00002:1');
insert into patch_info (lsid) values ('urn:lsid:broad.mit.edu:cancer.software.genepattern.server.patch:00004:1');
insert into patch_info (lsid) values ('urn:lsid:broad.mit.edu:cancer.software.genepattern.server.patch:00006:1');
insert into patch_info (lsid) values ('urn:lsid:broad.mit.edu:cancer.software.genepattern.server.patch:00007:1');
insert into patch_info (lsid) values ('urn:lsid:broad.mit.edu:cancer.software.genepattern.server.patch:00008:1');
insert into patch_info (lsid) values ('urn:lsid:broad.mit.edu:cancer.software.genepattern.server.patch:00009:1');
insert into patch_info (lsid) values ('urn:lsid:broad.mit.edu:cancer.software.genepattern.server.patch:00012:1');

commit;
