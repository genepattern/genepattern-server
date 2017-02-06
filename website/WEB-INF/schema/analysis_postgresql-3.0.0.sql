drop table if exists JOB_STATUS;
drop table if exists TASK_ACCESS;
drop table if exists TASK_MASTER;
drop table if exists ANALYSIS_JOB;
drop table if exists SUITE;
drop table if exists SUITE_MODULES;
drop table if exists LSIDS;
drop table if exists SEQUENCE_TABLE;
drop table if exists PROPS;
drop table if exists GP_USER_PROP;
drop table if exists GP_USER;
drop table if exists JOB_COMPLETION_EVENT;

/* JOB STATUS CODE TABLE */
CREATE TABLE JOB_STATUS
(
  STATUS_ID INTEGER NOT NULL UNIQUE,
  STATUS_NAME TEXT,
  PRIMARY KEY (STATUS_ID)
);


/* TASK ACCESS CODE TABLE */
CREATE TABLE TASK_ACCESS
(
  ACCESS_ID integer not null unique,
  NAME         TEXT,
  DESCRIPTION  TEXT,
  PRIMARY KEY (ACCESS_ID)
);


/* TASK MASTER TABLE */
create table TASK_MASTER (
  TASK_ID serial,
  TASK_NAME TEXT,
  DESCRIPTION TEXT,
  TYPE_ID integer,
  USER_ID TEXT,
  ACCESS_ID integer,
  LSID TEXT,
  ISINDEXED boolean DEFAULT false NOT NULL,
  TASKINFOATTRIBUTES text,
  PARAMETER_INFO text,
  PRIMARY KEY(TASK_ID)
 );

CREATE INDEX IDX_TASK_MASTER_USER_ID ON TASK_MASTER (USER_ID);
CREATE INDEX IDX_TASK_MASTER_ACCESS_ID ON TASK_MASTER (ACCESS_ID);
CREATE INDEX IDX_TASK_MASTER_TASK_NAME ON TASK_MASTER (TASK_NAME);
CREATE INDEX IDX_TASK_MASTER_LSID ON TASK_MASTER (LSID);


-- Create all sequences required from the first (3.0.0) release through to GP 3.9.2 
-- 
-- Example 'hibernate mapping' sequence required by the default PostgreSQLDialect
-- We don't need this because we use a CustomPostgreSQLDialect
-- 
-- CREATE SEQUENCE hibernate_sequence 
--   INCREMENT 1 
--   MINVALUE 1 
--  MAXVALUE 9223372036854775807 
--  START 1 
--  CACHE 5; 
--

CREATE SEQUENCE TASK_MASTER_SEQ
  START WITH 1
  ;

CREATE SEQUENCE ANALYSIS_JOB_SEQ
  START WITH 1
  ;

CREATE SEQUENCE GP_USER_PROP_SEQ
  START WITH 1
  ;

CREATE SEQUENCE job_completion_event_SEQ
  START WITH 1
  ;

CREATE SEQUENCE lsid_suite_identifier_seq
  START WITH 100
  ;

CREATE SEQUENCE lsid_identifier_seq
  START WITH 100
  ;

CREATE SEQUENCE SYSTEM_MESSAGE_SEQ
  START WITH 1
  ;

CREATE SEQUENCE BATCH_JOB_SEQ
  START WITH 1
  ;

CREATE SEQUENCE user_upload_SEQ;
CREATE SEQUENCE eula_record_SEQ;
CREATE SEQUENCE PIN_MODULE_SEQ
  START WITH 1
  ;

CREATE SEQUENCE queue_congestion_SEQ;

CREATE SEQUENCE JOB_COMMENT_SEQ
  START WITH 1
  ;

CREATE SEQUENCE TAG_SEQ
  START WITH 1
  ;

CREATE SEQUENCE JOB_TAG_SEQ
  START WITH 1
  ;

create sequence patch_info_SEQ;

/* ANALYSIS JOB */
create table ANALYSIS_JOB (
    JOB_NO bigserial,
    TASK_ID integer,
    STATUS_ID integer,
    DATE_SUBMITTED timestamp,
    DATE_COMPLETED timestamp,
    USER_ID TEXT,
    ISINDEXED integer,
    ACCESS_ID integer,
    JOB_NAME TEXT,
    LSID TEXT,
    TASK_LSID TEXT,
    TASK_NAME TEXT,
    PARENT integer,
    DELETED boolean not null,
    PARAMETER_INFO TEXT,
    primary key (JOB_NO));



/* SUITEs  */
CREATE TABLE SUITE
(
  LSID TEXT,
  NAME text,
  AUTHOR text,
  OWNER text,
  DESCRIPTION TEXT,
  ACCESS_ID NUMERIC(10,0),
  USER_ID text,
  PRIMARY KEY (LSID)
);

CREATE TABLE SUITE_MODULES
(
  LSID text,
  MODULE_LSID text
);



/* LSIDS */
CREATE TABLE LSIDS
(
  LSID text NOT NULL,
  LSID_NO_VERSION text,
  LSID_VERSION text
);

CREATE INDEX IDX_LSID_VERSION ON LSIDS (LSID_VERSION);

CREATE INDEX IDX_LSID_NO_VERSION ON LSIDS (LSID_NO_VERSION);

/* SEQUENCE TABLES FOR LSIDS */
CREATE TABLE SEQUENCE_TABLE (
    ID serial,
    NAME text not null unique,
    NEXT_VALUE integer not null,
    primary key (ID));

insert into SEQUENCE_TABLE (NAME, NEXT_VALUE) values('lsid_identifier_seq', 1);
insert into SEQUENCE_TABLE (NAME, NEXT_VALUE) values('lsid_suite_identifier_seq', 1);

/* PROPERTY TABLE */
CREATE TABLE PROPS
(
  -- "KEY" is a reserved word in MySQL
  "KEY" text NOT NULL UNIQUE,
  VALUE text
);


/* USER TABLE */

CREATE TABLE GP_USER
(
  USER_ID            text,
  GP_PASSWORD        text,
  EMAIL              text,
  LAST_LOGIN_DATE    TIMESTAMP,
  REGISTRATION_DATE    TIMESTAMP,
  LAST_LOGIN_IP      text,
  TOTAL_LOGIN_COUNT  INTEGER  DEFAULT 0  NOT NULL,
  PRIMARY KEY (USER_ID)
);

CREATE TABLE GP_USER_PROP
(
  ID serial,
  "KEY" text,
  VALUE text,
  GP_USER_ID text NOT NULL,
  PRIMARY KEY (ID)
);
CREATE INDEX IDX_GP_USER_PROP_KEY ON GP_USER_PROP ("KEY");

/* Event logging */
create table JOB_COMPLETION_EVENT
(
  ID serial,
  user_id text,
  type text,
  job_number integer,
  parent_job_number integer,
  task_lsid text,
  task_name text,
  completion_status text,
  completion_date timestamp,
  elapsed_time integer,
  primary key (id)
);

/* Constraints */

ALTER TABLE GP_USER_PROP
 ADD FOREIGN KEY (GP_USER_ID)
 REFERENCES GP_USER (USER_ID);

/* Data */

INSERT INTO JOB_STATUS VALUES(1,'Pending');
INSERT INTO JOB_STATUS VALUES(2,'Processing');
INSERT INTO JOB_STATUS VALUES(3,'Finished');
INSERT INTO JOB_STATUS VALUES(4,'Error');

INSERT INTO TASK_ACCESS VALUES(1,'public','public access');
INSERT INTO TASK_ACCESS VALUES(2,'private','access only for the owner');

-- this is now taken care of by the GP server, if you run these scripts by hand
-- you must set the correct schemaVersion in the DB
-- INSERT INTO PROPS (KEY, VALUE) VALUES ('schemaVersion', '3.0.0');

COMMIT;
