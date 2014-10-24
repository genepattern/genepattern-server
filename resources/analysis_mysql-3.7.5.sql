create table PIN_MODULE (
    id integer not null auto_increment unique,
    username varchar(255),
    lsid varchar(255),
    pin_position double precision,
    primary key (id));

-- update schema version
insert into PROPS (`KEY`, VALUE) VALUES ('registeredVersion3.7.5', '3.7.5');

commit;
