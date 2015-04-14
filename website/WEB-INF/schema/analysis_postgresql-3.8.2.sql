
-- update schema version
UPDATE PROPS SET VALUE = '3.8.2' where KEY = 'schemaVersion';

commit;
