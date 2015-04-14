-- for GenomeSpace integration, link GP user account to GS user account
alter table GS_ACCOUNT add GS_USERID varchar (255);
alter table GS_ACCOUNT add TOKEN_TIMESTAMP timestamp;
alter table GS_ACCOUNT add GS_EMAIL varchar (255);


-- updates for Word Add-In bug fix
create table job_queue (
    job_no integer not null,
    date_submitted timestamp,
    parent_job_no integer,
    status varchar(255),
    primary key (job_no)
);

-- updates for long path names in user upload dir
alter table user_upload alter name varchar(512)
alter table user_upload alter path varchar(4000)

-- update schema version
update props set value='3.4.0' where key='schemaVersion';
