-- update schema version
update PROPS set value='3.9.3' where `key`='schemaVersion';
commit;
