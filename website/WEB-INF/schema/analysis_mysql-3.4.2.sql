create table EULA_RECORD (
    id bigint not null auto_increment,
    user_id varchar(255) not null,
    lsid varchar(255) not null,
    date_recorded timestamp not null default now(),
    primary key (id),
    unique (user_id, lsid));

commit;
