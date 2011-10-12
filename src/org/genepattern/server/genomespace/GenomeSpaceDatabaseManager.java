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
     * Return the GenomeSpace token associated with the given GenomeSpace username
     * @param gpUserId
     * @return
     */
    public static String getGSToken(String genomeSpaceUsername) {
        GsAccount account = new GsAccountDAO().getByGPUserId(genomeSpaceUsername);
        if (account == null) {
            log.error("Unable to get the GsAccount from the database for the user");
            return null;
        }
        return account.getToken();
    }
    
    /**
     * Update the database for the given GenomeSpace username and token
     * @param userId
     * @param gsAuthenticationToken
     */
    public static void updateDatabase(String userId, String gsAuthenticationToken, String gsUsername) {
        GsAccountDAO dao = new GsAccountDAO();
        GsAccount account = new GsAccount();
        account.setGpUserId(userId);
        account.setToken(gsAuthenticationToken);
        account.setTokenTimestamp(new Date());
        account.setGsUserId(gsUsername);
        dao.saveOrUpdate(account);
        HibernateUtil.commitTransaction();
    }
}
