-- for GenomeSpace integration, link GP user account to GS user account
alter table GS_ACCOUNT add GS_USERID text;
alter table GS_ACCOUNT add TOKEN_TIMESTAMP TIMESTAMP;
alter table GS_ACCOUNT add GS_EMAIL text;

-- updates for Word Add-In bug fix
create table job_queue (
    job_no bigint primary key,
    date_submitted timestamp,
    parent_job_no bigint,
    status text
);

-- updates for long path names in user upload dir
alter table user_upload alter column name type text;
alter table user_upload alter column path type TEXT;

commit;
