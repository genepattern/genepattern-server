create table  job_output (
    gp_job_no bigint not null references ANALYSIS_JOB(JOB_NO) on delete cascade,
    path text not null,
    file_length decimal(19,0),
    last_modified timestamp,
    extension text,
    kind text,
    gpFileType text,
    hidden boolean default false not null,
    deleted boolean default false not null,
    primary key (gp_job_no, path)
);

create table queue_congestion (
    id serial primary key,
    queuetime decimal(19,0),
    queue text,
    unique (queue)
);

-- update schema version
-- UPDATE PROPS SET VALUE = '3.9.0' where KEY = 'schemaVersion';

commit;

