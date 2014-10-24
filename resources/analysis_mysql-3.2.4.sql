-- create index on the job_group table to speed up jdbc queries
CREATE INDEX IDX_JOB_GROUP_JOB_NO ON JOB_GROUP (JOB_NO);

INSERT INTO JOB_STATUS VALUES(5,'Dispatching');
INSERT INTO JOB_STATUS VALUES(0,'Waiting');

-- update schema version
INSERT INTO PROPS (`KEY`, VALUE) VALUES ('registeredVersion3.2.4', '3.2.4');

COMMIT;









