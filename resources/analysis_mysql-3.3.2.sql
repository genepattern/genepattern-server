-- record of user upload files
create table UPLOAD_FILE (
    -- use the File.canonicalPath as the primary key
    PATH varchar(4000),
    -- owner of the file
    USER_ID varchar (255) not null references GP_USER(USER_ID),
    NAME varchar (255),
    STATUS integer DEFAULT 1 NOT NULL,
    EXTENSION varchar (16),
    KIND varchar (32), -- from SemanticUtil.getKind, usually the extension
    FILE_LENGTH bigint unsigned, -- the length in bytes of the file
    LAST_MODIFIED timestamp -- the last modified date of the file
    -- ,  primary key (PATH)
);

-- update schema version
INSERT INTO PROPS (`KEY`, VALUE) VALUES ('registeredVersion3.3.2', '3.3.2');

COMMIT;









