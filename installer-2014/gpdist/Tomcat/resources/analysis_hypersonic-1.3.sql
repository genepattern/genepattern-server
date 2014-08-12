-- GenePattern/Omnigene database schema

--Task master table

create table task_master(task_ID int identity primary key, task_Name longvarchar, description longvarchar, className longvarchar, parameter_info longvarchar, load_flag bit, type_id int, repeat_minute longvarchar, repeat_hour varchar(60), repeat_day_of_month varchar(60), repeat_month_of_year varchar(60), repeat_day_of_week varchar(60), taskInfoAttributes longvarchar, user_id longvarchar, access_id int, isIndexed bit, lsid varchar(200));

create index idx_lsid on task_master(lsid);
create index idx_is_indexed on task_master(isIndexed);
create index idx_user_id on task_master(user_id);
create index idx_access_id on task_master(access_id);
create index idx_task_name on task_master(task_name);

create table lsids(lsid varchar(200), lsid_no_version varchar(200), lsid_version varchar(100));
create index idx_lsids_lsid on lsids(lsid);
create index idx_lsid_no_version on lsids(lsid_no_version);
create index idx_lsid_version on lsids(lsid_version);

--Job Info table

create table analysis_job(job_no int identity primary key, task_id int, status_id int, date_submitted timestamp, date_completed timestamp, parameter_info longvarchar, user_id longvarchar, isIndexed bit, access_id int, job_name longvarchar, lsid varchar(200), input_filename longvarchar, result_filename longvarchar);

create index idx_job_lsid on analysis_job(lsid);

--Job Status Info table
create table Job_status(status_id int, status_name varchar(20));

--Populate Job staus info table
insert into job_status values(1,'Pending');
insert into job_status values(2,'Processing');
insert into job_status values(3,'Finished');
insert into job_status values(4,'Error');


create unique index idx_status_id on job_status(status_id);

--Access Table
create table task_access(access_id int identity primary key, name longvarchar, description longvarchar);

-- Task type
create table task_type(id int identity primary key);

--Populate access  table
insert into task_access values(1,'public','public access');
insert into task_access values(2,'private','access only for the owner');

--"dual" table - same idea as Oracle's dual table
create table dual(not_used int);
insert into dual values (0);

create sequence lsid_identifier_seq as integer start with 1;

create table props(key varchar(100) primary key, value varchar(256));
insert into props (key, value) values ('schemaVersion', '1.3');
