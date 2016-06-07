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
  STATUS_NAME VARCHAR(20),
  PRIMARY KEY (STATUS_ID)
);


/* TASK ACCESS CODE TABLE */
CREATE TABLE TASK_ACCESS
(
  ACCESS_ID integer not null unique,
  NAME         VARCHAR(4000),
  DESCRIPTION  VARCHAR(4000),
  PRIMARY KEY (ACCESS_ID)
);


/* TASK MASTER TABLE */
create table TASK_MASTER (
  TASK_ID integer not null auto_increment unique,
  TASK_NAME varchar(511),
  DESCRIPTION varchar(4000),
  TYPE_ID integer,
  USER_ID varchar(511),
  ACCESS_ID integer,
  LSID varchar(511),
  ISINDEXED bit DEFAULT 0 NOT NULL,
  TASKINFOATTRIBUTES text,
  PARAMETER_INFO text,
  PRIMARY KEY(TASK_ID)
 );

CREATE INDEX IDX_TASK_MASTER_USER_ID ON TASK_MASTER (USER_ID);
CREATE INDEX IDX_TASK_MASTER_ACCESS_ID ON TASK_MASTER (ACCESS_ID);
CREATE INDEX IDX_TASK_MASTER_TASK_NAME ON TASK_MASTER (TASK_NAME);
CREATE INDEX IDX_TASK_MASTER_LSID ON TASK_MASTER (LSID);


/* ANALYSIS JOB */
create table ANALYSIS_JOB (
    JOB_NO integer not null auto_increment unique,
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
    PARAMETER_INFO TEXT,
    primary key (JOB_NO));



/* SUITEs  */
CREATE TABLE SUITE
(
  LSID VARCHAR(511),
  NAME VARCHAR(255),
  AUTHOR VARCHAR(255),
  OWNER VARCHAR(255),
  DESCRIPTION VARCHAR(4000),
  ACCESS_ID NUMERIC(10,0),
  USER_ID VARCHAR(255),
  PRIMARY KEY (LSID)
);

CREATE TABLE SUITE_MODULES
(
  LSID VARCHAR(255),
  MODULE_LSID VARCHAR(255)
);



/* LSIDS */
CREATE TABLE LSIDS
(
  LSID VARCHAR(255) NOT NULL,
  LSID_NO_VERSION VARCHAR(255),
  LSID_VERSION VARCHAR(255)
);

CREATE INDEX IDX_LSID_VERSION ON LSIDS (LSID_VERSION);

CREATE INDEX IDX_LSID_NO_VERSION ON LSIDS (LSID_NO_VERSION);

/* SEQUENCE TABLES FOR LSIDS */
CREATE TABLE SEQUENCE_TABLE (
    ID INTEGER not null auto_increment,
    NAME varchar(100) not null unique,
    NEXT_VALUE integer not null,
    primary key (ID));

insert into SEQUENCE_TABLE (NAME, NEXT_VALUE) values('lsid_identifier_seq', 1);
insert into SEQUENCE_TABLE (NAME, NEXT_VALUE) values('lsid_suite_identifier_seq', 1);

/* "KEY" is a reserved word in MySQL */
CREATE TABLE PROPS
(
  `KEY` VARCHAR(255) NOT NULL UNIQUE,
  VALUE VARCHAR(255),
  PRIMARY KEY (`KEY`)
);


/* USER TABLE */

CREATE TABLE GP_USER
(
  USER_ID            VARCHAR(255),
  GP_PASSWORD        TINYBLOB,
  EMAIL              VARCHAR(255),
  LAST_LOGIN_DATE    TIMESTAMP NULL,
  REGISTRATION_DATE    TIMESTAMP NULL,
  LAST_LOGIN_IP      VARCHAR(255),
  TOTAL_LOGIN_COUNT  INTEGER  DEFAULT 0  NOT NULL,
  PRIMARY KEY (USER_ID)
);

CREATE TABLE GP_USER_PROP
(
  ID integer not null auto_increment,
  `KEY` VARCHAR(255),
  VALUE VARCHAR(255),
  GP_USER_ID VARCHAR(255) NOT NULL,
  PRIMARY KEY (ID)
);
CREATE INDEX IDX_GP_USER_PROP_KEY ON GP_USER_PROP (`KEY`);

/* Event logging */
create table JOB_COMPLETION_EVENT
(
  ID integer not null auto_increment,
  user_id varchar(255),
  type varchar(255),
  job_number integer,
  parent_job_number integer,
  task_lsid varchar(255),
  task_name varchar(255),
  completion_status varchar(255),
  completion_date timestamp null,
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
COMMIT;

