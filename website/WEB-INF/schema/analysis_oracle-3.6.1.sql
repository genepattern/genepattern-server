--
-- create the task_install_record table
--
create table task_install (
    lsid varchar2(512) not null,
    user_id varchar2(255),
    date_installed timestamp,
    source_type varchar2(255),
    repo_url varchar2(255),
    prev_lsid varchar2(512),
    zipfile varchar2(255),
    libdir varchar2(255),
    primary key (lsid)
);


commit;

