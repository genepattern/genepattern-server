-- update schema version
UPDATE PROPS SET VALUE = '3.1' where `KEY` = 'schemaVersion';

COMMIT;














