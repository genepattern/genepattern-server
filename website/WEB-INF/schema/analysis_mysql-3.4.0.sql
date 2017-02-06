-- for GenomeSpace integration, link GP user account to GS user account
alter table GS_ACCOUNT add(GS_USERID VARCHAR(512));
alter table GS_ACCOUNT add(TOKEN_TIMESTAMP TIMESTAMP NULL);
alter table GS_ACCOUNT add (GS_EMAIL VARCHAR(512));

-- updates for Word Add-In bug fix
create table JOB_QUEUE (
    job_no integer not null,
    date_submitted timestamp null,
    parent_job_no integer not null,
    status varchar(255),
    primary key (job_no)
);

commit;
