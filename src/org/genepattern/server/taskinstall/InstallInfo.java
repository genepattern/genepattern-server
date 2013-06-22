package org.genepattern.server.taskinstall;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

import org.apache.log4j.Logger;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.util.LSID;

/**
 * Java bean representation of installation details for a given task.
 * 
 * @author pcarr
 *
 */
public class InstallInfo {
    final static private Logger log = Logger.getLogger(InstallInfo.class);

    //hierarchical enum
    public enum Type {
        UNKNOWN(null),
        REPOSITORY(null),
        ZIP(null),
        SERVER(null),
            CREATE(SERVER),
            EDIT(SERVER),
            CLONE(SERVER),
            PROVENANCE(SERVER)
        ;
        
        private final Type parent;
        private Type(Type parent) {
            this.parent=parent;
        }

        /**
         * So that CREATE.is(SERVER) == true ...
         * So that CREATE.is(ZIP) == false ...
         * 
         * @param other
         * @return
         */
        public boolean is(Type other) {
            if (other==null) {
                return false;
            }
            for (Type type = this; type != null; type = type.parent) {
                if (other==type) {
                    return true;
                }
            }
            return false;
        }
    }
    
    final private Type type;
    private LSID lsid=null;
    private String userId;
    private Date dateInstalled;
    private URL repositoryUrl;
    private GpFilePath zipFile;
    private LSID prevLsid;
    private GpFilePath libdir;

    public InstallInfo() {
        this(Type.UNKNOWN);
    }
    public InstallInfo(final Type type) {
        this.type=type;
    }
    
    public Type getType() {
        return type;
    }

    public void setLsidFromString(final String lsidStr) throws MalformedURLException {
        if (lsidStr==null) {
            throw new IllegalArgumentException("lsidStr==null");
        }
        setLsid(new LSID(lsidStr));
    }
    
    public void setLsid(final LSID newLsid) {
        if (this.lsid != null) {
            log.error("Don't change the lsid!");
            //throw new IllegalArgumentException("Don't change the lsid!");
        }
        this.lsid=newLsid;
    }
    public LSID getLsid() {
        return lsid;
    }
    public void setUserId(final String userId) {
        this.userId=userId;
    }
    public String getUserId() {
        return userId;
    }
    
    public void setDateInstalled(final Date dateInstalled) {
        this.dateInstalled=dateInstalled;
    }
    public Date getDateInstalled() {
        return dateInstalled;
    }

    public void setRepositoryUrl(final URL repositoryUrl) {
        this.repositoryUrl=repositoryUrl;
    }
    public URL getRepositoryUrl() {
        return repositoryUrl;
    }
    
    public GpFilePath getZipFile() {
        return zipFile;
    }

    public LSID getPrevLsid() {
        return prevLsid;
    }

    public GpFilePath getLibdir() {
        return libdir;
    }

}
