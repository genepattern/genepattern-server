create table job_input_value (
    gp_job_no bigint not null references ANALYSIS_JOB(JOB_NO) on delete cascade,
    idx integer default 1 not null,
    pname TEXT not null,
    pvalue text default '',
    group_id TEXT default '',
    group_name TEXT default '',
    primary key (gp_job_no, idx, pname)
);

-- update schema version
UPDATE PROPS SET VALUE = '3.8.1' where KEY = 'schemaVersion';

commit;
