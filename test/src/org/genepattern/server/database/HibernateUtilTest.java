/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.server.database;


import static org.junit.Assert.*;

import org.genepattern.junitutil.DbUtil;
import org.genepattern.server.DbException;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.database.HibernateUtil;
import org.hibernate.HibernateException;
import org.hibernate.SQLQuery;
import org.junit.Before;
import org.junit.Test;


public class HibernateUtilTest {
    private static final String taskLsid_seqName="lsid_identifier_seq";
    private static final String suiteLsid_seqName="lsid_suite_identifier_seq";
    private HibernateSessionManager mgr;
    private GpConfig gpConfig;

    
    @Before
    public void setUp() throws Exception {
        mgr=DbUtil.getTestDbSession();
        gpConfig = new GpConfig.Builder().build();
    }
    
    /**
     * Parameterized test of getNextSequenceValue; takes care of creating a new entry in the sequence_table.
     * 
     * @param seqId e.g. 'lsid' transformed to 'lsid_identifier_seq'
     * @param dbVendor HSQL | something else (ORACLE not supported)
     * @param createSequence when true create the sequence
     * @param doInTxn when true call beginTransaction before running the tests
     * @throws DbException
     */
    protected void doSeqTest(final String seqId, final String dbVendor, final boolean createSequence, final boolean doInTxn) throws DbException {
        final String seqName=seqId+"_identifier_seq";
        final int nextVal=1;

        try {
            if (createSequence) {
                createSequence(seqName);
            }
            assertFalse("inTxn, before test, dbVendor="+dbVendor, 
                    mgr.isInTransaction());
            if (doInTxn) {
                mgr.beginTransaction();
                assertTrue("inTxn, after beginTxn, dbVendor="+dbVendor, 
                        mgr.isInTransaction());
            }
            
            if (createSequence) { // proxy for new sequence, starting at 1
                exactCheck(doInTxn, dbVendor, seqName, nextVal);
            }
            if (createSequence) { 
                exactCheck(doInTxn, dbVendor, seqName, nextVal+1);
            }
            
            fuzzyCheck(doInTxn, dbVendor, seqName);
            fuzzyCheck(doInTxn, dbVendor, seqName);
        }
        finally {
            mgr.closeCurrentSession();
        }
    }
    
    protected void exactCheck(final boolean doInTxn, final String dbVendor, final String seqName, final int nextVal) throws DbException {
        final int actual=HibernateUtil.getNextSequenceValue(mgr, dbVendor, seqName);
        // exact match
        assertEquals("nextSequenceValue('+seqName+'), dbVendor="+dbVendor, nextVal, 
                actual);
        assertEquals("inTxn, dbVendor="+dbVendor+" after call", doInTxn, 
                mgr.isInTransaction());
    }

    protected void fuzzyCheck(final boolean doInTxn, final String dbVendor, final String seqName) throws DbException {
        final int actual=HibernateUtil.getNextSequenceValue(mgr, dbVendor, seqName);
        // fuzzy match
        assertEquals("nextSequenceVal('"+seqName+"'), dbVendor="+dbVendor, true, actual>0);
        assertEquals("inTxn, dbVendor="+dbVendor+" after call", doInTxn, 
                mgr.isInTransaction());
    }
    
    /**
     * test getNextSequenceValue, when called before a transaction (txn) is started;
     * using default HSQL DB.
     */
    @Test
    public void seqTest_hsqldb() throws DbException {
        doSeqTest("mock_01", "HSQLDB", true, false);        
    }

    @Test
    public void seqTest_hsqldb_lsid() throws DbException {
        boolean createSeq=false;
        doSeqTest("lsid", "HSQLDB", createSeq, false);        
    }

    @Test
    public void seqTest_hsqldb_lsid_txn() throws DbException {
        boolean createSeq=false;
        boolean inTxn=true;
        doSeqTest("lsid", "HSQLDB", createSeq, inTxn);        
    }

    /**
     * Expecting DbException when 'lsid' sequence already exists
     */
    @Test(expected=DbException.class)
    public void seqTest_hsqldb_lsid_exception() throws DbException {
        boolean createSeq=true;
        doSeqTest("lsid", "HSQLDB", createSeq, false);        
    }

    /**
     * test getNextSequenceValue within a transaction (txn);
     * using default HSQL DB.
     */
    @Test
    public void seqTest_hsqldb_txn() throws DbException {
        doSeqTest("mock_02", "HSQLDB", true, true);        
    }

    /**
     * test getNextSequenceValue, when called before a transaction (txn) is started;
     * using generic DB.
     */
    @Test
    public void seqTest_generic() throws DbException {
        doSeqTest("mock_03", "MOCK_VENDOR", true, false);        
    }
    
    /**
     * test getNextSequenceValue within a transaction (txn);
     * using generic DB.
     */
    @Test
    public void seqTest_generic_txn() throws DbException {
        doSeqTest("mock_04", "MOCK_VENDOR", true, true);        
    }

    /**
     * Get next 'lsid' value.
     */
    @Test
    public void getNextLsid() throws DbException {
        try {
            final int seqVal=HibernateUtil.getNextSequenceValue(mgr, gpConfig, taskLsid_seqName);
            assertEquals("nextSequenceVal="+seqVal+" for '"+taskLsid_seqName +"', nextSequenceVal>0", true, seqVal>0);
        }
        finally {
            mgr.closeCurrentSession();
        }
    }

    /**
     * Get next 'lsid' value in a transaction, call it twice.
     */
    @Test
    public void getNextLsid_txn() throws DbException {
        try {
            mgr.beginTransaction();
            int seqVal=HibernateUtil.getNextSequenceValue(mgr, gpConfig, taskLsid_seqName);
            assertEquals("nextSequenceVal="+seqVal+" for '"+taskLsid_seqName +"', nextSequenceVal>0", true, seqVal>0);
            seqVal=HibernateUtil.getNextSequenceValue(mgr, gpConfig, taskLsid_seqName);
            assertEquals("nextSequenceVal="+seqVal+" for '"+taskLsid_seqName +"', nextSequenceVal>0", true, seqVal>0);
        }
        finally {
            mgr.closeCurrentSession();
        }
    }

    /**
     * Get next 'lsid_suite' value.
     */
    @Test
    public void getNextSuiteLsid() throws DbException {
        try {
            final int seqVal=HibernateUtil.getNextSequenceValue(mgr, gpConfig, suiteLsid_seqName);
            assertEquals("nextSequenceVal="+seqVal+" for '"+taskLsid_seqName +"', nextSequenceVal>0", true, seqVal>0);
        }
        finally {
            mgr.closeCurrentSession();
        }
    }

    @Test
    public void testCreateSequence() throws DbException {
        final String mySeqName="my_table_identifier_seq";
        createSequence(mySeqName);
    }
    
    /** Expecting DbException because 'lsid_identifier_seq' is already in the sequence_table */
    @Test(expected=DbException.class)
    public void testCreateSequence_lsid_constraint_violation() throws DbException {
        createSequence("lsid_identifier_seq");
    }

    /** Expecting DbException because 'lsid_suite_identifier_seq' is already in the sequence_table */
    @Test(expected=DbException.class)
    public void testCreateSequence_lsid_suite_constraint_violation() throws DbException {
        createSequence("lsid_suite_identifier_seq");
    }
    
    /**
     * Add a new entry to the SEQUENCE_TABLE.
     * Example insert statement:
       <pre>
       insert into SEQUENCE_TABLE (NAME, NEXT_VALUE) values('lsid_identifier_seq', 1);
       </pre>
     * 
     *  Workaround for this exception:
     *      ids for this class must be manually assigned before calling save(): org.genepattern.server.domain.Sequence
     *  This code won't work,
     *  <pre>
         Sequence seq=new Sequence();
         seq.setName(seqName);
         seq.setNextValue(1);
         mgr.getSession().save(seq);
     *  </pre>
     *  Calling 'seq.setId(... next sequence id ...)' fixes the problem, but it requires knowing the next id. Cannot compute.
     * @param seqName the name of a new entry in the SEQUENCE_TABLE, e.g. 'lsid_identifier_seq'
     * @return The number of rows added
     * 
     * @throws DbException
     *     - general DB connection errors
     *     - when the sequence already exists
     */
    protected int createSequence(final String seqName) throws DbException {
        final boolean inTxn=mgr.isInTransaction();
        try {
            mgr.beginTransaction();
            final String sql = "insert into SEQUENCE_TABLE (NAME, NEXT_VALUE) values(:seqName, :seqNextValue)";
            final SQLQuery query = mgr.getSession().createSQLQuery(sql);
            query.setString("seqName", seqName);
            query.setInteger("seqNextValue", 1);
            int rval=query.executeUpdate();
            if (!inTxn) {
                mgr.commitTransaction();
            }
            return rval;
        }
        catch (HibernateException e) {
            if (e.getCause() != null) {
                throw new DbException(e.getCause().getLocalizedMessage(), e.getCause());
            }
            throw new DbException(e);
        }
        catch (Throwable t) {
            throw new DbException("Unexpected error: "+t.getLocalizedMessage(), t);
        }
        finally {
            if (!inTxn) {
                mgr.closeCurrentSession();
            }
        }
    }

}
