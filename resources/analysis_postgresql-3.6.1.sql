--
-- create the task_install_record table
--
create table task_install (
    lsid text primary key,
    user_id text,
    date_installed timestamp,
    source_type text,
    repo_url text,
    prev_lsid text,
    zipfile text,
    libdir text
);

-- update schema version
UPDATE PROPS SET VALUE = '3.6.1' where KEY = 'schemaVersion';

commit;

