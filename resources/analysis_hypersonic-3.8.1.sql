create table job_input_value (
    gp_job_no integer not null,
    idx integer default 1 not null,
    pname varchar(255) not null,
    pvalue varchar(1023) not null,
    group_id varchar(255) default '' not null,
    group_name varchar(255) default '' not null,
    primary key (gp_job_no, idx, pname),
    constraint jiv_gpjn_fk foreign key (GP_JOB_NO) references ANALYSIS_JOB(JOB_NO) on delete cascade
);

-- update schema version
update props set value='3.8.1' where key='schemaVersion';

