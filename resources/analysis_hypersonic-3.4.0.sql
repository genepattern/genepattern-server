-- for GenomeSpace integration, link GP user account to GS user account
alter table GS_ACCOUNT add GS_USERID varchar;
alter table GS_ACCOUNT add TOKEN_TIMESTAMP timestamp;
alter table GS_ACCOUNT add GS_EMAIL varchar;


-- updates for Word Add-In bug fix
create table job_queue (
    job_no integer not null,
    date_submitted timestamp,
    parent_job_no integer,
    status varchar(255),
    primary key (job_no)
);

-- update schema version
update props set value='3.4.0' where key='schemaVersion';
