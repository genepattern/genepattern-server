-- update schema version
UPDATE PROPS SET VALUE = '3.7.2' where KEY = 'schemaVersion';

commit;

