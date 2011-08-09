package org.genepattern.server.webapp.genomespace;

import java.net.URL;

import org.apache.log4j.Logger;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.domain.GsAccount;
import org.genepattern.server.domain.GsAccountDAO;
import org.genomespace.client.GsSession;

public class GenomeSpaceJobHelper {
    private static final Logger log = Logger.getLogger(GenomeSpaceJobHelper.class);
    
    public static boolean isGenomeSpaceFile(URL url) {
        return url.getHost().contains("genomespace.org");
    }
    
    public static String getGSToken(String gpUserId) {
        GsAccount account = new GsAccountDAO().getGsAccount(gpUserId);
        if (account == null) {
            log.error("Unable to get the GsAccount from the database for the user");
            return null;
        }
        return account.getToken();
    }
    
    public static void updateDatabase(String userId, GsSession gsSession) {
        GsAccountDAO dao = new GsAccountDAO();
        GsAccount account = new GsAccount();
        account.setGpUserid(userId);
        account.setToken(gsSession.getAuthenticationToken());
        dao.saveOrUpdate(account);
        HibernateUtil.commitTransaction();
    }
}
