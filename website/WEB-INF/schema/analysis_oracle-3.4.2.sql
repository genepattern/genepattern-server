--
create table eula_record (
    id number(19,0) not null,
    lsid varchar2(1024 char) not null,
    user_id varchar2(512 char) not null,
    date_recorded timestamp default sysdate not null,
    primary key (id),
    constraint user_id_fk foreign key (user_id) references GP_USER(USER_ID),
    unique (user_id, lsid) 
);
create sequence eula_record_SEQ;
create index idx_eula_record_lsid on eula_record (lsid);
create index idx_eula_record_user_id on eula_record (user_id); 

-- update schema version
UPDATE PROPS SET VALUE = '3.4.2' where KEY = 'schemaVersion';

commit;
