--
-- create ANALYSIS_JOB_ARCHIVE table and related trigger for stats reporting
-- 
--   Copy each deleted row from the analysis_job table into the analysis_job_archive table
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
    PARAMETER_INFO TEXT
);

-- Note:
-- In MySQL 5.6, all triggers are FOR EACH ROW; 
--   that is, the trigger is activated for each row that is inserted, updated, or deleted. 
--   MySQL 5.6 does not support triggers using FOR EACH STATEMENT.

-- DROP TRIGGER [IF EXISTS] [schema_name.]trigger_name
DROP TRIGGER IF EXISTS ON_ANALYSIS_JOB_DEL;

CREATE TRIGGER ON_ANALYSIS_JOB_DEL
BEFORE DELETE ON ANALYSIS_JOB
FOR EACH ROW
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
  )
;


