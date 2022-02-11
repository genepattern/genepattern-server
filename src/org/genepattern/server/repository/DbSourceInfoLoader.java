/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.repository;

import java.net.MalformedURLException;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.repository.SourceInfo.CreatedOnServer;
import org.genepattern.server.repository.SourceInfo.FromRepo;
import org.genepattern.server.repository.SourceInfo.FromUnknown;
import org.genepattern.server.repository.SourceInfo.FromZip;
import org.genepattern.server.repository.SourceInfo.ServerOnly;
import org.genepattern.server.taskinstall.InstallInfo;
import org.genepattern.server.taskinstall.RecordInstallInfoToDb;
import org.genepattern.server.taskinstall.dao.TaskInstall;
import org.genepattern.util.LSID;
import org.genepattern.webservice.TaskInfo;

/**
 * Get the SourceInfo from the record in the DB.
 * 
 * @author pcarr
 *
 */
public class DbSourceInfoLoader implements SourceInfoLoader {
    final static private Logger log = Logger.getLogger(SourceInfoLoader.class);
    
    private GpContext serverContext=GpContext.getServerContext();

    @Override
    public SourceInfo getSourceInfo(final TaskInfo taskInfo) {
        if (taskInfo==null) {
            log.error("taskInfo==null");
            return new FromUnknown();
        }
        if (taskInfo.getLsid()==null) {
            log.error("taskInfo.lsid==null");
            return new FromUnknown();
        }
        
        final boolean inTransaction=HibernateUtil.isInTransaction();
        TaskInstall taskInstall=null;
        try {
            taskInstall=new RecordInstallInfoToDb(HibernateUtil.instance()).query(taskInfo.getLsid());
        }
        catch (Throwable t) {
            log.error("error getting 'task_install' record from DB for lsid="+taskInfo.getLsid(), t);
        }
        finally {
            if (!inTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
        if (taskInstall==null) {
            //no record in db, probably installed before updating to GP 3.6.0+
            //use the lsid to guess the source
            boolean deduceFromLsid=ServerConfigurationFactory.instance().getGPBooleanProperty(serverContext, SourceInfo.PROP_DEDUCE_FROM_LSID, false);
            if (deduceFromLsid) {
                return fromLsid(taskInfo.getLsid());
            }
            else {
                return new FromUnknown();
            }
        }

        final String source_type=taskInstall.getSourceType();
        InstallInfo.Type type=InstallInfo.Type.UNKNOWN;
        try {
            type=InstallInfo.Type.valueOf( source_type );
        }
        catch (Throwable t) {
            log.error("Incorrect source_type="+source_type, t);
        }
        
        if (type.is(InstallInfo.Type.REPOSITORY)) {
            if (taskInstall.getRepoUrl()==null) {
                log.error("type is repository, but repoUrl is not set");
                return new FromUnknown();
            }
            RepositoryInfo repositoryInfo=RepositoryInfo.getRepositoryInfoLoader(serverContext).getRepository(taskInstall.getRepoUrl());
            if (repositoryInfo != null) {
                return new FromRepo(repositoryInfo);
            }
            else {
                return new FromUnknown();
            }
        }
        else if (type.is(InstallInfo.Type.ZIP)) {
            return new FromZip();
        }
        else if (type.is(InstallInfo.Type.SERVER)) {
            return new CreatedOnServer();
        }
        else if (type.is(InstallInfo.Type.SERVER_ONLY)) {
            return new ServerOnly(type);
        }
        return new FromUnknown();
    }
    
    private SourceInfo fromLsid(final String lsidStr) {
        if (lsidStr==null || lsidStr.length()==0) {
            log.error("taskInfo.lsid is not set");
            return new FromUnknown();
        }
        
        LSID lsid=null;
        String lsidVersion=null;
        try {
            lsid=new LSID(lsidStr);
            lsidVersion=lsid.getVersion();
        }
        catch (MalformedURLException e) {
            log.error(e);
        }
    
        if (lsidStr.toLowerCase().startsWith("urn:lsid:broad.mit.edu:cancer.software.genepattern.module.")) {
            boolean isBeta=false;
            if (lsidVersion != null && lsidVersion.contains(LSID.VERSION_DELIMITER)) {
                isBeta=true;
            }
            else {
                isBeta=false;
            }
            if (!isBeta) {
                RepositoryInfo prod=
                        RepositoryInfo.getRepositoryInfoLoader(serverContext).getRepository(RepositoryInfo.GP_PROD_URL);
                return new FromRepo(prod);
            }
            else {
                // assume it's from Broad beta repository
                RepositoryInfo beta=
                        RepositoryInfo.getRepositoryInfoLoader(serverContext).getRepository(RepositoryInfo.GP_BETA_URL);
                return new FromRepo(beta);
            }
        }
        
        boolean createdOnServer=false;
        final String serverAuthority=ServerConfigurationFactory.instance().getLsidAuthority(serverContext);
        if (lsid != null && serverAuthority.equals( lsid.getAuthority() )) {
            //assume it's created/edited on this server
            createdOnServer=true;
        }
        if (createdOnServer) {
            return new CreatedOnServer();
        }
        return new FromUnknown();
    }

}
