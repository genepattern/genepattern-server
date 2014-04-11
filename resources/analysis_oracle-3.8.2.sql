-- update schema version
update props set value='3.8.2' where key='schemaVersion';

commit;
