-- 
-- additional built-in patch_info entries
-- 

insert into patch_info (id, lsid) values (patch_info_SEQ.nextVal, 'urn:lsid:broadinstitute.org:plugin:Ant_1.8:1');
insert into patch_info (id, lsid) values (patch_info_SEQ.nextVal, 'urn:lsid:broad.mit.edu:cancer.software.genepattern.server.patch:GenePattern_3_4_2:2');
insert into patch_info (id, lsid) values (patch_info_SEQ.nextVal, 'urn:lsid:broadinstitute.org:plugin:GenePattern_3_9_1:0.1');
insert into patch_info (id, lsid) values (patch_info_SEQ.nextVal, 'urn:lsid:broadinstitute.org:plugin:GenePattern_3_9_3:1');

commit;
