-- update schema version
UPDATE PROPS SET VALUE = '3.8.0' where `KEY` = 'schemaVersion';

commit;
