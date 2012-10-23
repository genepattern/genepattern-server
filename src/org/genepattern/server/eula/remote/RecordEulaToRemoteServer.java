package org.genepattern.server.eula.remote;

import java.util.Date;

import org.apache.log4j.Logger;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.eula.EulaInfo;
import org.genepattern.server.eula.RecordEula;
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

    //@Override
    public void recordLicenseAgreement(final String userId, final EulaInfo eula) throws Exception {
        if (eula==null) {
            throw new IllegalArgumentException("eula==null");
        }
        //synchronous implementation
        PostToBroad action = new PostToBroad();
        action.setGpUserId(userId);
        action.setEulaInfo(eula);
        String email=getEmail(userId);
        if (email != null) {
            action.setEmail(email);
        }
        //Note: gpUrl is initialized in PostToBroad
        try {
            action.postRemoteRecord();
            log.debug("success");
        }
        catch (Throwable t) {
            log.error("failed to record remote EULA for userId,lsid="+userId+","+eula.getModuleLsid(), t);
        }
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
