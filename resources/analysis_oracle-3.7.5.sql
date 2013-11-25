create table PIN_MODULE (
    id integer NOT NULL,
    "user" varchar(511),
    lsid varchar(511),
    "index" binary_double,
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
update props set value='3.7.5' where key='schemaVersion';

