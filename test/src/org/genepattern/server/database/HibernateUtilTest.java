/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.server.database;


import static org.junit.Assert.*;

import org.genepattern.junitutil.DbUtil;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.database.HibernateUtil;
import org.junit.Before;
import org.junit.Test;


public class HibernateUtilTest {
    private static final String seqName="lsid_identifier_seq";
    private HibernateSessionManager mgr;
    
    @Before
    public void setUp() throws Exception {
        mgr=DbUtil.getTestDbSession();
    }
    
    @Test
    public void getNextSequenceValue() {
        GpConfig gpConfig = new GpConfig.Builder().build();
        mgr.beginTransaction();
        try {
            final int seqVal=HibernateUtil.getNextSequenceValue(mgr, gpConfig, seqName);
            assertEquals("nextSequenceVal="+seqVal+" for '"+seqName +"', nextSequenceVal>0", true, seqVal>0);
        }
        finally {
            //Note: for HSQL, rollback has no effect on the sequence
            HibernateUtil.rollbackTransaction();
        }
    }

}
