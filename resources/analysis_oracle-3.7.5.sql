create table PIN_MODULE (
    id integer NOT NULL,
    username varchar(511),
    lsid varchar(511),
    pin_position binary_double,
    primary key (id)
);

CREATE SEQUENCE PIN_MODULE_SEQ
  START WITH 1
  MAXVALUE 999999999999999999999999999
  MINVALUE 0
  NOCYCLE
  NOCACHE
  NOORDER;


-- update schema version
UPDATE PROPS SET VALUE = '3.7.5' where KEY = 'schemaVersion';

commit;