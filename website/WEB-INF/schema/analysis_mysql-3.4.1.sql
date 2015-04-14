-- update schema version
UPDATE PROPS SET VALUE = '3.4.1' where `KEY` = 'schemaVersion';
commit;
