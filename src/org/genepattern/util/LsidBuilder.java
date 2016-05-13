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
 * Initializer defaults: 
 *     authority from GpConfig
 *     namespace is GPConfig.TASK_NAMESPACE
 *     identity from next DB sequence for namespace
 *     version next major version, starting at 1, not already in the db.
 * 
 * @author pcarr
 *
 */
public class LsidBuilder {
    private static final String INITIAL_VERSION="1";

    private String authority=null;
    private String namespace=GPConstants.TASK_NAMESPACE;
    private String identifier=null;
    private String version=INITIAL_VERSION;
    
    private HibernateSessionManager _mgr=null;
    private GpConfig _gpConfig=null;
    private GpContext _gpContext=null;

    protected LsidBuilder mgrDefault() {
        if (_mgr == null) {
            return mgr(HibernateUtil.instance());
        }
        return this;
    }

    public LsidBuilder mgr(HibernateSessionManager mgr) {
        this._mgr=mgr;
        return this;
    }
    
    protected LsidBuilder gpConfigDefault() {
        if (_gpConfig == null) {
            return gpConfig(ServerConfigurationFactory.instance());
        }
        return this;
    }
    
    public LsidBuilder gpConfig(final GpConfig gpConfig) {
        this._gpConfig=gpConfig;
        return this;
    }

    protected LsidBuilder gpContextDefault() {
        if (_gpContext == null) {
            return gpContext(GpContext.getServerContext());
        }
        return this;
    }

    public LsidBuilder gpContext(final GpContext gpContext) {
        this._gpContext=gpContext;
        return this;
    }
    
    public LsidBuilder authorityFromConfig() {
        gpConfigDefault();
        gpContextDefault();
        return authorityFromConfig(_gpConfig, _gpContext);
    }
    public LsidBuilder authorityFromConfig(final GpConfig gpConfig, final GpContext gpContext) {
        authority=gpConfig.getLsidAuthority(gpContext);
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
    
    public LsidBuilder identifierFromDb() throws DbException {
        mgrDefault();
        gpConfigDefault();
        return identifierFromDb(_mgr, _gpConfig, namespace);
    }
    
    public LsidBuilder identifier(final String identifier) {
        this.identifier=identifier;
        return this;
    }

    public LsidBuilder identifierFromDb(final HibernateSessionManager mgr, final GpConfig gpConfig, final String namespace) throws DbException {
        //this.identifier=""+LSIDManager.getNextLSIDIdentifier(mgr, gpConfig, namespace);
        return identifierFromDb(mgr, gpConfig.getDbVendor(), namespace);
    }
    public LsidBuilder identifierFromDb(final HibernateSessionManager mgr, final String dbVendor, final String namespace) throws DbException {
        final String sequenceName=LSIDManager.getSequenceName(namespace);
        this.identifier=""+HibernateUtil.getNextSequenceValue(mgr, dbVendor, sequenceName); 
        return this;
    }
    
    public LsidBuilder version(final String version) {
        this.version=version;
        return this;
    } 
    
    public String getNamespace() {
        if (namespace==null) {
            // assume it's a new task lsid
            return GPConstants.TASK_NAMESPACE;
        }
        return namespace;
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
            authorityFromConfig();
        }
        lsid.setAuthority(authority);
        lsid.setNamespace(namespace);
        if (identifier==null) {
            // get the next one from the database
            identifierFromDb();
        }
        lsid.setIdentifier(identifier);
        lsid.setVersion(version);
        return lsid;
    }

}
