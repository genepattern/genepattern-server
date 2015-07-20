--
-- create table define custom top-level categories for a module 
-- the task must be a baseLsid (no version)
-- the category must be non-null, empty string or any string prefixed with a '.' means
-- the module should be hidden from the top-level categories on the Modules & Pipelines page
create table TASK_CATEGORY (
    category varchar(255) not null,
    task varchar(255) not null,
    primary key (category, task));

-- example insert statement
-- insert into task_category(category,task) values
--    ( 'MIT_701X', 'urn:lsid:8086.jtriley.starapp.mit.edu:genepatternmodules:11'  )

commit;

