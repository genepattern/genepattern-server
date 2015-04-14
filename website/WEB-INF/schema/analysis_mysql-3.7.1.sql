-- update schema version
UPDATE PROPS SET VALUE = '3.7.1' where `KEY` = 'schemaVersion';

commit;

