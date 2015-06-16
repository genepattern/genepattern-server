create table category (
    category_id integer primary key,
    name varchar(255) unique not null
);
create sequence category_SEQ;

-- the mapping table which maps task_install to category 
create table task_install_category (
    lsid varchar(512) not null references task_install(lsid) on delete cascade,
    category_id integer not null references category(category_id) on delete cascade
);

-- update schema version
UPDATE PROPS SET VALUE = '3.9.3' where KEY = 'schemaVersion';
commit;
