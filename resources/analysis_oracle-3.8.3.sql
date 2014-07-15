create table  job_output (
    gp_job_no integer not null,
    path varchar(255) not null,
    file_length number(19,0),
    last_modified timestamp,
    extension varchar(255),
    kind varchar(255),
    gpFileType varchar(255),
    hidden number(1,0) default 0 not null,
    deleted number(1,0) default 0 not null,
    primary key (gp_job_no, path),
    constraint jo_gpjn_fk foreign key (GP_JOB_NO) references ANALYSIS_JOB(JOB_NO) on delete cascade
);

--
-- add time logging columns to the job_runner_job table
--
alter table job_runner_job add ( 
    submit_time timestamp default null,
    start_time timestamp default null,
    end_time timestamp default null,
    cpu_time number(19,0) default 0,
    max_mem number(19,0) default 0,
    max_swap number(19,0) default 0,
    max_processes integer default 0,
    max_threads integer default 0
);

-- update schema version
update props set value='3.8.3' where key='schemaVersion';

commit;

