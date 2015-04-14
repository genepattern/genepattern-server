-- add installedPatchLSIDs table
create table patch_info (
    id serial primary key,
    lsid text not null,
    user_id text default null,
    url text default null,
    patch_dir text default null,
    status_date timestamp default current_timestamp not null,
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

-- update schema version
UPDATE PROPS SET VALUE = '3.9.2' where KEY = 'schemaVersion';
commit;
