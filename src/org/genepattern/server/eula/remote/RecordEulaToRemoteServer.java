package org.genepattern.server.eula.remote;

import java.io.IOException;
import java.util.Date;

import org.apache.log4j.Logger;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.eula.EulaInfo;
import org.genepattern.server.eula.RecordEula;
import org.genepattern.server.eula.InitException;
import org.genepattern.server.eula.dao.RecordEulaToDb;
import org.genepattern.server.eula.remote.PostToBroad.PostException;
import org.genepattern.server.user.User;
import org.genepattern.server.user.UserDAO;

public class RecordEulaToRemoteServer implements RecordEula {
    final static private Logger log = Logger.getLogger(RecordEulaToRemoteServer.class);
    private String remoteUrl;
    
    public RecordEulaToRemoteServer(final String remoteUrl) {
        this.remoteUrl=remoteUrl;
    }

    private String getEmail(final String userId) {
        //HACK: requires active local DB, with valid users 
        final boolean inTransaction=HibernateUtil.isInTransaction();
        try {
            UserDAO dao=new UserDAO();
            User user=dao.findById(userId);
            if (user != null) {
                return user.getEmail();
            }
        }
        catch (Throwable t) {
            log.error("Error getting User instance for userId="+userId, t);
        }
        finally {
            if (!inTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
        return "";
    }
    
    private void beforePost(final String userId, EulaInfo eula) {
        //don't do anything before, we updated the DB when recording locally
        log.debug("about to POST eula for userId="+userId+", lsid="+eula.getModuleLsid()+", remoteUrl="+remoteUrl);
    }
    
    private void afterPost(final String userId, final EulaInfo eula, final boolean success, final String errorMessage) {
        log.debug("afterPost, userId="+userId+", lsid="+eula.getModuleLsid()+", remoteUrl="+remoteUrl+", success="+success);
        try {
            new RecordEulaToDb().updateRemoteQueue(userId, eula, remoteUrl, success);
            //TODO: add logging event to DB, something like 'insert into remote_eula_log { <user>, <lsid>, <remote_url>, <date>, <status>, <message> }'
        }
        catch (Throwable t) {
            log.error("error after POST, userId="+userId+", lsid="+eula.getModuleLsid()+", remoteUrl="+remoteUrl+", success="+success, t);
        }
        
        //Note: at the moment, we keep all records in the queue, we could delete records from the queue with the following
        // new RecordEulaToDb().removeFromQueue(userId, eula, remoteUrl);
    }

    //@Override
    public void recordLicenseAgreement(final String userId, final EulaInfo eula) {
        if (eula==null) {
            throw new IllegalArgumentException("eula==null");
        }
        
        
        beforePost(userId, eula);
        
        PostToBroad action = new PostToBroad();
        action.setGpUserId(userId);
        action.setEulaInfo(eula);
        String email=getEmail(userId);
        if (email != null) {
            action.setEmail(email);
        }
        action.setRemoteUrl(remoteUrl);
        //Note: gpUrl is initialized in PostToBroad
        
        boolean success=false;
        String errorMessage=null;
        try {
            action.doPost();
            success=true;
            log.debug("success");
         }
        catch (InitException e) {
            errorMessage=e.getLocalizedMessage();
            log.error(e);
        }
        catch (IOException e) {
            errorMessage=e.getLocalizedMessage();
            log.error(e);
        }
        catch (PostException e) {
            errorMessage=e.getLocalizedMessage();
            log.error(e);
            //also, record this to the DB
        }
        
        afterPost(userId, eula, success, errorMessage);
    }

    //@Override
    public boolean hasUserAgreed(final String userId, final EulaInfo eula) throws Exception {
        throw new Exception("Not implemented!");
    }

    //@Override
    public Date getUserAgreementDate(final String userId, final EulaInfo eula) throws Exception {
        throw new Exception("Not implemented!");
    }

    //@Override
    public void addToRemoteQueue(final String userId, final EulaInfo eula, final String remoteUrl) throws Exception {
        // TODO Auto-generated method stub
        throw new Exception("Not implemented!"); 
    }

}
