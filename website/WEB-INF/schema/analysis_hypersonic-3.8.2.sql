--
-- drop the uniqueness constraint from the 'job_runner_job' table, initially created in gp-3.7.6
-- Note: for hsqldb, I don't know of a way to drop the constraint from the table automatically from within this script
--
-- Manually, you can first get the constraint name,
--     SELECT constraint_name FROM INFORMATION_SCHEMA.SYSTEM_TABLE_CONSTRAINTS where table_name='JOB_RUNNER_JOB' and constraint_type='UNIQUE';
-- Then drop the constraint, e.g.
--     alter table job_runner_job drop constraint sys_ct_176;
--
-- Because we don't know in advance the constraint name, create a new table, copying all the data from the original
-- 

-- create tmp table ...
create table jrj_copy (
    gp_job_no integer not null,
    jr_classname varchar(255) not null,
    jr_name varchar(255) not null,
    ext_job_id varchar(255),
    job_state varchar(255),
    status_date timestamp not null,
    status_message varchar(255),
    exit_code integer,
    terminating_signal varchar(255),
    working_dir varchar(255) not null,
    stdin_file varchar(255),
    stdout_file varchar(255),
    log_file varchar(255),
    stderr_file varchar(255),
    primary key (gp_job_no)
);

-- ... as a copy of the job_runner_job table
insert into jrj_copy select * from job_runner_job;

drop table job_runner_job;

create table job_runner_job (
    gp_job_no integer not null,
    jr_classname varchar(511) not null,
    jr_name varchar(255) not null,
    ext_job_id varchar(255),
    job_state varchar(255),
    status_date timestamp not null,
    status_message varchar(2000),
    exit_code integer,
    terminating_signal varchar(255),
    working_dir varchar(255) not null,
    stdin_file varchar(255),
    stdout_file varchar(255),
    log_file varchar(255),
    stderr_file varchar(255),
    primary key (gp_job_no)
);
alter table job_runner_job add constraint gp_job_no_fk FOREIGN KEY (gp_job_no) references ANALYSIS_JOB(job_no) on delete cascade;

insert into job_runner_job  select * from jrj_copy;

drop table jrj_copy;

