-- create table  job_output (
--    gp_job_no integer not null,
--    path varchar(255) not null,
--    deleted bit not null,
--    extension varchar(255),
--    file_length bigint,
--    gpFileType varchar(255),
--    hidden bit not null,
--    kind varchar(255),
--    last_modified timestamp,
--    primary key (gp_job_no, path),
--    unique (gp_job_no, path)
-- );

-- update schema version
update props set value='3.8.3' where key='schemaVersion';

