create table job_input_value (
    gp_job_no bigint not null references ANALYSIS_JOB(JOB_NO) on delete cascade,
    idx integer default 1 not null,
    pname TEXT not null,
    pvalue text default '',
    group_id TEXT default '',
    group_name TEXT default '',
    primary key (gp_job_no, idx, pname)
);

commit;
