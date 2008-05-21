alter table GP_USER add registration_date timestamp;

-- update schema version
UPDATE GPPORTAL.PROPS SET VALUE = '3.1' where KEY = 'schemaVersion';

COMMIT;














