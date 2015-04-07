--
create table eula_record (
    id bigserial primary key,
    lsid text not null,
    user_id text,
    date_recorded timestamp default current_timestamp not null,
    unique (user_id, lsid)
);

create index idx_eula_record_lsid on eula_record (lsid);
create index idx_eula_record_user_id on eula_record (user_id); 

-- update schema version
UPDATE PROPS SET VALUE = '3.4.2' where KEY = 'schemaVersion';

commit;
