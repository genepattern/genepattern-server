/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2009) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.
 
 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.database;


import junit.framework.TestCase;

public class HibernateUtilTest extends TestCase {
    
    public void testGetNextSequenceValue() {
    	HibernateUtil.beginTransaction();
        System.setProperty("database.vendor", "HSQL");
        String seqName = "lsid_suite_identifier_seq";       
        System.out.println(HibernateUtil.getNextSequenceValue(seqName));
        HibernateUtil.rollbackTransaction();
    }

}
