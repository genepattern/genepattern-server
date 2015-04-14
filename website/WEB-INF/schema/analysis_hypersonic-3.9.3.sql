-- update schema version
update props set value='3.9.3' where key='schemaVersion';
commit;
