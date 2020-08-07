--
-- create ANALYSIS_JOB_ARCHIVE and JOB_RUNNER_JOB_ARCHIVE tables and related trigger for stats reporting
-- 
--   Copy each deleted row from the analysis_job table into the analysis_job_archive table
--   Copy each deleted row from the job_runner_job table into the job_runner_job_archive table
--

create table analysis_job_archive (
  job_no int not null unique, 
  task_id int, 
  status_id int, 
  date_submitted timestamp, 
  date_completed timestamp, 
  parameter_info longvarchar,
  user_id longvarchar, 
  isIndexed bit, 
  access_id int, 
  job_name longvarchar, 
  lsid varchar(200), 
  task_lsid varchar(200),
  task_name longvarchar,
  parent int,
  deleted bit
);

create table job_runner_job_archive (
    gp_job_no integer not null,
    lsid varchar(255),
    jr_classname varchar(255) not null,
    jr_name varchar(255) not null,
    ext_job_id varchar(255),
    queue_id varchar(511),
    job_state varchar(255),
    submit_time timestamp,
    start_time timestamp,
    end_time timestamp,
    cpu_time bigint,
    max_mem bigint,
    max_swap bigint,
    max_processes integer,
    max_threads integer,
    status_date timestamp not null,
    status_message varchar(2000),
    exit_code integer,
    terminating_signal varchar(255),
    req_mem bigint,
    req_cpu_count integer,
    req_node_count integer,
    req_walltime  varchar(15),
    req_queue  varchar(255),
    working_dir varchar(255) not null,
    stdin_file varchar(255),
    stdout_file varchar(255),
    log_file varchar(255),
    stderr_file varchar(255)
);

drop trigger if exists on_analysis_job_del;

create trigger on_analysis_job_del
after delete on analysis_job 
referencing old row as OLD
for each row
begin atomic
  insert into analysis_job_archive (
    job_no, 
    task_id, 
    status_id, 
    date_submitted,
    date_completed, 
    parameter_info, 
    user_id, 
    isindexed, 
    access_id,
    job_name, 
    lsid, 
    task_lsid, 
    task_name, 
    parent,
    deleted 
  )
  VALUES (
    OLD.job_no, 
    OLD.task_id, 
    OLD.status_id, 
    OLD.date_submitted,
    OLD.date_completed, 
    OLD.parameter_info,
    OLD.user_id, 
    OLD.isindexed, 
    OLD.access_id,
    OLD.job_name, 
    OLD.lsid, 
    OLD.task_lsid, 
    OLD.task_name, 
    OLD.parent,
    OLD.deleted
  )\;
end;

drop trigger if exists on_job_runner_job_del;

create trigger on_job_runner_job_del
after delete on job_runner_job 
referencing old row as OLD
for each row
begin atomic
  insert into job_runner_job_archive (
    gp_job_no,
    lsid, 
    jr_classname, 
    jr_name, 
    ext_job_id, 
    job_state, 
    status_date, 
    status_message, 
    exit_code, 
    terminating_signal, 
    req_mem, 
    req_cpu_count, 
    req_node_count, 
    req_walltime, 
    req_queue, 
    working_dir, 
    stdin_file, 
    stdout_file, 
    log_file, 
    stderr_file, 
    queue_id, 
    submit_time, 
    start_time, 
    end_time, 
    cpu_time, 
    max_mem, 
    max_swap, 
    max_processes, 
    max_threads
  )
  VALUES ( 
    OLD.gp_job_no,
    OLD.lsid, 
    OLD.jr_classname, 
    OLD.jr_name, 
    OLD.ext_job_id, 
    OLD.job_state, 
    OLD.status_date, 
    OLD.status_message, 
    OLD.exit_code, 
    OLD.terminating_signal, 
    OLD.req_mem, 
    OLD.req_cpu_count, 
    OLD.req_node_count, 
    OLD.req_walltime, 
    OLD.req_queue, 
    OLD.working_dir, 
    OLD.stdin_file, 
    OLD.stdout_file, 
    OLD.log_file, 
    OLD.stderr_file, 
    OLD.queue_id, 
    OLD.submit_time, 
    OLD.start_time, 
    OLD.end_time, 
    OLD.cpu_time, 
    OLD.max_mem, 
    OLD.max_swap, 
    OLD.max_processes, 
    OLD.max_threads
  )\;
end;

--
-- create TASK_DOCKER_IMAGE view 
--   to make it easier to query the job.docker.image for
--   all installed modules
-- 
create view TASK_DOCKER_IMAGE as
select 
  TASK_NAME,
  REGEXP_SUBSTRING(lsid,'[0-9.]*.$') as "LSID_VERSION",
  REPLACE(
    REGEXP_SUBSTRING(
      REGEXP_SUBSTRING(taskinfoattributes, 'job.docker.image">\n<object>.*'), 
      '[^<>]*</object>'
    ),
    '</object>', 
    ''
  ) as DOCKER_IMAGE
from task_master
where taskinfoattributes like '%job.docker.image%';

create view analysis_job_total as
select 
  job_no,
  task_id, 
  status_id, 
  date_submitted, 
  date_completed, 
  user_id, 
  isIndexed, 
  access_id, 
  job_name, 
  lsid, 
  task_lsid,
  task_name,
  parent,
  deleted
from analysis_job
union select
  job_no,
  task_id, 
  status_id, 
  date_submitted, 
  date_completed, 
  user_id, 
  isIndexed, 
  access_id, 
  job_name, 
  lsid, 
  task_lsid,
  task_name,
  parent,
  deleted
from analysis_job_archive;