-- record of user upload files
create table UPLOAD_FILE (
    -- use the File.canonicalPath as the primary key
    PATH longvarchar primary key,
    -- owner of the file
    USER_ID varchar (255) not null references GP_USER(USER_ID),
    NAME varchar (255),
    STATUS INTEGER DEFAULT 1,
    EXTENSION varchar (32),
    KIND varchar (32), -- from SemanticUtil.getKind, usually the extension
    FILE_LENGTH bigint, -- the length in bytes of the file
    LAST_MODIFIED timestamp -- the last modified date of the file
);
