create table BATCH_JOB (
    JOB_NO int identity  not null,
    USER_ID varchar (255),
    DELETED boolean, 
constraint bj_pk primary key (JOB_NO),
constraint bj_fk foreign key (USER_ID) references GP_USER(USER_ID)
);

create table BATCH_ANALYSIS(
    BATCH_JOB int not null,
    ANALYSIS_JOB int not null,
constraint aj_fk foreign key (ANALYSIS_JOB) references ANALYSIS_JOB(JOB_NO)
);
-- update schema version
update props set value='3.2.1' where key='schemaVersion';
