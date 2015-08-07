create table job_input (
    id number(10,0) not null, 
    job_id integer,
    name varchar2 (511),
    user_value varchar2 (4000), 
    cmd_value varchar2 (4000),  
    type varchar2 (255),  
    unique (job_id, name),
    CONSTRAINT JI_PK PRIMARY KEY (id)
);
create index idx_job_input_job_id on job_input (job_id);

create table job_input_attribute (
    id number(10,0) not null, 
    input_id number(10,0),
    name varchar2 (255),
    val varchar2 (4000), 
    CONSTRAINT JIA_PK PRIMARY KEY (id)
);
create index idx_jia_id on job_input_attribute (input_id);

create table job_result (
    id number(10,0) not null, 
    job_id integer,
    name varchar2 (511),
    path varchar2 (511), 
    log number (1,0) default 0 not null,  
    unique (job_id, name),
    CONSTRAINT JR_PK PRIMARY KEY (id)
);
create index idx_job_result_job_id on job_result (job_id);

commit;
