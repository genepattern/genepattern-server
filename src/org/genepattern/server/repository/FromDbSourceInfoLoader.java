package org.genepattern.server.repository;

import java.net.MalformedURLException;

import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.repository.SourceInfo.CreatedOnServer;
import org.genepattern.server.repository.SourceInfo.FromRepo;
import org.genepattern.server.repository.SourceInfo.FromUnknown;
import org.genepattern.server.repository.SourceInfo.FromZip;
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
public class FromDbSourceInfoLoader implements SourceInfoLoader {
    final static private Logger log = Logger.getLogger(SourceInfoLoader.class);
    
    private Context serverContext=ServerConfiguration.Context.getServerContext();

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
        LSID lsid;
        try {
            lsid = new LSID(taskInfo.getLsid());
        }
        catch (MalformedURLException e) {
            log.error("taskInfo.lsid is invalid", e);
            return new FromUnknown();
        }
        
        final boolean inTransaction=HibernateUtil.isInTransaction();
        TaskInstall taskInstall=null;
        try {
            taskInstall=new RecordInstallInfoToDb().query(lsid.toString());
        }
        catch (Throwable t) {
            log.error("error getting 'task_install' record from DB for lsid="+lsid.toString(), t);
        }
        finally {
            if (!inTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
        if (taskInstall==null) {
            return new FromUnknown();
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
        return new FromUnknown();
    }

}
