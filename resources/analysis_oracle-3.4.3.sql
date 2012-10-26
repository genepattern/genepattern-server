--
-- the 'eula_remote_queue' table stores records for all EULA which 
--     are POSTed to a particular remote server
--     the 'recorded' field is set to true (aka 1) after the POST has succeeded.
--
create table eula_remote_queue (
    eula_record_id number(19,0) not null,
    remote_url varchar2(1024 char) not null,
    recorded number (1,0) default 0 not null,
    num_attempts number default 0 not null,
    date_recorded timestamp default sysdate not null,
    primary key (eula_record_id),
    unique (eula_record_id, remote_url),
    constraint erq_fk FOREIGN KEY (eula_record_id) references eula_record(id)
);

create index idx_eula_remote_queue_url on eula_remote_queue (remote_url);

-- update schema version
insert into PROPS (KEY, VALUE) VALUES ('registeredVersion3.4.3', '3.4.3');
commit;

