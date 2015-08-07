-- create indexes on the job_group table to speed up jdbc queries
create index idx_job_group_job_no on job_group(job_no);

-- add a new job status
insert into JOB_STATUS values(5,'Dispatching');
insert into JOB_STATUS values(0,'Waiting');
