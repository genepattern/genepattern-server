create table BATCH_JOB (
    JOB_NO int identity  not null,
    USER_ID varchar (255),
    DELETED boolean, 
    DATE_SUBMITTED timestamp,
constraint bj_pk primary key (JOB_NO),
constraint bj_fk foreign key (USER_ID) references GP_USER(USER_ID)
);

create table BATCH_ANALYSIS(
    BATCH_JOB int not null,
    ANALYSIS_JOB int not null,
constraint ba_bj_fk foreign key (BATCH_JOB) references BATCH_JOB(JOB_NO) on delete cascade,
constraint ba_aj_fk foreign key (ANALYSIS_JOB) references ANALYSIS_JOB(JOB_NO) on delete cascade
);
-- update schema version
update props set value='3.3.0' where key='schemaVersion';
