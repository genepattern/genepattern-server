-- add table(s) for System Alert Messages.

create table SYSTEM_MESSAGE (
    id int identity primary key,
    message LONGVARCHAR not null,
    start_time TIMESTAMP default now not null,
    end_time TIMESTAMP null,
    deleteOnRestart BIT default 0 not null
);
