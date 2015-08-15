/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.eula;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.eula.dao.RecordEulaToDb;
import org.genepattern.server.eula.remote.RecordEulaToRemoteServerAsync;
import org.genepattern.server.config.Value;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.user.User;
import org.genepattern.server.user.UserDAO;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.TaskInfo;


/**
 * The default method for recording EULA in the local GP server.
 * 
 * It saves a local record, and adds an entry to the local db table, 'eula_remote_queue'.
 * This is done in a single transaction, so that if we are not able to add the entry to the 'eula_remote_queue',
 * the entire transaction will fail.
 * 
 * The actual remote POST is done asynchronously in a different thread.
 * 
 * The following configuration properties control remote POST
 *     # if there is no setting, use the default value, compiled in the source code
 *     # set the remoteUrl
 *     org.genepattern.server.eula.EulaManager.remoteUrl: "http://vgpweb01.broadinstitute.org:3000/eulas"
 *     # if the remoteUrl is an empty list, it means don't POST
 *     org.genepattern.server.eula.EulaManager.remoteUrl: []
 *     # can be a list, which means post to more than one remote URL
 *     org.genepattern.server.eula.EulaManager.remoteUrl: [ "http://vgpweb01.broadinstitute.org:3000/eulas", "http://eulas.genepattern.org/eulas" ]     
 * 
 * @author pcarr
 *
 */
public class RecordEulaDefault implements RecordEula {
    final static private Logger log = Logger.getLogger(RecordEulaDefault.class);
    /** the name of the property to use in the config.yaml file */
    final static public String PROP_REMOTE_URL="org.genepattern.server.eula.EulaManager.remoteUrl";
    /** the default value for the remoteUrl */
    final static public String REMOTE_URL_DEFAULT="http://eulas.genepattern.org/eulas";
    /** the default remoteUrl, on,y accessible from behind the Broad's firewall. */
    final static public String REMOTE_URL_PRIVATE="http://vgpweb01.broadinstitute.org:3000/eulas";
    
    private final HibernateSessionManager mgr;
    private RecordEula local;

    public RecordEulaDefault(final HibernateSessionManager mgr) {
        this.mgr=mgr;
        this.local=new RecordEulaToDb(this.mgr);
    }
    
    public RecordEulaDefault(final HibernateSessionManager mgr, final RecordEula localDb) {
        this.mgr=mgr;
        this.local=localDb;
    }
    
    private User getUser(final String userId) {
        //this method requires active local DB, with valid users 
        final boolean inTransaction=mgr.isInTransaction();
        try {
            UserDAO dao=new UserDAO(mgr);
            User user=dao.findById(userId);
            return user;
        }
        catch (Throwable t) {
            log.error("Error getting User instance for userId="+userId, t);
        }
        finally {
            if (!inTransaction) {
                mgr.closeCurrentSession();
            }
        }
        
        return null;
    }

    private GpContext getContextForEula(final String userId, final EulaInfo eula) {
        @SuppressWarnings("deprecation")
        GpContext eulaContext=GpContext.getContextForUser(userId);
        TaskInfo taskInfo = new TaskInfo();
        taskInfo.giveTaskInfoAttributes().put(GPConstants.LSID, eula.getModuleLsid());
        taskInfo.setName(eula.getModuleName());
        eulaContext.setTaskInfo(taskInfo);
        return eulaContext;
    }

    private List<String> getRemoteUrls(final String userId, final EulaInfo eula) {
        GpContext eulaContext=getContextForEula(userId, eula);
        Value val=ServerConfigurationFactory.instance().getValue(eulaContext, PROP_REMOTE_URL);
        if (val==null) {
            // null means, use the default value
            //    as opposed to an empty list, which means, don't post
            List<String> rval=new ArrayList<String>();
            rval.add(REMOTE_URL_DEFAULT);
            return rval;
        }
        return val.getValues();
    }

    public void recordLicenseAgreement(final String userId, final EulaInfo eula) throws Exception {
        log.debug("recordLicenseAgreement("+userId+","+eula.getModuleLsid()+")");
        
        List<String> remoteUrls=getRemoteUrls(userId, eula);

        //within one transaction,
        boolean inTransaction=mgr.isInTransaction();
        try {
            mgr.beginTransaction();
            //1) first, record local record,
            local.recordLicenseAgreement(userId, eula);
            //2) add DB entry to the 'eula_remote_queue'
            for (String remoteUrl : remoteUrls) {
                local.addToRemoteQueue(userId, eula, remoteUrl);
            }
            if (!inTransaction) {
                mgr.commitTransaction();
            }
            else {
                log.debug("committing hibernate transaction, even though it was started before this method");
                mgr.commitTransaction();
            }
        }
        catch (Throwable t) {
            String message="Error recording eula to local GP server: "+t.getLocalizedMessage();
            log.error(message,t);
            mgr.rollbackTransaction();
            throw new Exception(message);
        }
        finally {
            if (!inTransaction) {
                mgr.closeCurrentSession();
            }
        }

        //2) schedule asynchronous POST of remote record to each remoteUrl
        // need a valid User object
        User user = getUser(userId);
        RecordEulaToRemoteServerAsync remote=new RecordEulaToRemoteServerAsync(local);
        for (String remoteUrl : remoteUrls) {
            remote.postToRemoteUrl(user, eula, remoteUrl);
        }
    }

    public boolean hasUserAgreed(final String userId, final EulaInfo eula) throws Exception {
        log.debug("delegating to local.hasUserAgreed");
        return local.hasUserAgreed(userId, eula);
    }

    public Date getUserAgreementDate(final String userId, final EulaInfo eula) throws Exception {
        log.debug("delegating to local.getUserAgreementDate");
        return local.getUserAgreementDate(userId, eula);
    }

    public void addToRemoteQueue(final String userId, final EulaInfo eula, final String remoteUrl) throws Exception {
        log.debug("delegating to local.addToRemoteQueue");
        local.addToRemoteQueue(userId, eula, remoteUrl);
    }

    public void updateRemoteQueue(String userId, EulaInfo eula, String remoteUrl, boolean success) throws Exception {
        log.debug("delegating to local.updateRemoteQueue");
        local.updateRemoteQueue(userId, eula, remoteUrl, success);
    }

}
