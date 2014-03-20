create table job_input_value (
    gp_job_no integer not null,
    idx integer default 1 not null,
    pname varchar(511) not null,
    pvalue varchar(1023) default '',
    group_id varchar(511) default '',
    group_name varchar(511) default '',
    primary key (gp_job_no, idx, pname),
    constraint jiv_gpjn_fk foreign key (GP_JOB_NO) references ANALYSIS_JOB(JOB_NO) on delete cascade
);


--
-- small change to make it easier to remove eula records
-- 
alter table eula_remote_queue 
    drop constraint erq_fk;
alter table eula_remote_queue
    add constraint erq_fk
    FOREIGN KEY (eula_record_id) references eula_record(id)
    on delete cascade;

-- update schema version
update props set value='3.8.1' where key='schemaVersion';

commit;
