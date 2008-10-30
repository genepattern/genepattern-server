package org.genepattern.server;

import java.security.NoSuchAlgorithmException;
import java.util.Date;

import org.apache.log4j.Logger;
import org.genepattern.server.auth.AuthenticationException;
import org.genepattern.server.auth.DefaultGenePatternAuthentication;
import org.genepattern.server.auth.IAuthenticationPlugin;
import org.genepattern.server.auth.WorldAccessAuthentication;
import org.genepattern.server.user.User;
import org.genepattern.server.user.UserDAO;

/**
 * Common interface for managing user accounts and groups, used in the web application and soap server.
 * 
 * @author pcarr
 */
public class UserAccountManager {
    private static Logger log = Logger.getLogger(UserAccountManager.class);

    //force use of factory methods
    private UserAccountManager() {
    }
    
    private static UserAccountManager userAccountManager = null;
    public static UserAccountManager instance() {
        if (userAccountManager == null) {
            userAccountManager = new UserAccountManager();
            String prop = System.getProperty("require.password", "false").toLowerCase();
            userAccountManager.passwordRequired = (prop.equals("true") || prop.equals("y") || prop.equals("yes"));

            String createAccountAllowedProp = System.getProperty("create.account.allowed", "true").toLowerCase();
            userAccountManager.createAccountAllowed = 
                createAccountAllowedProp.equals("true") || 
                createAccountAllowedProp.equals("y") || 
                createAccountAllowedProp.equals("yes");
            
            userAccountManager.authentication = new DefaultGenePatternAuthentication();
            //userAccountManager.authentication = new WorldAccessAuthentication();
        }
        return userAccountManager;
    }
    
    private boolean passwordRequired = true;
    private boolean createAccountAllowed = true;
    private IAuthenticationPlugin authentication = null;

    /**
     * Flag indicating whether or not users can register new accounts via the web interface.
     * @return
     */
    public boolean isCreateAccountAllowed() {
        return createAccountAllowed;
    }

    /**
     * Flag indicating whether or not passwords are required for the default genepattern authentication scheme.
     * @return
     */
    public boolean isPasswordRequired() {
        return passwordRequired;
    }

    /**
     * Is there already a GenePattern user account with this username.
     * 
     * @param username
     * @return
     */
    public boolean userExists(String username) {
        User user = (new UserDAO()).findById(username);
        return user != null;
    }

    /**
     * Create a new GenePattern user account.
     * 
     * @param username
     * @throws AuthenticationException - if the user is already registered.
     */
    public void createUser(String username) 
    throws AuthenticationException
    {
        String password = "";
        createUser(username, password);
    }

    /**
     * Create a new GenePattern user account.
     * 
     * @param username
     * @param password
     * @throws AuthenticationException - if the user is already registered.
     */
    public void createUser(String username, String password) 
    throws AuthenticationException
    {
        String email = null;
        createUser(username, password, email);
    }
    
    /**
     * Create a new GenePattern user account.
     * 
     * @param username
     * @param password
     * @param email
     * @throws AuthenticationException - if the user is already registered.
     */
    public void createUser(String username, String password, String email) 
    throws AuthenticationException {
        if (userExists(username)) {
            throw new AuthenticationException(AuthenticationException.Type.INVALID_USERNAME, "User already registered: "+username);
        }

        //TODO: apply rule: validate username
        //TODO: apply rule: validate password
        //TODO: apply rule: validate email
        if (password == null) {
            password = "";
        }

        User newUser = new User();
        newUser.setUserId(username);
        newUser.setRegistrationDate(new Date());
        if (email != null) {
            newUser.setEmail(email);
        }
        try {
            newUser.setPassword(EncryptionUtil.encrypt(password));
        } 
        catch (NoSuchAlgorithmException e) {
            log.error(e);
        }
        (new UserDAO()).save(newUser);
    }
    
    /**
     * Get the IAuthenticationPlugin for this GenePattern Server.
     * @return
     */
    public IAuthenticationPlugin getAuthentication() {
        return authentication;
    }
    
}
