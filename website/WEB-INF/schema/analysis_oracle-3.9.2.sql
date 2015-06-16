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
    patch_dir varchar(255 char) default null,
    status_date timestamp default sysdate not null,
    primary key (id),
    unique (lsid)
);
create sequence patch_info_SEQ;

-- initialize with default values, previously set in genepattern.properties file
insert into patch_info (id, lsid) values (patch_info_SEQ.nextVal, 'urn:lsid:broad.mit.edu:cancer.software.genepattern.server.patch:00002:1');
insert into patch_info (id, lsid) values (patch_info_SEQ.nextVal, 'urn:lsid:broad.mit.edu:cancer.software.genepattern.server.patch:00004:1');
insert into patch_info (id, lsid) values (patch_info_SEQ.nextVal, 'urn:lsid:broad.mit.edu:cancer.software.genepattern.server.patch:00006:1');
insert into patch_info (id, lsid) values (patch_info_SEQ.nextVal, 'urn:lsid:broad.mit.edu:cancer.software.genepattern.server.patch:00007:1');
insert into patch_info (id, lsid) values (patch_info_SEQ.nextVal, 'urn:lsid:broad.mit.edu:cancer.software.genepattern.server.patch:00008:1');
insert into patch_info (id, lsid) values (patch_info_SEQ.nextVal, 'urn:lsid:broad.mit.edu:cancer.software.genepattern.server.patch:00009:1');
insert into patch_info (id, lsid) values (patch_info_SEQ.nextVal, 'urn:lsid:broad.mit.edu:cancer.software.genepattern.server.patch:00012:1');

-- update schema version
UPDATE PROPS SET VALUE = '3.9.2' where KEY = 'schemaVersion';
commit;
