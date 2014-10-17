create table job_comment (
 id integer not null,
 gp_job_no integer not null,
 comment_text varchar(1023) not null,
 -- parent id references id of the job_comment table
 parent_id integer, 
 posted_date timestamp not null,
 user_id varchar(255) not null,
 primary key (id),
 CONSTRAINT fk_gp_job_no
    FOREIGN KEY (gp_job_no)
    REFERENCES analysis_job (job_no)
    ON DELETE CASCADE
);

CREATE SEQUENCE JOB_COMMENT_SEQ
  START WITH 1
  MAXVALUE 999999999999999999999999999
  MINVALUE 0
  NOCYCLE
  NOCACHE
  NOORDER;

create table tag (
 tag_id integer not null,
 date_added timestamp not null,
 public_tag number(1,0) default 0 not null,
 tag varchar(511) not null,
 user_id varchar(255) not null,
 primary key (tag_id)
);

CREATE SEQUENCE TAG_SEQ
  START WITH 1
  MAXVALUE 999999999999999999999999999
  MINVALUE 0
  NOCYCLE
  NOCACHE
  NOORDER;

create table job_tag (
 id integer not null,
 date_tagged timestamp not null,
 gp_job_no integer not null,
 tag_id integer not null,
 user_id varchar(255) not null,
 primary key (id),
 CONSTRAINT jt_fk_tag_id
    FOREIGN KEY (tag_id)
    REFERENCES tag (tag_id),
 CONSTRAINT jt_fk_gp_job_no
    FOREIGN KEY (gp_job_no)
    REFERENCES analysis_job (job_no)
    ON DELETE CASCADE
);

CREATE SEQUENCE JOB_TAG_SEQ
  START WITH 1
  MAXVALUE 999999999999999999999999999
  MINVALUE 0
  NOCYCLE
  NOCACHE
  NOORDER;

create index idx_comment_text on job_comment(comment_text);

create index idx_tag on tag(tag);

-- update schema version
update props set value='3.9.1' where key='schemaVersion';
