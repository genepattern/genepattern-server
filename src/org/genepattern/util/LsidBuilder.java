package org.genepattern.util;
import java.net.MalformedURLException;

import org.genepattern.server.DbException;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.genepattern.LSIDManager;


/**
 * Helper class to build an LSID with a fluent pattern. Template:
 * 
 *     lsid=urn:lsid:{lsid.authority}:{namespace}:{identifier}:{version}
 * 
 * Initializes defaults: 
 *     authority from GpConfig
 *     namespace is GPConfig.TASK_NAMESPACE
 *     identity from next DB sequence for namespace
 *     version next major version, starting at 1, not already in the db.
 * 
 * @author pcarr
 *
 */
public class LsidBuilder {
    public static enum VersionIncrement {
        NEXT_MAJOR,
        NEXT_MINOR,
        NEXT_PATCH
    }

    private static final String initialVersion="1";
    private HibernateSessionManager mgr=null;
    private GpConfig gpConfig=null;
    private GpContext gpContext=null;
    private String authority=null;
    private String namespace=null;
    private String identifier=null;
    private String version=null;

    public LsidBuilder mgr(final HibernateSessionManager mgr) {
        this.mgr=mgr;
        return this;
    }
    public LsidBuilder gpConfig(final GpConfig gpConfig) {
        this.gpConfig=gpConfig;
        return this;
    }
    public LsidBuilder gpContext(final GpContext gpContext) {
        this.gpContext=gpContext;
        return this;
    }

    public LsidBuilder authority(final String authority) {
        this.authority=authority;
        return this;
    }
    public LsidBuilder namespace(final String namespace) {
        this.namespace=namespace;
        return this;
    }
    public LsidBuilder identifier(final String identifier) {
        this.identifier=identifier;
        return this;
    }
    public LsidBuilder version(final String version) {
        this.version=version;
        return this;
    } 

    private HibernateSessionManager getMgr() {
        if (mgr==null) {
            mgr=HibernateUtil.instance();
        }
        return mgr;
    }

    private GpConfig getGpConfig() {
        if (gpConfig==null) {
            gpConfig=ServerConfigurationFactory.instance();
        }
        return gpConfig;
    }

    private GpContext getGpContext() {
        if (gpContext==null) {
            gpContext=GpContext.getServerContext();
        }
        return gpContext;
    }
    
    /**
     * not thread-safe, for best results call this once per instance
     * @return
     * @throws MalformedURLException
     * @throws DbException
     */
    public LSID build() throws MalformedURLException, DbException { 
        LSID lsid=new LSID("");
        if (authority==null) {
            authority=getGpConfig().getLsidAuthority(getGpContext());
        }
        lsid.setAuthority(authority);
        if (namespace==null) {
            // assume it's a new task lsid
            namespace=GPConstants.TASK_NAMESPACE;
        }
        lsid.setNamespace(namespace);
        if (identifier==null) {
            // get the next one from the database
            identifier=""+LSIDManager.getNextLSIDIdentifier(getMgr(), gpConfig, namespace);
        }
        lsid.setIdentifier(identifier);
        if (version==null) {
            version=initialVersion;
        }
        lsid.setVersion(version);
        return lsid;
    }

}
