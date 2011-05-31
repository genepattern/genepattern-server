-- record of user upload files
create table GS_ACCOUNT (
    -- use the File.canonicalPath as the primary key
    GP_USERID varchar primary key,
    -- owner of the file
    TOKEN varchar (255),
constraint gsa_fk foreign key (GP_USERID) references GP_USER(USER_ID)
);

-- update schema version
update props set value='3.3.3' where key='schemaVersion';
