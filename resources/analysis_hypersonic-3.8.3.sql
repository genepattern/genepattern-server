create table  job_output (
    gp_job_no integer not null,
    path varchar(255) not null,
    file_length bigint,
    last_modified timestamp,
    extension varchar(255),
    kind varchar(255),
    gpFileType varchar(255),
    hidden bit not null,
    deleted bit not null,
    primary key (gp_job_no, path)
);

-- oracle
--create table  job_output (
--    gp_job_no integer not null,
--    path varchar(255) not null,
--    file_length number(19,0),
--    last_modified timestamp,
--    extension varchar(255),
--    kind varchar(255),
--    gpFileType varchar(255),
--    hidden number(1,0) default 0 not null,
--    deleted number(1,0) default 0 not null,
--    primary key (gp_job_no, path)
--);


-- update schema version
update props set value='3.8.3' where key='schemaVersion';

