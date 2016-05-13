-- drop unused tables from DB
-- the 'UPLOAD_FILE' table was added in 3.3.2, but is no longer needed
drop table if exists UPLOAD_FILE;
