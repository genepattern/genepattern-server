-- for GenomeSpace integration, link GP user account to GS user account
alter table GS_ACCOUNT add(GS_USERID VARCHAR2(512));
alter table GS_ACCOUNT add(TOKEN_TIMESTAMP TIMESTAMP);
alter table GS_ACCOUNT add (GS_EMAIL VARCHAR2(512));

-- update schema version
insert into PROPS (KEY, VALUE) VALUES ('registeredVersion3.4.0', '3.4.0');
commit;
