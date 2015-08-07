-- add table(s) for System Alert Messages.

create table SYSTEM_MESSAGE (
    id bigint not null auto_increment unique,
    message varchar(4000),
    start_time timestamp not null default now(),
    end_time timestamp,
    deleteOnRestart bit,
    primary key (id));

COMMIT;














