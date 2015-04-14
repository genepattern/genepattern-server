-- record of user upload files 
create table USER_UPLOAD (
    id bigint not null auto_increment,
    user_id varchar(255),
    path varchar(4000),
    name varchar(512),
    last_modified datetime,
    file_length bigint unsigned,
    extension varchar(255),
    kind varchar(255),
    num_parts integer,
    num_parts_recd integer,
    primary key (id)
    -- ,
    -- Can't enfore uniqueness constraint with large column
    -- unique (user_id, path)
    );

-- for GenomeSpace integration, link GP user account to GS user account
create table GS_ACCOUNT (
    -- use the File.canonicalPath as the primary key
    GP_USERID varchar(255) not null unique references GP_USER(USER_ID),
    -- owner of the file
    TOKEN varchar(255)
);

-- improve performance by creating indexes on the ANALYSIS_JOB table
CREATE INDEX IDX_AJ_STATUS ON ANALYSIS_JOB(STATUS_ID);
CREATE INDEX IDX_AJ_PARENT ON ANALYSIS_JOB(PARENT);

-- for SGE integration
create table JOB_SGE (
    GP_JOB_NO integer not null,
    SGE_JOB_ID varchar(255),
    SGE_SUBMIT_TIME timestamp,
    SGE_START_TIME timestamp,
    SGE_END_TIME timestamp,
    SGE_RETURN_CODE integer,
    SGE_JOB_COMPLETION_STATUS varchar(511),
    primary key (GP_JOB_NO));

CREATE INDEX IDX_SGE_JOB_ID on JOB_SGE (SGE_JOB_ID);

-- update schema version
UPDATE PROPS SET VALUE = '3.3.3' where `KEY` = 'schemaVersion';

COMMIT;






