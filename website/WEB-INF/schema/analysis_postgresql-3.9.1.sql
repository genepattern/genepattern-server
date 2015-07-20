create table job_comment (
 id serial primary key,
 gp_job_no bigint not null references analysis_job (job_no) on delete cascade,
 comment_text text not null,
 -- parent id references id of the job_comment table
 parent_id integer,
 posted_date timestamp not null,
 user_id text not null
);

create table tag (
 tag_id serial primary key,
 date_added timestamp not null,
 public_tag boolean default false not null,
 tag TEXT not null,
 user_id text not null
);


create table job_tag (
 id serial primary key,
 date_tagged timestamp not null,
 gp_job_no bigint not null references analysis_job (job_no) on delete cascade,
 tag_id integer not null references tag (tag_id),
 user_id text not null
);

create index idx_comment_text on job_comment(comment_text);

create index idx_tag on tag(tag);

commit;
