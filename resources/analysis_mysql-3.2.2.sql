-- update schema version
UPDATE PROPS SET VALUE = '3.2.2' where `KEY` = 'schemaVersion';

COMMIT;









