-- add tables for Group Permissions.
-- Permission Flags
CREATE TABLE PERMISSION_FLAG
(
  ID INTEGER PRIMARY KEY,
  NAME text NOT NULL
)
;
--Populate Permission Flags
INSERT INTO PERMISSION_FLAG (ID, NAME) VALUES ('1', 'READ_WRITE');
INSERT INTO PERMISSION_FLAG (ID, NAME) VALUES ('2', 'READ');

-- Table for storing group permissions per job
CREATE TABLE JOB_GROUP
(
  JOB_NO BIGINT NOT NULL references ANALYSIS_JOB(JOB_NO),
  GROUP_ID text NOT NULL,
  PERMISSION_FLAG INTEGER NOT NULL references PERMISSION_FLAG(ID)
, PRIMARY KEY
  (
    JOB_NO,
    GROUP_ID
  )
)
;

COMMIT;














