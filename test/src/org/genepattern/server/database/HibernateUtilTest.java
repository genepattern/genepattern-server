/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

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
