-- add table(s) for System Alert Messages.

CREATE TABLE SYSTEM_MESSAGE (
    id              serial,
    message         TEXT NOT NULL,
    start_time      TIMESTAMP DEFAULT current_timestamp NOT NULL,
    end_time        TIMESTAMP NULL,
    deleteOnRestart BOOLEAN  NOT NULL,
    PRIMARY KEY (id)
    );

-- COMMIT;














