-- 
-- additional built-in patch_info entries
-- 
-- Note: the 'where not exists' clause is required to avoid duplicate insert
--     For example when updating a server which has already installed one of the patches
-- 

insert into patch_info (lsid) select 
        'urn:lsid:broadinstitute.org:plugin:Ant_1.8:1' as lsid from dual
    where not exists ( select lsid from patch_info where lsid = 
        'urn:lsid:broadinstitute.org:plugin:Ant_1.8:1' );

insert into patch_info (lsid) select 
        'urn:lsid:broad.mit.edu:cancer.software.genepattern.server.patch:GenePattern_3_4_2:2' as lsid from dual
    where not exists ( select lsid from patch_info where lsid = 
        'urn:lsid:broad.mit.edu:cancer.software.genepattern.server.patch:GenePattern_3_4_2:2' );

insert into patch_info (lsid) select 
        'urn:lsid:broadinstitute.org:plugin:GenePattern_3_9_1:0.1' as lsid from dual
    where not exists ( select lsid from patch_info where lsid = 
        'urn:lsid:broadinstitute.org:plugin:GenePattern_3_9_1:0.1' );

insert into patch_info (lsid) select 
        'urn:lsid:broadinstitute.org:plugin:GenePattern_3_9_3:1' as lsid from dual
    where not exists ( select lsid from patch_info where lsid = 
        'urn:lsid:broadinstitute.org:plugin:GenePattern_3_9_3:1' );
