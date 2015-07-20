create table BATCH_JOB (
    JOB_NO int identity  not null,
    USER_ID varchar (255) references GP_USER(USER_ID),
    DELETED boolean, 
    DATE_SUBMITTED timestamp
);

create table BATCH_ANALYSIS(
    BATCH_JOB int not null references BATCH_JOB(JOB_NO) on delete cascade,
    ANALYSIS_JOB int not null references ANALYSIS_JOB(JOB_NO) on delete cascade
);
