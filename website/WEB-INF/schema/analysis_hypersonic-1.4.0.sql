alter table analysis_job drop input_filename;
alter table analysis_job drop result_filename;
alter table analysis_job add task_lsid varchar(200);
alter table analysis_job add task_name longvarchar;
alter table analysis_job add parent int;
alter table analysis_job add deleted bit default false;

drop index idx_job_lsid;

create index idx_analysis_job_lsid on analysis_job(lsid);
create index idx_analysis_job_parent on analysis_job(parent);
create index idx_analysis_job_job_no on analysis_job(job_no);
create index idx_analysis_job_deleted on analysis_job(deleted);

drop table task_type;

delete from job_status where status_name='Timed Out';
update job_status set status_name='Pending' where status_name='Not Started';
