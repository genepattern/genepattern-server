
/* Add registration date to user */
alter table GP_USER add registration_date timestamp;


update props set value='3.1' where key='schemaVersion';


