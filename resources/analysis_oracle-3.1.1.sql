-- add table(s) for System Alert Messages.

create table SYSTEM_MESSAGE (
    id INTEGER NOT NULL,
    message VARCHAR2(4000 CHAR), not null,
    start_time TIMESTAMP default now not null,
    end_time TIMESTAMP null,
    deleteOnRestart INTEGER(1) default 0 not null,
    PRIMARY KEY (id)
) tablespace GPPORTAL;

-- update schema version
UPDATE GPPORTAL.PROPS SET VALUE = '3.1.1' where KEY = 'schemaVersion';

COMMIT;














