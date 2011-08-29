-- for GenomeSpace integration, link GP user account to GS user account
create table GS_ACCOUNT (
    GP_USERID varchar2(255),
    TOKEN varchar2 (255),
constraint gsa_pk primary key (GP_USERID),
constraint gsa_fk foreign key (GP_USERID) references GP_USER(USER_ID)
);

-- improve performance by creating indexes on the ANALYSIS_JOB table
CREATE INDEX IDX_AJ_STATUS ON ANALYSIS_JOB(STATUS_ID);
CREATE INDEX IDX_AJ_PARENT ON ANALYSIS_JOB(PARENT);

-- update schema version
INSERT INTO PROPS (KEY, VALUE) VALUES ('registeredVersion3.3.3', '3.3.3');

COMMIT;






