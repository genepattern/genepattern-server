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
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.util.LsidBuilder;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 * LSID Template:
 *     lsid=urn:lsid:{lsid.authority}:{namespace}:{identifier}:{version}
 * 
 * @author pcarr
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestGetNextTaskLsid {
    private static HibernateSessionManager mgr;
    private GpConfig gpConfig;
    private GpContext gpContext;
    
    // use Generic Sequence table for testing
    private final static String customDbVendor="MOCK_JUNIT_DB_VENDOR";
    
    @BeforeClass
    public static void beforeClass() throws ExecutionException, DbException {
        mgr=DbUtil.getTestDbSession();
    }
    
    protected String initCustomNamespace() throws DbException {
        when(gpConfig.getDbVendor()).thenReturn(customDbVendor);

        String customNamespace;
        String customSeqName;
        customNamespace="junit." + Demo.randomSequence(8);
        customSeqName=customNamespace+"_seq";
        HibernateUtil.createSequenceGeneric(mgr, customSeqName);
        return customNamespace;
    }
    // increment the next lsid identifier for the given customNamespace,
    // helper method for test setup
    protected void incrementIdentifier(final String customNamespace) throws DbException {
        LSIDManager.getNextLSIDIdentifier(mgr, gpConfig, customNamespace);
    }
    
    @Before
    public void setUp() {
        gpConfig=Demo.gpConfig();
        gpContext=Demo.serverContext;
        
        // validate test setup
        assertEquals("Demo.gpConfig.dbVendor", "HSQL", gpConfig.getDbVendor());
        assertEquals("Demo.gpConfig.lsidAuthority", GpConfig.DEFAULT_LSID_AUTHORITY, 
                gpConfig.getLsidAuthority(gpContext));
    }

    @Test
    public void test_01_requestedLsid_isNull() throws ExecutionException, RemoteException, MalformedURLException {
        // version (null)->"1"
        final String requestedLSID=null;
        assertEquals(
                // expected
                new LSID("urn:lsid:broad-cancer-genomics:genepatternmodules:1:1"),
                // actual
                LSIDManager.getNextTaskLsid(mgr, gpConfig, gpContext, requestedLSID)
                );
    }
    
    @Test
    public void test_02_requestedLsid_isEmpty() throws ExecutionException, RemoteException, MalformedURLException {
        // version ""->"1"
        final String requestedLSID="";
        assertEquals(
                // expected
                new LSID("urn:lsid:broad-cancer-genomics:genepatternmodules:2:1"),
                // actual
                LSIDManager.getNextTaskLsid(mgr, gpConfig, gpContext, requestedLSID)
                );
    }
    
    @Test
    public void test_03_requestedLsid_matches_lsidAuthority() throws MalformedURLException, RemoteException {
        // version 1->2
        final String requestedLSID="urn:lsid:broad-cancer-genomics:genepatternmodules:1:1";
        assertEquals(
                // expected
                new LSID("urn:lsid:broad-cancer-genomics:genepatternmodules:1:2"),
                // actual
                LSIDManager.getNextTaskLsid(mgr, gpConfig, gpContext, requestedLSID)
                );
    }
    
    @Test
    public void customSequenceName() {
        final String namespace="junit." + Demo.randomSequence(8);
        assertEquals(namespace+"_seq", LSIDManager.getSequenceName(namespace));
    }
    
    @Test
    public void customNamespace_genericDbSequence() throws MalformedURLException, RemoteException, DbException {
        final String customNamespace=initCustomNamespace();
        // only works for non-HSQL non-ORACLE dbVendor
        when(gpConfig.getDbVendor()).thenReturn(customDbVendor);
        assertEquals(
                // expected
                new LSID("urn:lsid:broad-cancer-genomics:"+customNamespace+":1:1"),
                // actual 
                LSIDManager.getNextLsid(mgr, gpConfig, gpContext, customNamespace, null)
                );
    }

    @Test
    public void testLsidEmptyInitializer() throws MalformedURLException {
        LSID lsid=new LSID(""); //urn:lsid::::");
        assertEquals("lsid.authority", "", lsid.getAuthority());
        assertEquals("lsid.namespace", "", lsid.getNamespace());
        assertEquals("lsid.identifier", "", lsid.getIdentifier());
        assertEquals("lsid.version", "", lsid.getVersion());
    }
    
    @Test
    public void test_04_LsidBuilderDefaults() throws MalformedURLException, DbException {
        // initialize with defaults (Note: need to account for test order because a sequence is used to generate the next identifier)
        LSID lsid=new LsidBuilder()
                .mgr(mgr)
                .gpConfig(gpConfig)
                .gpContext(gpContext)
        .build();
        
        // if authority not set, use GpConfig
        assertEquals("lsid.authority", GpConfig.DEFAULT_LSID_AUTHORITY, lsid.getAuthority());
        assertEquals("lsid.namespace", GPConstants.TASK_NAMESPACE, lsid.getNamespace());
        assertEquals("lsid.identifier", "3", lsid.getIdentifier());
        assertEquals("initialVersion", "1", lsid.getVersion());
    }
    
    
    // first round of tests, 
    //     use custom namespace,
    //     request new lsid identifier (e.g. get next sequence value for custom namespace)
    
    // case 1: new major version
    @Test
    public void lsidBuilder_newLsid_customNamespace() throws MalformedURLException, DbException {
        final String customNamespace=initCustomNamespace();
        LSID lsid=new LsidBuilder()
            .mgr(mgr)
            .gpConfig(gpConfig)
            .gpContext(gpContext)
            .namespace(customNamespace)
        .build();
        
        assertEquals("lsid.authority", GpConfig.DEFAULT_LSID_AUTHORITY, lsid.getAuthority());
        assertEquals("lsid.namespace", customNamespace, lsid.getNamespace());
        assertEquals("lsid.identifier", "1", lsid.getIdentifier());
        assertEquals("initialVersion", "1", lsid.getVersion());
    }
    
    @Test
    public void case_2_new_minor_version() throws DbException, MalformedURLException {
        final String customNamespace=initCustomNamespace();
        LSID lsid=new LsidBuilder()
            .mgr(mgr)
            .gpConfig(gpConfig)
            .gpContext(gpContext)
            .namespace(customNamespace)
            .version("0.1")
        .build();
        
        assertEquals("lsid.authority", GpConfig.DEFAULT_LSID_AUTHORITY, lsid.getAuthority());
        assertEquals("lsid.namespace", customNamespace, lsid.getNamespace());
        assertEquals("lsid.identifier", "1", lsid.getIdentifier());
        assertEquals("initialVersion", "0.1", lsid.getVersion());
    }
    
    @Ignore @Test
    public void nextMajor_requestedIsNull() throws DbException, MalformedURLException, RemoteException {
        final String requestedLsid=null;
        final String customNamespace=initCustomNamespace();
        // only works for non-HSQL non-ORACLE dbVendor
        when(gpConfig.getDbVendor()).thenReturn(customDbVendor);
        assertEquals(
                // expected
                new LSID("urn:lsid:broad-cancer-genomics:"+customNamespace+":1:1"),
                // actual 
                LSIDManager.getNextLsid(mgr, gpConfig, gpContext, customNamespace, requestedLsid)
                );        
    }

    @Test
    public void nextMajor_requestedIsEmpty() throws DbException, MalformedURLException, RemoteException {
        final String requestedLsid="";
        final String customNamespace=initCustomNamespace();
        // only works for non-HSQL non-ORACLE dbVendor
        when(gpConfig.getDbVendor()).thenReturn(customDbVendor);
        assertEquals(
                // expected
                new LSID("urn:lsid:broad-cancer-genomics:"+customNamespace+":1:1"),
                // actual 
                LSIDManager.getNextLsid(mgr, gpConfig, gpContext, customNamespace, requestedLsid)
                );        
    }
    
    @Test
    public void nextMajor_requestedIsMajor() throws DbException, MalformedURLException, RemoteException {
        
        //final String requestedLsid="";
        final String customNamespace=initCustomNamespace();
        // only works for non-HSQL non-ORACLE dbVendor
        when(gpConfig.getDbVendor()).thenReturn(customDbVendor);
        
        //String initialLsid="";
        String requestedLsid="urn:lsid:broad-cancer-genomics:"+customNamespace+":1:1";
        assertEquals(
                // expected
                new LSID("urn:lsid:broad-cancer-genomics:"+customNamespace+":1:2"),
                // actual 
                LSIDManager.getNextLsid(mgr, gpConfig, gpContext, customNamespace, requestedLsid)
                );        
        requestedLsid="urn:lsid:broad-cancer-genomics:"+customNamespace+":1:2";
        assertEquals(
                // expected
                new LSID("urn:lsid:broad-cancer-genomics:"+customNamespace+":1:3"),
                // actual 
                LSIDManager.getNextLsid(mgr, gpConfig, gpContext, customNamespace, requestedLsid)
                );        
    }
    
    // use-cases:
    /*
Get nextTaskLsid when
    ... new module minor version
    ... new module major version
    ... existing module, increment default
    ... existing module, next major
        1->2
        1.1 -> 2
    ... existing module, next minor
    ... existing module, next patch
    
    next
        ""->1
        1->2
        1.1->1.2
        
    nextUp
        ""->1 (warn)
        1->2 (warn)
        1.2 -> 2
        1.1.2 -> 1.2

    nextDown
        "" -> 0.1
        1 -> 1.1
        1.1 -> 1.1.1
        
                      
     */
    
        // from "" -> "0.1"  (create new LSID)
        
        // Given: 
        //     requestedLSID
        //     versionIncrement
        //     namespace, default=TASK_NAMESPACE (TODO: sic)
        
        
        // when requestedLSID is null or empty and ...
        // ... increment is NOT_SET
        // ... increment is major
        // ... increment is minor
        // ... increment is patch
    
        //  *     lsid=urn:lsid:{lsid.authority}:{namespace}:{identifier}:{version}
//    public static class LsidBuilder {
//        
//        // not thread-safe, for best results call this once per instance
//        public LSID build() throws MalformedURLException, DbException { 
//            LSID lsid=new LSID("");
//            if (authority==null) {
//                authority=getGpConfig().getLsidAuthority(getGpContext());
//            }
//            lsid.setAuthority(authority);
//            if (namespace==null) {
//                // assume it's a new task lsid
//                namespace=GPConstants.TASK_NAMESPACE;
//            }
//            lsid.setNamespace(namespace);
//            if (identifier==null) {
//                // get the next one from the database
//                identifier=""+LSIDManager.getNextLSIDIdentifier(getMgr(), gpConfig, namespace);
//            }
//            lsid.setIdentifier(identifier);
//            if (version==null) {
//                version=initialVersion;
//            }
//            lsid.setVersion(version);
//            return lsid;
//        }
//
//        private static final String initialVersion="1";
//        private HibernateSessionManager mgr=null;
//        private GpConfig gpConfig=null;
//        private GpContext gpContext=null;
//        private String authority=null;
//        private String namespace=null;
//        private String identifier=null;
//        private String version=null;
//        
//        public LsidBuilder mgr(final HibernateSessionManager mgr) {
//            this.mgr=mgr;
//            return this;
//        }
//        public LsidBuilder gpConfig(final GpConfig gpConfig) {
//            this.gpConfig=gpConfig;
//            return this;
//        }
//        public LsidBuilder gpContext(final GpContext gpContext) {
//            this.gpContext=gpContext;
//            return this;
//        }
//        
//        public LsidBuilder authority(final String authority) {
//            this.authority=authority;
//            return this;
//        }
//        public LsidBuilder namespace(final String namespace) {
//            this.namespace=namespace;
//            return this;
//        }
//        public LsidBuilder identifier(final String identifier) {
//            this.identifier=identifier;
//            return this;
//        }
//        public LsidBuilder version(final String version) {
//            this.version=version;
//            return this;
//        } 
//
//        private HibernateSessionManager getMgr() {
//            if (mgr==null) {
//                mgr=HibernateUtil.instance();
//            }
//            return mgr;
//        }
//        
//        private GpConfig getGpConfig() {
//            if (gpConfig==null) {
//                gpConfig=ServerConfigurationFactory.instance();
//            }
//            return gpConfig;
//        }
//        
//        private GpContext getGpContext() {
//            if (gpContext==null) {
//                gpContext=GpContext.getServerContext();
//            }
//            return gpContext;
//        }
//
//    }

}
