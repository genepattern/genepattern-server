create table PIN_MODULE (
    id serial primary key,
    username TEXT,
    lsid TEXT,
    pin_position double precision
);

-- update schema version
UPDATE PROPS SET VALUE = '3.7.5' where KEY = 'schemaVersion';

commit;