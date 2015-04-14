-- update schema version
UPDATE PROPS SET VALUE = '3.9.3' where KEY = 'schemaVersion';
commit;
