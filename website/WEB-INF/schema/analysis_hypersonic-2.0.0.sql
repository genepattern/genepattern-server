--Suite Table
create table suite(lsid longvarchar, name longvarchar, author longvarchar, owner longvarchar, description longvarchar, access_id int);

-- Suite Modules
create table suite_modules(lsid longvarchar, module_lsid longvarchar);

create index idx_suite_lsid on suite(lsid);
create index idx_suite_modules_lsid on suite_modules(lsid);
create index idx_modules_suite_lsid on suite_modules(module_lsid);

create sequence lsid_suite_identifier_seq as integer start with 1;
