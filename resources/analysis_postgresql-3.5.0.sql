--
-- the 'eula_remote_queue' table stores records for all EULA which 
--     are POSTed to a particular remote server
--     the 'recorded' field is set to true (aka 1) after the POST has succeeded.
--
create table eula_remote_queue (
    eula_record_id bigint not null references eula_record(id) on delete cascade,
    remote_url text not null,
    recorded boolean  default false not null,
    num_attempts double precision default 0 not null,
    date_recorded timestamp default current_timestamp not null,
    primary key (eula_record_id, remote_url)
);

-- update schema version
UPDATE PROPS SET VALUE = '3.5.0' where KEY = 'schemaVersion';
commit;

