--
-- create the task_install_record table
--
create table task_install (
    lsid varchar(512) not null,
    user_id varchar(255),
    date_installed timestamp,
    source_type varchar(255),
    repo_url varchar(255),
    prev_lsid varchar(512),
    zipfile varchar(255),
    libdir varchar(255),
    primary key (lsid)
);

