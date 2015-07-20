create table CATEGORY (
    category_id integer not null primary key auto_increment,
    name varchar(255) unique not null
);


-- the mapping table which maps task_install to category 
create table TASK_INSTALL_CATEGORY (
    lsid varchar(512) not null references TASK_INSTALL(lsid) on delete cascade,
    category_id integer not null references CATEGORY(category_id) on delete cascade
);

