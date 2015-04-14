drop table if exists PERMISSION_FLAG;
drop table if exists JOB_GROUP;

-- add tables for Group Permissions.
-- Permission Flags
CREATE TABLE PERMISSION_FLAG
(
  ID INTEGER NOT NULL,
  NAME VARCHAR(32) NOT NULL,
  primary key (id));

-- Populate Permission Flags
INSERT INTO PERMISSION_FLAG (ID, NAME) VALUES ('1', 'READ_WRITE');
INSERT INTO PERMISSION_FLAG (ID, NAME) VALUES ('2', 'READ');

-- Table for storing group permissions per job
CREATE TABLE JOB_GROUP
(
  JOB_NO integer NOT NULL references ANALYSIS_JOB(JOB_NO),
  GROUP_ID VARCHAR(255) NOT NULL,
  PERMISSION_FLAG integer NOT NULL references PERMISSION_FLAG(ID),
  primary key ( JOB_NO, GROUP_ID ));

-- update schema version
UPDATE PROPS SET VALUE = '3.2.0' where `KEY` = 'schemaVersion';

COMMIT;














