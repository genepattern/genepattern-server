package org.genepattern.server.webservice.server.dao;

import junit.framework.TestCase;

public class HibernateUtilTest extends TestCase {
    
    public void testGetNextSequenceValue() {
        
        String seqName = "lsid_suite_identifier_seq";       
        System.out.println(HibernateUtil.getNextSequenceValue(seqName));
        
    }

}
