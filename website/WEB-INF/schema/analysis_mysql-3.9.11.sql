--
-- create ANALYSIS_JOB_ARCHIVE and JOB_RUNNER_JOB_ARCHIVE tables and related trigger for stats reporting
-- 
--   Copy each deleted row from the analysis_job table into the analysis_job_archive table
--   Copy each deleted row from the job_runner_job table into the job_runner_job_archive table
--
-- Notes: for debugging ...
--
-- to select all triggers
-- SELECT TRIGGER_SCHEMA, TRIGGER_NAME, EVENT_MANIPULATION, EVENT_OBJECT_TABLE, ACTION_STATEMENT
--     FROM INFORMATION_SCHEMA.TRIGGERS
--
-- to delete the trigger
-- DROP TRIGGER ON_ANALYSIS_JOB_DEL

create table ANALYSIS_JOB_ARCHIVE (
    JOB_NO integer not null unique,
    TASK_ID integer,
    STATUS_ID integer,
    DATE_SUBMITTED timestamp null,
    DATE_COMPLETED timestamp null,
    USER_ID varchar(511),
    ISINDEXED integer,
    ACCESS_ID integer,
    JOB_NAME varchar(511),
    LSID varchar(511),
    TASK_LSID varchar(511),
    TASK_NAME varchar(511),
    PARENT integer,
    DELETED bit not null,
    PARAMETER_INFO mediumtext
);

create table JOB_RUNNER_JOB_ARCHIVE (
  `gp_job_no` int(11) NOT NULL,
  `lsid` varchar(255) DEFAULT NULL,
  `jr_classname` varchar(255) NOT NULL,
  `jr_name` varchar(255) NOT NULL,
  `ext_job_id` varchar(255) DEFAULT NULL,
  `job_state` varchar(255) DEFAULT NULL,
  `status_date` timestamp NOT NULL,
  `status_message` varchar(255) DEFAULT NULL,
  `exit_code` int(11) DEFAULT NULL,
  `terminating_signal` varchar(255) DEFAULT NULL,
  `req_mem` bigint(20) unsigned DEFAULT NULL,
  `req_cpu_count` int(11) DEFAULT NULL,
  `req_node_count` int(11) DEFAULT NULL,
  `req_walltime` varchar(15) DEFAULT NULL,
  `req_queue` varchar(255) DEFAULT NULL,
  `working_dir` varchar(255) NOT NULL,
  `stdin_file` varchar(255) DEFAULT NULL,
  `stdout_file` varchar(255) DEFAULT NULL,
  `log_file` varchar(255) DEFAULT NULL,
  `stderr_file` varchar(255) DEFAULT NULL,
  `queue_id` varchar(511) DEFAULT NULL,
  `submit_time` timestamp NULL DEFAULT NULL,
  `start_time` timestamp NULL DEFAULT NULL,
  `end_time` timestamp NULL DEFAULT NULL,
  `cpu_time` bigint(20) unsigned DEFAULT NULL,
  `max_mem` bigint(20) unsigned DEFAULT NULL,
  `max_swap` bigint(20) unsigned DEFAULT NULL,
  `max_processes` int(11) DEFAULT NULL,
  `max_threads` int(11) DEFAULT NULL
);

-- Note:
-- In MySQL 5.6, all triggers are FOR EACH ROW; 
--   that is, the trigger is activated for each row that is inserted, updated, or deleted. 
--   MySQL 5.6 does not support triggers using FOR EACH STATEMENT.

-- DROP TRIGGER [IF EXISTS] [schema_name.]trigger_name
DROP TRIGGER IF EXISTS ON_ANALYSIS_JOB_DEL;

--
-- initial implementation
--
-- CREATE TRIGGER ON_ANALYSIS_JOB_DEL
-- BEFORE DELETE ON ANALYSIS_JOB
-- FOR EACH ROW
--   INSERT INTO ANALYSIS_JOB_ARCHIVE VALUES (
--     OLD.JOB_NO, 
--     OLD.TASK_ID, 
--     OLD.STATUS_ID, 
--     OLD.DATE_SUBMITTED,
--     OLD.DATE_COMPLETED, 
--     OLD.USER_ID, 
--     OLD.ISINDEXED, 
--     OLD.ACCESS_ID,
--     OLD.JOB_NAME, 
--     OLD.LSID, 
--     OLD.TASK_LSID, 
--     OLD.TASK_NAME, 
--     OLD.PARENT,
--     OLD.DELETED, 
--     OLD.PARAMETER_INFO 
--   )
-- ;

CREATE TRIGGER ON_ANALYSIS_JOB_DEL
BEFORE DELETE ON ANALYSIS_JOB
FOR EACH ROW
BEGIN
  INSERT INTO ANALYSIS_JOB_ARCHIVE VALUES (
    OLD.JOB_NO, 
    OLD.TASK_ID, 
    OLD.STATUS_ID, 
    OLD.DATE_SUBMITTED,
    OLD.DATE_COMPLETED, 
    OLD.USER_ID, 
    OLD.ISINDEXED, 
    OLD.ACCESS_ID,
    OLD.JOB_NAME, 
    OLD.LSID, 
    OLD.TASK_LSID, 
    OLD.TASK_NAME, 
    OLD.PARENT,
    OLD.DELETED, 
    OLD.PARAMETER_INFO 
  )\;
  INSERT into JOB_RUNNER_JOB_ARCHIVE (
    `gp_job_no`,
    `lsid`, 
    `jr_classname`, 
    `jr_name`, 
    `ext_job_id`, 
    `job_state`, 
    `status_date`, 
    `status_message`, 
    `exit_code`, 
    `terminating_signal`, 
    `req_mem`, 
    `req_cpu_count`, 
    `req_node_count`, 
    `req_walltime`, 
    `req_queue`, 
    `working_dir`, 
    `stdin_file`, 
    `stdout_file`, 
    `log_file`, 
    `stderr_file`, 
    `queue_id`, 
    `submit_time`, 
    `start_time`, 
    `end_time`, 
    `cpu_time`, 
    `max_mem`, 
    `max_swap`, 
    `max_processes`, 
    `max_threads`
  ) 
  select 
    `gp_job_no`,
    `lsid`, 
    `jr_classname`, 
    `jr_name`, 
    `ext_job_id`, 
    `job_state`, 
    `status_date`, 
    `status_message`, 
    `exit_code`, 
    `terminating_signal`, 
    `req_mem`, 
    `req_cpu_count`, 
    `req_node_count`, 
    `req_walltime`, 
    `req_queue`, 
    `working_dir`, 
    `stdin_file`, 
    `stdout_file`, 
    `log_file`, 
    `stderr_file`, 
    `queue_id`, 
    `submit_time`, 
    `start_time`, 
    `end_time`, 
    `cpu_time`, 
    `max_mem`, 
    `max_swap`, 
    `max_processes`, 
    `max_threads`
  from JOB_RUNNER_JOB where `gp_job_no` = OLD.job_no\;
END;

