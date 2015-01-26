-- create index on the job_group table to speed up jdbc queries
CREATE INDEX IDX_JOB_GROUP_JOB_NO ON JOB_GROUP (JOB_NO);

INSERT INTO JOB_STATUS VALUES(5,'Dispatching');
INSERT INTO JOB_STATUS VALUES(0,'Waiting');

-- create job_dependency table
--create table WAITING_JOB (
--  job_no NUMBER(10,0) NOT NULL,
--  wait_for_job_no NUMBER(10,0) NOT NULL, 
  
--  CONSTRAINT job_dependency_u
--      UNIQUE(job_no, wait_for_job_no),
--  CONSTRAINT job_dependency_fk01
--      FOREIGN KEY (job_no)
--      REFERENCES analysis_job (job_no)
--      ON DELETE CASCADE,
--    CONSTRAINT job_dependency_fk02
--      FOREIGN KEY (wait_for_job_no)
--      REFERENCES analysis_job (job_no)
--      ON DELETE CASCADE
--);

-- update schema version
UPDATE PROPS SET VALUE = '3.2.4' where KEY = 'schemaVersion';

COMMIT;









