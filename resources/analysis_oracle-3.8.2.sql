--
-- drop the uniqueness constraint from the 'job_runner_job' table, initially created in gp-3.7.6
--
alter table job_runner_job drop unique (jr_classname, jr_name, ext_job_id);

--
-- Add foreign key constraint with cascade delete 
--
-- To avoid ORA-02298, first delete any entries in the job_runner_job table 
-- which no longer reference entries in the (parent) analysis_job table
delete from job_runner_job where gp_job_no in (
    select distinct (gp_job_no) from job_runner_job
    minus
    select distinct (job_no) from analysis_job
);
commit;
-- then add the constraint
alter table job_runner_job add constraint gp_job_no_fk FOREIGN KEY (gp_job_no) references ANALYSIS_JOB(job_no) on delete cascade;

-- update schema version
update props set value='3.8.2' where `key`='schemaVersion';

commit;
