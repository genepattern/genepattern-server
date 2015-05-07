/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.genomespace;

import java.util.Date;
import java.util.Map.Entry;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.genepattern.server.UserAccountManager;
import org.genepattern.server.auth.AuthenticationException;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.webapp.jsf.UIBeanHelper;
import org.genepattern.util.GPConstants;

/**
 * Manager for handing logging into and registering GenomeSpace accounts
 * @author tabor
 */
public class GenomeSpaceLoginManager {
    private static Logger log = Logger.getLogger(GenomeSpaceLoginManager.class);
    
    // Keys used to attach or retrieve GenomeSpace information from the current GenePattern session
    public static String GS_SESSION_KEY = "GS_SESSION";
    public static String GS_USER_KEY = "GS_USER";
    public static String GS_TOKEN_KEY = "GS_TOKEN";
    public static String GS_EMAIL_KEY = "GS_EMAIL";
    public static String GS_OPENID_KEY = "GS_OPENID";
    public static String GS_DIRECTORIES_KEY = "GS_DIRECTORIES";
    public static String GS_FILE_METADATAS = "GS_FILE_METADATAS";
    
    // Constants used to redirect the user to a GenomeSpace login prompt if their session is expiring
    public static String REDIRECT_KEY = "origin";
    public static String GS_LOGIN_PAGE = "/gp/pages/genomespace/signon.jsf";
    
    /**
     * Logs a user into GenomeSpace using the GenomeSpace information attached to the current GenePattern session
     * @param httpSession
     * @return
     * @throws GenomeSpaceException
     */
    public static boolean loginFromSession(HttpSession httpSession) throws GenomeSpaceException {
        String token = (String) httpSession.getAttribute(GS_TOKEN_KEY);
        String gsUsername = (String) httpSession.getAttribute(GS_USER_KEY);
        String gp_username = (String) httpSession.getAttribute(GPConstants.USERID);
        if (token == null || gp_username == null) return false;
        
        GpContext context = GpContext.getContextForUser(gp_username);
        String genomeSpaceEnvironment = GenomeSpaceClientFactory.getGenomeSpaceEnvironment(context);

        GenomeSpaceLogin login = GenomeSpaceClientFactory.instance().submitLogin(genomeSpaceEnvironment, token);
        if (login == null) return false;
        
        // Get the correct username because in the CDK as it stands now GsSession.getCachedUsernameForSSO() is sometimes stale
        if (gsUsername != null) {
            login.setUsername(gsUsername);
        }
        
        setSessionAttributes(login, httpSession);
        return true;
    }
    
    /**
     * Logs a user into GenomeSpace using the information in the database associated with the given GenePattern user
     * @param gp_username
     * @param httpSession
     * @return
     * @throws GenomeSpaceException
     */
    public static boolean loginFromDatabase(String gp_username, HttpSession httpSession) throws GenomeSpaceException {
        GpContext context = GpContext.getContextForUser(gp_username);
        String genomeSpaceEnvironment = GenomeSpaceClientFactory.getGenomeSpaceEnvironment(context);
        
        if (GenomeSpaceDatabaseManager.isGPAccountAssociated(gp_username)) {
            // Check for GS token expiration and redirect to GenomeSpace login if expired or about to expire
            if (tokenExpiring(gp_username)) {
                GenomeSpaceBean genomeSpaceBean = (GenomeSpaceBean) UIBeanHelper.getManagedBean("#{genomeSpaceBean}");
                GenomeSpaceManager.setTokenExpired(httpSession, true);
                httpSession.setAttribute(REDIRECT_KEY, GS_LOGIN_PAGE);
                return false;
            }

            String token = GenomeSpaceDatabaseManager.getGSToken(gp_username);
            try {
                GenomeSpaceLogin login = GenomeSpaceClientFactory.instance().submitLogin(genomeSpaceEnvironment, token);
                if (login == null) return false;
                
                // Get the correct username because in the CDK as it stands now GsSession.getCachedUsernameForSSO() is sometimes stale
                String gsUsername = GenomeSpaceDatabaseManager.getGSUsername(gp_username);
                if (gsUsername != null) {
                    login.setUsername(gsUsername);
                }
                
                setSessionAttributes(login, httpSession);
                return true;
            }
            catch (Throwable t) {
                log.info("Issue with logging into GenomeSpace");
                return false;
            }
        }
        return false;
    }
    
    /**
     * Logs a user into GenomeSpace using a provided username and password
     * @param env
     * @param genomeSpaceUsername
     * @param genomeSpacePassword
     * @param httpSession
     * @return
     * @throws GenomeSpaceException
     */
    public static boolean loginFromUsername(String env, String genomeSpaceUsername, String genomeSpacePassword, HttpSession httpSession) throws GenomeSpaceException {
        GenomeSpaceLogin login = GenomeSpaceClientFactory.instance().submitLogin(env, genomeSpaceUsername, genomeSpacePassword);
        if (login == null) return false;
        setSessionAttributes(login, httpSession);
        return true;
    }
    
    /**
     * Attaches the provided GenomeSpace information to the current GenePattern session
     * @param login
     * @param httpSession
     */
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
    
    /**
     * Determines whether the provided GenomeSpace user has an associated GenePattern account
     * @param gsAccount
     * @return
     */
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
    
    /**
     * Generates an available GenePattern username based on the given GenomeSpace username
     * @param gsUsername
     * @return
     */
    public static String generateUsername(String gsUsername) {
        String suggestedName = gsUsername;
        int count = 1;
        while (UserAccountManager.instance().userExists(suggestedName)) {
            suggestedName = gsUsername + count;
            count++;
        }
        
        return suggestedName;
    }
    
    /**
     * Generates a random password for auto-creating a GenePattern account
     * @return
     */
    public static String generatePassword() {
        Random rand = new Random();
        return "GS" + rand.nextInt();
    }
    
    /**
     * Creates a GenePattern account with the given username, password and email
     * @param username
     * @param password
     * @param email
     */
    public static void createGenePatternAccount(String username, String password, String email) {
        try {
            UserAccountManager.instance().createUser(username, password, email);
        }
        catch (AuthenticationException e) {
            log.error("Error auto-creating a new GenePattern user: " + username);
        }
    }
    
    /**
     * Reads the token timestamp from the database for the given GenePattern user and estimates 
     * when the timestamp will expire based on an assumed expiration of one week, with one day's
     * wiggle room before expiration
     * @param gpUsername
     * @return
     */
    public static Date estimateTokenExpiration(String gpUsername) {
        Date timestamp = GenomeSpaceDatabaseManager.getTokenTimestamp(gpUsername);
        // Assumed to expire in a week, but here to estimate 6 days to give it some wiggle room
        long expiration = timestamp.getTime() + 518400000;
        return new Date(expiration);
    }
    
    /**
     * Returns whether the GenomeSpace token for the given GenePattern user has expired or is about to expire
     * @param gpUsername
     * @return
     */
    public static boolean tokenExpiring(String gpUsername) {
        Date expiration = estimateTokenExpiration(gpUsername);
        Date currentTime = new Date();
        return currentTime.after(expiration);
    }
}
