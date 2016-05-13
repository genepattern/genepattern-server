-- drop unused tables from DB
-- the 'upload_file' table was added in 3.3.2, but is no longer needed
drop table if exists upload_file;
