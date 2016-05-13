package org.genepattern.server.genepattern;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.util.concurrent.ExecutionException;

import org.genepattern.junitutil.DbUtil;
import org.genepattern.junitutil.Demo;
import org.genepattern.server.DbException;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.domain.Lsid;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.util.LsidBuilder;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * LSID Template:
 *     lsid=urn:lsid:{lsid.authority}:{namespace}:{identifier}:{version}
 * 
 * @author pcarr
 */
public class TestGetNextTaskLsid {
    private static HibernateSessionManager mgr;
    private static GpConfig defaultGpConfig; // uses HSQL DB
    private static GpConfig gpConfig; // uses custom DB vendor
    
    private static GpContext gpContext;
    
    // use Generic Sequence table for testing
    private final static String customDbVendor="MOCK_JUNIT_DB_VENDOR";
    
    private static String junit_authority="junit-lsid-authority";
    private static String junit_namespace;
    private static int junit_identifier;
    private static String junit_baseLsid;
    private static final String[] existingVersions=new String[]{"1", "2", "2.1", "3", "3.1", "3.2", /* skip 3.3 */ "3.4", "3.4.1", "3.5", "3.5.1", "3.5.2"};
    
    @BeforeClass
    public static void beforeClass() throws ExecutionException, DbException {
        mgr=DbUtil.getTestDbSession();
        
        defaultGpConfig=Demo.gpConfig();
        gpConfig=Demo.gpConfig();
        when(gpConfig.getDbVendor()).thenReturn(customDbVendor);
        gpContext=Demo.serverContext;

        assertEquals("Demo.defaultGpConfig.dbVendor", "HSQL", defaultGpConfig.getDbVendor());
        assertEquals("Demo.gpConfig.lsidAuthority", GpConfig.DEFAULT_LSID_AUTHORITY, 
                gpConfig.getLsidAuthority(gpContext));
        
        junit_namespace=initCustomNamespace();
        junit_identifier=initCustomIdentifier(junit_namespace);
        junit_baseLsid="urn:lsid:broad-cancer-genomics:"+junit_namespace+":"+junit_identifier;
        for(final String v : existingVersions) {
            saveLsid(mgr, junit_baseLsid+":"+v);
        }
    }
    
    // helper method for adding LSID to the db
    public static void saveLsid(final HibernateSessionManager mgr, final String lsid) {
        Lsid lsidHibernate = new Lsid(lsid);
        final boolean isInTransaction=mgr.isInTransaction();
        try {
            mgr.beginTransaction();
            mgr.getSession().save(lsidHibernate);
            if (!isInTransaction) {
                mgr.commitTransaction();
            }
        }
        finally {
            if (!isInTransaction) {
                mgr.commitTransaction();
            }
        }
    }

    // helper method for creating a custom lsid namespace
    protected static String initCustomNamespace() throws DbException {
        String customNamespace;
        String customSeqName;
        customNamespace="junit." + Demo.randomSequence(8);
        customSeqName=customNamespace+"_seq";
        HibernateUtil.createSequenceGeneric(mgr, customSeqName);
        return customNamespace;
    }

    /**
     * Increment the next lsid identifier for the given customNamespace.
     * 
     * @param customNamespace
     * @throws DbException
     */
    protected static int initCustomIdentifier(final String customNamespace) throws DbException {
        return LSIDManager.getNextLSIDIdentifier(mgr, gpConfig, customNamespace);
    }
    
    @Test
    public void testLsidEmptyInitializer() throws MalformedURLException {
        LSID lsid=new LSID(""); //urn:lsid::::");
        assertEquals("lsid.authority",  "", lsid.getAuthority());
        assertEquals("lsid.namespace",  "", lsid.getNamespace());
        assertEquals("lsid.identifier", "", lsid.getIdentifier());
        assertEquals("lsid.version",    "", lsid.getVersion());
    }
    
    @Test
    public void getSequenceName_custom_namespace() {
        final String namespace="junit." + Demo.randomSequence(8);
        assertEquals(namespace+"_seq", LSIDManager.getSequenceName(namespace));
    }
    
    // helper test, because we can't know the exact lsid.identifier
    protected void testFromHsqlDb(final String requestedLsid) throws RemoteException {
        @SuppressWarnings("deprecation")
        final LSID actual=LSIDManager.getNextTaskLsid(mgr, defaultGpConfig, gpContext, requestedLsid);
        assertEquals("nextLsid('"+requestedLsid+"').authority", GpConfig.DEFAULT_LSID_AUTHORITY, actual.getAuthority());
        assertEquals("nextLsid('"+requestedLsid+"').namespace", GPConstants.TASK_NAMESPACE, actual.getNamespace());
        assertEquals("nextLsid('"+requestedLsid+"').identifier >= 0", true, Integer.valueOf(actual.getIdentifier()) >= 0);
        assertEquals("nextLsid('"+requestedLsid+"').version", "1", actual.getVersion());
    }
    
    
    @Test
    public void hsqldb_getNextLsid_fromNull() throws RemoteException {
        testFromHsqlDb(null);
    }

    @Test
    public void hsqldb_getNextLsid_fromEmpty() throws RemoteException {
        testFromHsqlDb("");
    }

    @Test
    public void hsqldb_getNextLsid_fromCustomAuthority() throws RemoteException {
        testFromHsqlDb("urn:lsid:"+"junit-lsid-authority"+":"+GPConstants.TASK_NAMESPACE+":"+"1"+":2");
    }
    
    @Test
    public void getNextLsid_fromNull() throws ExecutionException, RemoteException, MalformedURLException, DbException {
        final String customNamespace=initCustomNamespace();
        // version (null)->"1"
        final String requestedLSID=null;
        assertEquals(
                // expected
                new LSID("urn:lsid:broad-cancer-genomics:"+customNamespace+":1:1"),
                // actual
                LSIDManager.getNextLsid(mgr, gpConfig, gpContext, customNamespace, requestedLSID)
                );
    }
    
    @Test
    public void getNextLsid_fromEmpty() throws ExecutionException, RemoteException, MalformedURLException, DbException {
        final String customNamespace=initCustomNamespace();
        // version ""->"1"
        final String requestedLSID="";
        assertEquals(
                // expected
                new LSID("urn:lsid:broad-cancer-genomics:"+customNamespace+":1:1"),
                // actual
                LSIDManager.getNextLsid(mgr, gpConfig, gpContext, customNamespace, requestedLSID)
                );
    }
    
    @Test
    public void getNextLsid_from_v1() throws MalformedURLException, RemoteException, DbException {
        final String fromV="1";
        final String toV="2";
        
        final String customNamespace=initCustomNamespace();
        final int customIdentifier=initCustomIdentifier(customNamespace);

        // version 1->2
        final String requestedLSID="urn:lsid:broad-cancer-genomics:"+customNamespace+":"+customIdentifier+":"+fromV;
        assertEquals(
                "nextLsid from v"+fromV+"",
                // expected
                new LSID("urn:lsid:broad-cancer-genomics:"+customNamespace+":"+customIdentifier+":"+toV),
                // actual
                LSIDManager.getNextLsid(mgr, gpConfig, gpContext, customNamespace, requestedLSID)
                );
    }
    
    @Test
    public void getNextLsid_from_v2() throws MalformedURLException, RemoteException, DbException {
        final String customNamespace=initCustomNamespace();

        // version 2->3
        final String requestedLSID="urn:lsid:broad-cancer-genomics:"+customNamespace+":1:2";
        assertEquals(
                // expected
                new LSID("urn:lsid:broad-cancer-genomics:"+customNamespace+":1:3"),
                // actual
                LSIDManager.getNextLsid(mgr, gpConfig, gpContext, customNamespace, requestedLSID)
                );
    }
    
    /**
     * When the the requested lsid.authority does not match the server lsid.authority, 
     * create a new lsid with a new identifier starting a version 1.
     */
    @Test
    public void getNextLsid_fromCustomLsidAuthority() throws MalformedURLException, RemoteException, DbException {
        final String customNamespace=initCustomNamespace();
        final int customIdentifier=initCustomIdentifier(customNamespace);
        final String customAuthority="junit-lsid-authority";

        final String requestedLsidStr="urn:lsid:"+customAuthority+":"+customNamespace+":"+customIdentifier+":2";
        final LSID actual=LSIDManager.getNextLsid(mgr, gpConfig, gpContext, customNamespace, requestedLsidStr);
        // expecting ...
        assertEquals("nextLsid.authority", GpConfig.DEFAULT_LSID_AUTHORITY, actual.getAuthority());
        assertEquals("nextLsid.namespace", customNamespace, actual.getNamespace());
        assertEquals("nextLsid.identifier (expecting new identifier)", ""+(customIdentifier+1), actual.getIdentifier());
        assertEquals("nextLsid.version (expecting initial v1 version)", "1", actual.getVersion());
    }

    /**
     * Test case when next version already exists.
     * See expectedVersions for the list of versions pre-populated into the LSIDS table.
     * 
     * @param fromVersion
     * @param expectedVersion
     * @param message
     * @throws MalformedURLException
     * @throws RemoteException
     */
    protected void doVersionExistsTest(final String fromVersion, final String expectedVersion, final String message) 
    throws Exception {
        assertEquals(
                "nextLsid from v"+fromVersion+", "+message,
                // expected
                new LSID(junit_baseLsid+":"+expectedVersion),
                // actual
                LSIDManager.getNextLsid(mgr, gpConfig, gpContext, junit_namespace, junit_baseLsid+":"+fromVersion)
                );
    }

    @Test
    public void versionExists_majorToMinor() throws Exception {
        // 1 -> 1.1, 2 is already present
        doVersionExistsTest("1", "1.1", "2 is already present");
    }

    @Test
    public void versionExists_majorToPatch() throws Exception {
        // 2 -> 2.0.1, 3 is already present, 2.1 is already present
        doVersionExistsTest("2", "2.0.1", "3 is already present, 2.1 is already present");
    }

    @Test
    public void versionExists_minorToMinor() throws Exception {
        // 2.1 -> 2.2, 3 is already present, 2.1 is already present
        doVersionExistsTest("2.1", "2.2", "3 is already present, 2.1 is already present");
    }

    @Test
    public void versionExists_minorToMinor_gap() throws Exception {
        // 3.2 -> 3.3, 3.3 is not present
        doVersionExistsTest("3.2", "3.3", "3.3 is not present, 3.4 is");
    }

    @Test
    public void versionExists_minorToPatch() throws Exception {
        // 3.1 -> 3.1.1, 3.2 is already present
        doVersionExistsTest("3.1", "3.1.1", "3.2 is already present");
    }

    @Test
    public void versionExists_patchToPatch() throws Exception {
        doVersionExistsTest("3.4.1", "3.4.2", "3.5 is present");
    }

    @Test
    public void versionExists_latestMajor() throws Exception {
        doVersionExistsTest("3", "4", "latest major version (jumps to next major), even when 3.1 is present");
    }
    
    @Test
    public void versionExists_latestMinor() throws Exception {
        doVersionExistsTest("3.5", "3.6", "latest minor version");
    }
    
    @Test
    public void versionExists_latestPatch() throws Exception {
        doVersionExistsTest("3.5.2", "3.5.3", "latest patch version");
    }
    
    @Test
    public void versionExists_nextMajor_not_in_db() throws Exception {
        doVersionExistsTest("4", "5", "version 4 is not in DB, but we still increment");
    }
    
    @Test
    public void versionExists_nextMinor_not_in_db() throws Exception {
        doVersionExistsTest("3.6", "3.7", "version 3.6 is not in DB, but we still increment");
    }

    @Test
    public void versionExists_nextPatch_not_in_db() throws Exception {
        doVersionExistsTest("3.5.3", "3.5.4", "version 3.5.3 is not in DB, but we still increment");
    }


    @Test
    public void customNamespace_genericDbSequence() throws MalformedURLException, RemoteException, DbException {
        final String customNamespace=initCustomNamespace();
        // only works for non-HSQL non-ORACLE dbVendor
        //when(gpConfig.getDbVendor()).thenReturn(customDbVendor);
        assertEquals(
                // expected
                new LSID("urn:lsid:broad-cancer-genomics:"+customNamespace+":1:1"),
                // actual 
                LSIDManager.getNextLsid(mgr, gpConfig, gpContext, customNamespace, null)
                );
    }

    @Test
    public void lsidBuilder_custom() throws MalformedURLException, DbException {
        LSID lsid=new LsidBuilder()
            .authority(junit_authority)
            .namespace(junit_namespace)
            .identifier(""+junit_identifier)
            .version("3.1.415")
        .build();
        
        assertEquals("lsid.authority", junit_authority, lsid.getAuthority());
        assertEquals("lsid.namespace", junit_namespace, lsid.getNamespace());
        assertEquals("lsid.identifier", ""+junit_identifier, lsid.getIdentifier());
        assertEquals("initialVersion", "3.1.415", lsid.getVersion());
    }

    @Test
    public void lsidBuilder_default() throws MalformedURLException, DbException {  
        final LSID lsid=new LsidBuilder()
                .mgr(mgr)
                .gpConfig(gpConfig)
                .gpContext(gpContext)
        .build();
 
        assertEquals("lsid.authority", GpConfig.DEFAULT_LSID_AUTHORITY, lsid.getAuthority());
        assertEquals("lsid.namespace", GPConstants.TASK_NAMESPACE, lsid.getNamespace());
        // Note: identifier check is not exact because it's generated from  a DB sequence
        assertEquals("lsid.identifier >= 0", true, Integer.valueOf(lsid.getIdentifier()) >= 0);
        assertEquals("lsid.version", "1", lsid.getVersion());
    } 
    
}
