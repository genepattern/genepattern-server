/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2011) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.
 
 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.database;


import static junit.framework.Assert.assertEquals;
import org.genepattern.junitutil.DbUtil;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.database.HibernateUtil;
import org.junit.Before;
import org.junit.Test;


public class HibernateUtilTest {
    private final String seqName="lsid_identifier_seq";
    @Before
    public void setUp() throws Exception {
        DbUtil.initDb();
    }
    
    @Test
    public void getNextSequenceValue_legacy() throws Exception {
        System.setProperty("database.vendor", "HSQL");

        HibernateUtil.beginTransaction();
        try {
            final int seqVal=HibernateUtil.getNextSequenceValue(seqName);
            assertEquals("nextSequenceVal="+seqVal+" for '"+seqName +"', nextSequenceVal>0", true, seqVal>0);
        }
        finally {
            //Note: for HSQL, rollback has no effect on the sequence
            HibernateUtil.rollbackTransaction();
        }
    }
    
    @Test
    public void getNextSequenceValue() {
        GpConfig gpConfig = new GpConfig.Builder().build();
        HibernateUtil.beginTransaction();
        try {
            final int seqVal=HibernateUtil.getNextSequenceValue(gpConfig, seqName);
            assertEquals("nextSequenceVal="+seqVal+" for '"+seqName +"', nextSequenceVal>0", true, seqVal>0);
        }
        finally {
            //Note: for HSQL, rollback has no effect on the sequence
            HibernateUtil.rollbackTransaction();
        }
    }

}
