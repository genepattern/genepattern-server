package org.genepattern.server.genomespace;

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
        GsAccount account = new GsAccountDAO().getGsAccount(genomeSpaceUsername);
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
    public static void updateDatabase(String userId, String gsAuthenticationToken) {
        GsAccountDAO dao = new GsAccountDAO();
        GsAccount account = new GsAccount();
        account.setGpUserid(userId);
        account.setToken(gsAuthenticationToken);
        dao.saveOrUpdate(account);
        HibernateUtil.commitTransaction();
    }
}
