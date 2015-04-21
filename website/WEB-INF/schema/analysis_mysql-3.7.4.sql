-- update schema version
UPDATE PROPS SET VALUE = '3.7.4' where `KEY` = 'schemaVersion';

commit;

