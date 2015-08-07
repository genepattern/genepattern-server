
-- record of user upload files
create table UPLOAD_FILE (
    -- use the File.canonicalPath as the primary key
    PATH TEXT PRIMARY KEY,
    -- owner of the file
    USER_ID text not null references GP_USER(USER_ID),
    NAME text,
    STATUS SMALLINT DEFAULT 1 NOT NULL,
    EXTENSION text,
    KIND text, -- from SemanticUtil.getKind, usually the extension
    FILE_LENGTH DECIMAL(20,0), -- the length in bytes of the file
    LAST_MODIFIED timestamp -- the last modified date of the file
);

CREATE INDEX IDX_UF_USER_ID ON UPLOAD_FILE (USER_ID);

COMMIT;









