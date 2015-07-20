-- add tables for Group Permissions.
-- Permission Flags
create table PERMISSION_FLAG (
    ID int not null,
    NAME varchar(32) not null,
constraint pf_pk primary key (id)
);
--Populate Permission Flags
insert into PERMISSION_FLAG values(1,'READ_WRITE');
insert into PERMISSION_FLAG values(2,'READ');

-- Table for storing group permissions per job
create table JOB_GROUP (
    JOB_NO int not null,
    GROUP_ID varchar(128) not null,
    PERMISSION_FLAG int not null,
constraint jg_pk PRIMARY KEY (job_no, group_id),
constraint jn_fk FOREIGN KEY (job_no) references ANALYSIS_JOB(job_no),
constraint pf_fk FOREIGN KEY (permission_flag) references PERMISSION_FLAG(id)
);
