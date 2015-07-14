create table PIN_MODULE (
    id integer not null auto_increment unique,
    username varchar(255),
    lsid varchar(255),
    pin_position double precision,
    primary key (id));

commit;
