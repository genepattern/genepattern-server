package org.genepattern.server.eula.remote;

import java.io.IOException;
import java.util.Date;

import org.apache.log4j.Logger;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.eula.EulaInfo;
import org.genepattern.server.eula.RecordEula;
import org.genepattern.server.eula.InitException;
import org.genepattern.server.eula.remote.PostToBroad.PostException;
import org.genepattern.server.user.User;
import org.genepattern.server.user.UserDAO;

public class RecordEulaToRemoteServer implements RecordEula {
    final static private Logger log = Logger.getLogger(RecordEulaToRemoteServer.class);

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
    
    private void beforePost() {
        //TODO: if necessary, add row to remote_eula_queue { <user>, <lsid>, <remote_url>, <date_added=now>, <status=NOT_YET_RECORDED> }'
    }
    
    private void afterPost(final boolean success, final String errorMessage) {
        if (success) {
           //TODO: save status to local DB, something like 'delete from remote_eula_queue { <user>, <lsid>, <remote_url> }'
        }
        //TODO: add logging event to DB, something like 'insert into remote_eula_log { <user>, <lsid>, <remote_url>, <date>, <status>, <message> }'
    }

    //@Override
    public void recordLicenseAgreement(final String userId, final EulaInfo eula) {
        if (eula==null) {
            throw new IllegalArgumentException("eula==null");
        }
        
        
        beforePost();
        
        PostToBroad action = new PostToBroad();
        action.setGpUserId(userId);
        action.setEulaInfo(eula);
        String email=getEmail(userId);
        if (email != null) {
            action.setEmail(email);
        }
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
        //catch (Throwable t) {
            //TODO: save status to local DB, something like 'update remote_eula { <user>, <lsid>, <remote_url>, <date>, <status=ERROR>, <status_msg=> }'
            //log.error("failed to record remote EULA for userId,lsid="+userId+","+eula.getModuleLsid(), t);
        //}
        
        afterPost(success, errorMessage);
    }

    //@Override
    public boolean hasUserAgreed(final String userId, final EulaInfo eula) throws Exception {
        throw new Exception("Not implemented!");
    }

    //@Override
    public Date getUserAgreementDate(final String userId, final EulaInfo eula) throws Exception {
        throw new Exception("Not implemented!");
    }

}
