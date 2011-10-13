package org.genepattern.server.genomespace;

import java.util.Map.Entry;

import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.util.GPConstants;

public class GenomeSpaceLoginManager {
    private static Logger log = Logger.getLogger(GenomeSpaceLoginManager.class);
    
    public static boolean loginFromToken(String gp_username, HttpSession httpSession) throws GenomeSpaceException {
        Context context = Context.getContextForUser(gp_username);
        String genomeSpaceEnvironment = GenomeSpaceClientFactory.getGenomeSpaceEnvironment(context);
        
        if (GenomeSpaceDatabaseManager.isGPAccountAssociated(gp_username)) {
            String token = GenomeSpaceDatabaseManager.getGSToken(gp_username);
            GenomeSpaceLogin login = GenomeSpaceClientFactory.getGenomeSpaceClient().submitLogin(genomeSpaceEnvironment, token);
            if (login == null) return false;
            setSessionAttributes(login, httpSession);
            return true;
        }
        return false;
    }
    
    public static boolean loginFromUsername(String env, String genomeSpaceUsername, String genomeSpacePassword, HttpSession httpSession) throws GenomeSpaceException {
        GenomeSpaceLogin login = GenomeSpaceClientFactory.getGenomeSpaceClient().submitLogin(env, genomeSpaceUsername, genomeSpacePassword);
        if (login == null) return false;
        setSessionAttributes(login, httpSession);
        return true;
    }
    
    private static void setSessionAttributes(GenomeSpaceLogin login, HttpSession httpSession) {
        // Set attributes from login in the GenePattern session
        for(Entry<String,Object> entry : login.getAttributes().entrySet()) {
            httpSession.setAttribute(entry.getKey(), entry.getValue()); 
        }
        String gpUsername = (String) httpSession.getAttribute(GPConstants.USERID);
        log.error("Writing to database: " + gpUsername);
        GenomeSpaceDatabaseManager.updateDatabase(gpUsername, login.getAuthenticationToken(), login.getUsername(), login.getEmail());
    }
}
