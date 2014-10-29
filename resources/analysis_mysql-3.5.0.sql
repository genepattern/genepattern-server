--
-- the 'eula_remote_queue' table stores records for all EULA which 
--     are POSTed to a particular remote server
--     the 'recorded' field is set to true (aka 1) after the POST has succeeded.
--
create table EULA_REMOTE_QUEUE (
    eula_record_id bigint not null references eula_record(id),
    remote_url varchar(255) not null,
    recorded bit default 0 not null,
    num_attempts integer default 0 not null,
    date_recorded timestamp not null default now(),
    primary key (eula_record_id, remote_url));

-- update schema version
insert into PROPS (`KEY`, VALUE) VALUES ('registeredVersion3.5.0', '3.5.0');
commit;

