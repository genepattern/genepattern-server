-- record of user upload files
create table UPLOAD_FILE (
    -- use the File.canonicalPath as the primary key
    PATH varchar primary key,
    -- owner of the file
    USER_ID varchar (255) not null,
    NAME varchar,
    STATUS INTEGER DEFAULT 1,
    EXTENSION varchar,
    KIND varchar, -- from SemanticUtil.getKind, usually the extension
    FILE_LENGTH bigint, -- the length in bytes of the file
    LAST_MODIFIED timestamp, -- the last modified date of the file
constraint uf_pk primary key (PATH),
constraint uf_fk foreign key (USER_ID) references GP_USER(USER_ID)
);

-- update schema version
update props set value='3.3.2' where key='schemaVersion';
