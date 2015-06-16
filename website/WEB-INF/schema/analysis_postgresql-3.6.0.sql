-- update schema version
UPDATE PROPS SET VALUE = '3.6.0' where KEY = 'schemaVersion';

commit;

