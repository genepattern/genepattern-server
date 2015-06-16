--
-- the 'eula_remote_queue' table stores records for all EULA which 
--     are POSTed to a particular remote server
--     the 'recorded' field is set to true (aka 1) after the POST has succeeded.
--
create table eula_remote_queue (
    eula_record_id bigint not null,
    remote_url varchar(255) not null,
    recorded boolean default false not null,
    num_attempts integer default 0 not null,
    date_recorded timestamp default now not null,
    primary key (eula_record_id, remote_url),
    unique (eula_record_id, remote_url),
    constraint erq_fk FOREIGN KEY (eula_record_id) references eula_record(id)
);

create index idx_eula_remote_queue_url on eula_remote_queue (remote_url);

-- update schema version
update props set value='3.5.0' where key='schemaVersion';

