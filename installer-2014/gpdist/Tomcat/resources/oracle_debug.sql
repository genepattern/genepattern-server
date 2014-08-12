select * from v$session
  where USERNAME = 'GPPORTAL'
go
select * from v$locked_object 
  where ORACLE_USERNAME = 'GPPORTAL'
  order by SESSION_ID
go
select * from v$lock 
  where sid = 951 or sid = 971
go
select * from v$lock where sid = 1057
go
select owner, object_name, subobject_name, object_id, object_type 
  from dba_objects 
  where object_id = 351571 or object_id = 351591
go
select * 
  from dba_objects 
  where object_id = 351571 or object_id = 351591 
go
select a.JOB_NO, j.status_id, j.status_name 
  from ANALYSIS_JOB a, JOB_STATUS j 
  where a.status_id = j.status_id 
  order by a.JOB_NO
go
select * from JOB_STATUS
go
update ANALYSIS_JOB set  status_id = 3 where  status_id = 2
go
select JOB_NO, TASK_ID, STATUS_ID, DATE_SUBMITTED, DATE_COMPLETED, USER_ID, ISINDEXED, ACCESS_ID, JOB_NAME, LSID, TASK_LSID, TASK_NAME, PARENT
  from ANALYSIS_JOB
  where DELETED = 0
  order by JOB_NO
go
select TASK_ID, TASK_NAME, DESCRIPTION, TYPE_ID, USER_ID, ACCESS_ID, LSID, ISINDEXED 
  from TASK_MASTER
  order by TASK_ID
go
select * from JOB_COMPLETION_EVENT 
  order by ID
go
select JOB_NO, TASK_ID, STATUS_ID, DELETED from ANALYSIS_JOB
  order by JOB_NO
go
delete from JOB_COMPLETION_EVENT where JOB_NUMBER = 84
go
delete from JOB_COMPLETION_EVENT
go
select * from db$objects
go
select * from lsids
go
update ANALYSIS_JOB set STATUS_ID = 3 where job_no = 190



