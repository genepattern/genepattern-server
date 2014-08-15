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

alter table job_runner_job add ( 
    lsid varchar(255)
);


--
-- add queue_id column to the job_runner_job table
--
alter table job_runner_job add ( 
    queue_id varchar(511) default null
);

--
-- add time logging columns to the job_runner_job table
--
alter table job_runner_job add ( 
    submit_time timestamp default null,
    start_time timestamp default null,
    end_time timestamp default null,
    cpu_time number(19,0) default null,
    max_mem number(19,0) default null,
    max_swap number(19,0) default null,
    max_processes integer default null,
    max_threads integer default null
);

create table queue_congestion (
    id number(19,0) not null,
    queuetime number(19,0),
    queue varchar(255 char),
    primary key (id),
    unique (queue)
);
create sequence queue_congestion_SEQ;

-- update schema version
update props set value='3.9.0' where key='schemaVersion';

commit;

