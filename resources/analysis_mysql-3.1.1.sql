-- add table(s) for System Alert Messages.

create table GENEPATTERN.SYSTEM_MESSAGE (
    id bigint not null auto_increment unique,
    message varchar(4000),
    start_time timestamp not null default now(),
    end_time timestamp,
    deleteOnRestart bit,
    primary key (id));

-- update schema version
INSERT INTO PROPS (`KEY`,VALUE) values ('registeredVersion3.1.1', '3.1.1');

COMMIT;














