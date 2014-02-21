package org.genepattern.server.eula.remote;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.eula.EulaInfo;
import org.genepattern.server.eula.InitException;
import org.genepattern.server.eula.RecordEula;
import org.genepattern.server.eula.dao.RecordEulaToDb;
import org.genepattern.server.eula.remote.PostToBroad.PostException;
import org.genepattern.server.user.User;

/**
 * Helper class which POSTs the eula info to a remote server, then records the status 
 * of the post into the local GP DB.
 * 
 * @author pcarr
 */
public class RecordEulaToRemoteServer {
    final static private Logger log = Logger.getLogger(RecordEulaToRemoteServer.class);
    private String remoteUrl;
    
    private RecordEula recordEula=null;
    /**
     * Optionally set a RecordEula callback, so that we can record success or failure of remote POST
     * into the local GP server DB. When this is not set, use the default method of saving
     * eula records to the local DB.
     * 
     * @param callback, can be null, which means, use the default implementation
     */
    public void setRecordEula(RecordEula callback) {
        this.recordEula=callback;
    }
    private RecordEula getRecordEula() {
        if (recordEula != null) {
            return recordEula;
        }
        return new RecordEulaToDb();
    }
    
    public RecordEulaToRemoteServer(final String remoteUrl) {
        this.remoteUrl=remoteUrl;
    }
    
    public void postToRemoteUrl(final User gpUser, final EulaInfo eula) {
        if (eula==null) {
            throw new IllegalArgumentException("eula==null");
        }
        if (gpUser==null) {
            throw new IllegalArgumentException("gpUser==null");
        }
        
        final String userId=gpUser.getUserId();
        final String email=gpUser.getEmail();
        log.debug("about to POST eula for userId="+userId+", lsid="+eula.getModuleLsid()+", remoteUrl="+remoteUrl);
        
        PostToBroad action = new PostToBroad();
        action.setGpUserId(userId);
        action.setEulaInfo(eula);
        //String email=getEmail(userId);
        if (email != null) {
            action.setEmail(email);
        }
        action.setRemoteUrl(remoteUrl);
        String gpUrl=ServerConfigurationFactory.instance().getGenePatternURL().toString();
        action.setGpUrl(gpUrl);
        
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

    private void afterPost(final String userId, final EulaInfo eula, final boolean success, final String errorMessage) {
        log.debug("afterPost, userId="+userId+", lsid="+eula.getModuleLsid()+", remoteUrl="+remoteUrl+", success="+success);
        if (!success) {
            log.error("failed to POST eula to remote server: "+
                "userId="+userId+", lsid="+eula.getModuleLsid()+", remoteUrl="+remoteUrl+", success="+success+", errorMessage: "+errorMessage);
        }
        try {
            RecordEula callback = getRecordEula();
            callback.updateRemoteQueue(userId, eula, remoteUrl, success);
        }
        catch (Throwable t) {
            log.error("error after POST, userId="+userId+", lsid="+eula.getModuleLsid()+", remoteUrl="+remoteUrl+", success="+success, t);
        }
        
        //Note: at the moment, we keep all records in the queue, we could delete records from the queue with the following
        // new RecordEulaToDb().removeFromQueue(userId, eula, remoteUrl);
    }

}
