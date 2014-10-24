create table eula_record (
    id bigint not null auto_increment,
    user_id varchar(255) not null,
    lsid varchar(255) not null,
    date_recorded timestamp not null default now(),
    primary key (id),
    unique (user_id, lsid));

-- update schema version
insert into PROPS (`KEY`, VALUE) VALUES ('registeredVersion3.4.2', '3.4.2');
commit;
