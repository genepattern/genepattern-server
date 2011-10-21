package org.genepattern.server.genomespace;

import java.util.Date;

import org.apache.log4j.Logger;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.domain.GsAccount;
import org.genepattern.server.domain.GsAccountDAO;

/**
 * Managers all GenomeSpace interaction with the GenePattern database
 * @author tabor
 *
 */
public class GenomeSpaceDatabaseManager {
    private static final Logger log = Logger.getLogger(GenomeSpaceDatabaseManager.class);
    
    /**
     * Return the GenomeSpace token associated with the given GenePattern username
     * @param gpUserId
     * @return
     */
    public static String getGSToken(String gpUsername) {
        GsAccount account = new GsAccountDAO().getByGPUserId(gpUsername);
        if (account == null) {
            log.error("Unable to get the GsAccount from the database for the user");
            return null;
        }
        return account.getToken();
    }
    
    public static String getGSUsername(String gpUsername) {
        GsAccount account = new GsAccountDAO().getByGPUserId(gpUsername);
        if (account == null) {
            log.error("Unable to get the GsAccount from the database for the user");
            return null;
        }
        return account.getGsUserId();
    }
    
    public static boolean isGPAccountAssociated(String gpUsername) {
        GsAccount account = new GsAccountDAO().getByGPUserId(gpUsername);
        if (account == null) return false;
        if (account.getGsUserId() == null) {
            return false;
        }
        else {
            return true;
        }
    }
    
    public static boolean isGSAccountAssociated(String gsUsername) {
        GsAccount account = new GsAccountDAO().getByGSUserId(gsUsername);
        if (account == null) return false;
        if (account.getGpUserId() == null) {
            return false;
        }
        else {
            return true;
        }
    }
    
    public static String getGPUsername(String gsUsername) {
        GsAccount account = new GsAccountDAO().getByGSUserId(gsUsername);
        if (account == null) return null;
        return account.getGpUserId();
    }
    
    /**
     * Update the database for the given GenomeSpace username and token
     * @param userId
     * @param gsAuthenticationToken
     */
    public static void updateDatabase(String userId, String gsAuthenticationToken, String gsUsername, String email) {
        GsAccountDAO dao = new GsAccountDAO();
        GsAccount account = new GsAccountDAO().getByGPUserId(userId);
        if (account == null) account = new GsAccount();
        account.setGpUserId(userId);
        account.setToken(gsAuthenticationToken);
        account.setTokenTimestamp(new Date());
        account.setGsUserId(gsUsername);
        account.setEmail(email);
        dao.saveOrUpdate(account);
        HibernateUtil.commitTransaction();
    }
}
