-- update schema version
UPDATE PROPS SET VALUE = '3.5.1' where KEY = 'schemaVersion';

commit;

