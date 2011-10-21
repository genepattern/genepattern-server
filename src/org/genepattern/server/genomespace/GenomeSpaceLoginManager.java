package org.genepattern.server.genomespace;

import java.util.Map.Entry;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.genepattern.server.UserAccountManager;
import org.genepattern.server.auth.AuthenticationException;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.util.GPConstants;

public class GenomeSpaceLoginManager {
    private static Logger log = Logger.getLogger(GenomeSpaceLoginManager.class);
    
    public static String GS_SESSION_KEY = "GS_SESSION";
    public static String GS_USER_KEY = "GS_USER";
    public static String GS_TOKEN_KEY = "GS_TOKEN";
    public static String GS_EMAIL_KEY = "GS_EMAIL";
    public static String GS_DIRECTORIES_KEY = "GS_DIRECTORIES";
    public static String GS_FILE_METADATAS = "GS_FILE_METADATAS";
    
    public static boolean loginFromSession(HttpSession httpSession) throws GenomeSpaceException {
        String token = (String) httpSession.getAttribute(GS_TOKEN_KEY);
        String gsUsername = (String) httpSession.getAttribute(GS_USER_KEY);
        String gp_username = (String) httpSession.getAttribute(GPConstants.USERID);
        if (token == null || gp_username == null) return false;
        
        Context context = Context.getContextForUser(gp_username);
        String genomeSpaceEnvironment = GenomeSpaceClientFactory.getGenomeSpaceEnvironment(context);
        
        GenomeSpaceLogin login = GenomeSpaceClientFactory.getGenomeSpaceClient().submitLogin(genomeSpaceEnvironment, token);
        if (login == null) return false;
        
        // Get the correct username because in the CDK as it stands now GsSession.getCachedUsernameForSSO() is sometimes stale
        if (gsUsername != null) {
            login.setUsername(gsUsername);
        }
        
        setSessionAttributes(login, httpSession);
        return true;
    }
    
    public static boolean loginFromDatabase(String gp_username, HttpSession httpSession) throws GenomeSpaceException {
        Context context = Context.getContextForUser(gp_username);
        String genomeSpaceEnvironment = GenomeSpaceClientFactory.getGenomeSpaceEnvironment(context);
        
        if (GenomeSpaceDatabaseManager.isGPAccountAssociated(gp_username)) {
            String token = GenomeSpaceDatabaseManager.getGSToken(gp_username);
            GenomeSpaceLogin login = GenomeSpaceClientFactory.getGenomeSpaceClient().submitLogin(genomeSpaceEnvironment, token);
            if (login == null) return false;
            
            // Get the correct username because in the CDK as it stands now GsSession.getCachedUsernameForSSO() is sometimes stale
            String gsUsername = GenomeSpaceDatabaseManager.getGSUsername(gp_username);
            if (gsUsername != null) {
                login.setUsername(gsUsername);
            }
            
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
    
    public static void setSessionAttributes(GenomeSpaceLogin login, HttpSession httpSession) {
        // Set attributes from login in the GenePattern session
        for(Entry<String,Object> entry : login.getAttributes().entrySet()) {
            httpSession.setAttribute(entry.getKey(), entry.getValue()); 
        }
        httpSession.setAttribute(GS_USER_KEY, login.getUsername());
        httpSession.setAttribute(GS_TOKEN_KEY, login.getAuthenticationToken());
        httpSession.setAttribute(GS_EMAIL_KEY, login.getEmail());
        String gpUsername = (String) httpSession.getAttribute(GPConstants.USERID);
        log.info("Writing to database: " + gpUsername);
        GenomeSpaceDatabaseManager.updateDatabase(gpUsername, login.getAuthenticationToken(), login.getUsername(), login.getEmail());
    }
    
    public static boolean isGSAccountAssociated(String gsAccount) {
        return GenomeSpaceDatabaseManager.isGSAccountAssociated(gsAccount);
    }
    
    /**
     * Determine if the user has gone through GenomeSpace's OpenID authentication and return the GP username if so.
     * Otherwise return null.
     * @param request
     * @param response
     * @return
     */
    public static String authenticate(HttpServletRequest request, HttpServletResponse response) {
        String gsUsername = (String) request.getSession().getAttribute(GenomeSpaceLoginManager.GS_USER_KEY);
        String gsToken = (String) request.getSession().getAttribute(GenomeSpaceLoginManager.GS_TOKEN_KEY);
        String gsEmail = (String) request.getSession().getAttribute(GenomeSpaceLoginManager.GS_EMAIL_KEY);
        String gpUsername = GenomeSpaceDatabaseManager.getGPUsername(gsUsername);
        
        if (gpUsername != null && gsToken != null) {
            GenomeSpaceDatabaseManager.updateDatabase(gpUsername, gsToken, gsUsername, gsEmail);
        }
        else {
            return null;
        }
        
        return gpUsername;
    }
    
    public static String generateUsername(String gsUsername) {
        String suggestedName = gsUsername;
        int count = 1;
        while (UserAccountManager.instance().userExists(suggestedName)) {
            suggestedName = gsUsername + count;
            count++;
        }
        
        return suggestedName;
    }
    
    public static String generatePassword() {
        Random rand = new Random();
        return "GS" + rand.nextInt();
    }
    
    public static void createGenePatternAccount(String username, String password, String email) {
        try {
            UserAccountManager.instance().createUser(username, password, email);
        }
        catch (AuthenticationException e) {
            log.error("Error auto-creating a new GenePattern user: " + username);
        }
    }
}
