create table  job_output (
    gp_job_no integer not null,
    path varchar(255) not null,
    file_length bigint,
    last_modified timestamp,
    extension varchar(255),
    kind varchar(255),
    gpFileType varchar(255),
    hidden bit not null,
    deleted bit not null,
    primary key (gp_job_no, path),
    constraint jo_gpjn_fk foreign key (GP_JOB_NO) references ANALYSIS_JOB(JOB_NO) on delete cascade
);

--
-- add time logging columns to the job_runner_job table
--
alter table job_runner_job add column 
    submit_time timestamp default null
before status_date;

alter table job_runner_job add column 
    start_time timestamp default null
before status_date;

alter table job_runner_job add column 
    end_time timestamp default null
before status_date;

alter table job_runner_job add column
    cpu_time bigint default 0
before status_date;
    
alter table job_runner_job add column
    max_mem bigint default 0
before status_date;

alter table job_runner_job add column
    max_swap bigint default 0
before status_date;

alter table job_runner_job add column
    max_processes integer default 0
before status_date;

alter table job_runner_job add column
    max_threads integer default 0
before status_date;

-- update schema version
update props set value='3.8.3' where key='schemaVersion';

