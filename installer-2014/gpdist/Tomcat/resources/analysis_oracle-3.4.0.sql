-- for GenomeSpace integration, link GP user account to GS user account
alter table GS_ACCOUNT add(GS_USERID VARCHAR2(512));
alter table GS_ACCOUNT add(TOKEN_TIMESTAMP TIMESTAMP);
alter table GS_ACCOUNT add (GS_EMAIL VARCHAR2(512));

-- updates for Word Add-In bug fix
create table job_queue (
    job_no number(10,0) not null,
    date_submitted timestamp,
    parent_job_no number(10,0),
    status varchar2(255 char),
    primary key (job_no)
);

-- updates for long path names in user upload dir
alter table user_upload modify name varchar2(512)
alter table user_upload modify path varchar2(4000)

-- update schema version
insert into PROPS (KEY, VALUE) VALUES ('registeredVersion3.4.0', '3.4.0');
commit;
