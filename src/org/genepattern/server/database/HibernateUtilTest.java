package org.genepattern.server.database;


import junit.framework.TestCase;

public class HibernateUtilTest extends TestCase {
    
    public void testGetNextSequenceValue() {
        
        String seqName = "lsid_suite_identifier_seq";       
        System.out.println(HibernateUtil.getNextSequenceValue(seqName));
        
    }

}
