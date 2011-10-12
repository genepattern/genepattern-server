-- for GenomeSpace integration, link GP user account to GS user account
alter table GS_ACCOUNT add GS_USERID varchar;
alter table GS_ACCOUNT add TOKEN_TIMESTAMP timestamp;

-- update schema version
update props set value='3.4.0' where key='schemaVersion';
