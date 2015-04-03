-- update schema version
UPDATE PROPS SET VALUE = '3.7.0' where KEY = 'schemaVersion';

commit;

