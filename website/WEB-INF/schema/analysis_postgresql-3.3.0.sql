CREATE TABLE BATCH_JOB (
    JOB_NO BIGSERIAL PRIMARY KEY,
    USER_ID text references GP_USER ( USER_ID ),
    DELETED BOOLEAN DEFAULT FALSE NOT NULL,
    DATE_SUBMITTED TIMESTAMP
);

CREATE TABLE BATCH_ANALYSIS (
    BATCH_JOB BIGINT NOT NULL references BATCH_JOB(JOB_NO) on delete cascade,
    ANALYSIS_JOB BIGINT NOT NULL  references ANALYSIS_JOB(JOB_NO) on delete cascade
);

-- update schema version                                                                                                                                                                                                                        
UPDATE PROPS SET VALUE = '3.3.0' where KEY = 'schemaVersion';

COMMIT;
