/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server;

import java.io.File;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import org.apache.log4j.Logger;
import org.genepattern.server.auth.AuthenticationException;
import org.genepattern.server.auth.DefaultGenePatternAuthentication;
import org.genepattern.server.auth.DefaultGroupMembership;
import org.genepattern.server.auth.GroupMembershipWrapper;
import org.genepattern.server.auth.IAuthenticationPlugin;
import org.genepattern.server.auth.IGroupMembershipPlugin;
import org.genepattern.server.auth.NoAuthentication;
import org.genepattern.server.auth.XmlGroupMembership;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.user.User;
import org.genepattern.server.user.UserDAO;

/**
 * Common interface for managing user accounts and groups, used in the web application and soap server.
 * 
 * @author pcarr
 */
public class UserAccountManager {
    private static Logger log = Logger.getLogger(UserAccountManager.class);
    
    public static final String PROP_AUTHENTICATION_CLASS = "authentication.class";
    public static final String PROP_GROUP_MEMBERSHIP_CLASS = "group.membership.class";

    /**
     * @return the singleton instance of the UserAccountManager.
     */
    public static UserAccountManager instance() {
        return Singleton.userAccountManager;
    }
    
    /**
     * Lazy load the singleton instance of the UserAccountManager class.
     */
    private static class Singleton {
        static UserAccountManager userAccountManager = new UserAccountManager();
    }
    
    private IAuthenticationPlugin authentication = null;
    private IGroupMembershipPlugin groupMembership = null;

    //these properties are used in the DefaultGenePatternAuthentication class, and in the LoginBean
    private boolean createAccountAllowed = true;
    private boolean showRegistrationLink = true;
    
    //this property is optionally (when set) used in the default goup membership class, XmlGroupMembership
    private File userGroups=null;

    /**
     * private constructor requires call to {@link #instance()}.
     */
    private UserAccountManager() {
        p_refreshProperties();
        p_refreshUsersAndGroups();
    }

    /**
     * Flag indicating whether or not users can register new accounts via the web interface.
     * @return
     */
    public boolean isCreateAccountAllowed() {
        return createAccountAllowed;
    }
    
    public boolean isShowRegistrationLink() {
        return showRegistrationLink;
    }

    /**
     * Optionally set a non-default location for the users and groups file.
     * You must call refreshUsersAndGroups before this change takes effect.
     * 
     * Note: this only has effect if you are using the default (XmlGroupMembership) implementation
     * of the IGroupMembershipPlugin interface.
     * 
     * @param userGroups
     */
    public void setUserGroups(final File userGroups) {
        this.userGroups=userGroups;
    }

    /** @deprecated */
    public static void validateNewUsername(final String username) throws AuthenticationException {
        validateNewUsername(HibernateUtil.instance(), username);
    }

    /** @deprecated, should pass in a valid GpConfig and userContext */
    public static void validateNewUsername(final HibernateSessionManager mgr, final String username) throws AuthenticationException {
        final GpConfig gpConfig=ServerConfigurationFactory.instance();
        validateNewUsername(gpConfig, mgr, username);
    }

    /**
     * Validate the username before creating a new account.
     * Prohibit creating new user accounts whose names differ only by case.
     * The username must be a valid filename on the server's file system.
     * 
     * @param username
     * @throws AuthenticationException
     */
    public static void validateNewUsername(final GpConfig gpConfig, final HibernateSessionManager mgr, final String username) throws AuthenticationException {
        //1) is it a valid username
        validateUsername(username);
        //2) is it a unique username
        User user = (new UserDAO(mgr)).findByIdIgnoreCase(username);
        if (user != null) {
            throw new AuthenticationException(AuthenticationException.Type.INVALID_USERNAME,
                    "User already registered: "+user.getUserId());
        }
        //3) can create user dir for user
        try {
            final GpContext userContext = GpContext.getContextForUser(username);
            final File userDir = gpConfig.getUserDir(userContext);
            log.info("creating user dir: "+userDir.getPath());
        }
        catch (Throwable t) {
            throw new AuthenticationException(AuthenticationException.Type.INVALID_USERNAME, 
                    "Error creating 'gp.user.dir' for username="+username+", error: " + t.getLocalizedMessage() );
        }
    }

    /**
     * Is the username valid for a GenePattern account. This does not check for similar names in the database.
     * It just enforces any rules on what constitutes a valid name.
     * <ul>
     * <li>No space characters allowed at the beginning or end of the name.
     * <li>Must map to valid filename on the servers file system. E.g. for unix, no '/' characters allowed.
     * </ul>
     * 
     * @param username
     * @throws AuthenticationException if the username is not valid
     */
    static public void validateUsername(String username) throws AuthenticationException {
        if (username == null) {
            throw new AuthenticationException(AuthenticationException.Type.INVALID_USERNAME,
                    "Username is null");
        }
        if (username.startsWith(" ")) {
            throw new AuthenticationException(AuthenticationException.Type.INVALID_USERNAME, 
                    "Invalid username: '"+username+"': Can't start with a space (' ') character.");
        }
        if (username.endsWith(" ")) {
            throw new AuthenticationException(AuthenticationException.Type.INVALID_USERNAME, 
                    "Invalid username: '"+username+"': Can't end with a space (' ') character.");
        }
        if (username.contains(File.separator)) {
            throw new AuthenticationException(AuthenticationException.Type.INVALID_USERNAME, 
                    "Invalid username: '"+username+"': Can't contain a file separator ('"+File.separator+"') character.");
        }
        if (username.contains("/")) {
            throw new AuthenticationException(AuthenticationException.Type.INVALID_USERNAME,
                    "Invalid username: '"+username+"': Can't contain ('/') character.");
        } 
        if (username.contains("\\")) {
            throw new AuthenticationException(AuthenticationException.Type.INVALID_USERNAME,
                    "Invalid username: '"+username+"': Can't contain ('\\') character.");
        } 
        if (username.startsWith("\"")) {
            throw new AuthenticationException(AuthenticationException.Type.INVALID_USERNAME,
                    "Invalid username: '"+username+"': Can't begin with a quote ('\"') character.");
        }
        if (username.endsWith("\"")) {
            throw new AuthenticationException(AuthenticationException.Type.INVALID_USERNAME,
                    "Invalid username: '"+username+"': Can't end with a quote ('\"') character.");
        }
        if (username.startsWith("\'")) {
            throw new AuthenticationException(AuthenticationException.Type.INVALID_USERNAME,
                    "Invalid username: '"+username+"': Can't begin with a quote (') character.");
        }
        if (username.endsWith("\'")) {
            throw new AuthenticationException(AuthenticationException.Type.INVALID_USERNAME,
                    "Invalid username: '"+username+"': Can't end with a quote (') character.");
        }
        if (username.startsWith("-")) {
            throw new AuthenticationException(AuthenticationException.Type.INVALID_USERNAME, 
                    "Invalid username: '"+username+"': Can't start with the '-' character.");
        }
        if (username.contains("\t")) {
            throw new AuthenticationException(AuthenticationException.Type.INVALID_USERNAME,
                    "Invalid username: '"+username+"': Can't contain a TAB character.");
        }
        if (username.contains(";")) {
            throw new AuthenticationException(AuthenticationException.Type.INVALID_USERNAME,
                    "Invalid username: '"+username+"': Can't contain a semicolon (';') character.");
        }
        if (username.startsWith(".")) {
            throw new AuthenticationException(AuthenticationException.Type.INVALID_USERNAME,
                    "Invalid username: '"+username+"': Can't start with the dot ('.') character.");
        }
    }

    /** @deprecated */
    public static boolean userExists(final String username) {
        return userExists(HibernateUtil.instance(), username);
    }

    /**
     * Is there already a GenePattern user account with this username.
     * 
     * @param username
     * @return
     */
    public static boolean userExists(final HibernateSessionManager mgr, final String username) {
        final User user = (new UserDAO(mgr)).findByIdIgnoreCase(username);
        return user != null;
    }

    /**
     * Create a new GenePattern user account.
     * 
     * @param username
     * @throws AuthenticationException - if the user is already registered.
     * 
     * @deprecated should pass in valid HibernateSessionManager and GpConfig
     */
    public void createUser(String username) throws AuthenticationException {
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
    public void createUser(String username, String password) throws AuthenticationException {
        String email = null;
        createUser(username, password, email);
    }
    
    /** @deprecated */
    public static void createUser(final String username, final String passwordOrNull, final String email) throws AuthenticationException {
        createUser(ServerConfigurationFactory.instance(), HibernateUtil.instance(), username, passwordOrNull, email);
    }
    
    /**
     * Create a new GenePattern user account.
     * 
     * @param username
     * @param password, can be null
     * @param email, can be null
     * @throws AuthenticationException - if the user is already registered.
     */
    public static void createUser(final GpConfig gpConfig, final HibernateSessionManager mgr, final String username, final String passwordOrNull, final String email) throws AuthenticationException {
        validateNewUsername(gpConfig, mgr, username);

        final User newUser = new User();
        newUser.setUserId(username);
        newUser.setRegistrationDate(new Date());
        if (email != null) {
            newUser.setEmail(email);
        }
        try {
            newUser.setPassword(EncryptionUtil.encrypt(passwordOrNull));
        } 
        catch (NoSuchAlgorithmException e) {
            log.error(e);
        }
        (new UserDAO(mgr)).save(newUser);
    }
    
    /**
     * Authenticate using the username:password pair by looking up the credentials in the GenePattern user database.
     * 
     * @param username
     * @param password - the user's unencrypted password
     * @return
     * @throws AuthenticationException
     */
    public boolean authenticateUser(final String username, final byte[] password) throws AuthenticationException {
        if (username == null) {
            throw new AuthenticationException(AuthenticationException.Type.INVALID_USERNAME, "Missing required parmameter: username");
        }

        final GpConfig gpConfig=ServerConfigurationFactory.instance();
        final boolean isPasswordRequired = gpConfig.isPasswordRequired(GpContext.getServerContext());
        if (isPasswordRequired && password == null) {
            throw new AuthenticationException(AuthenticationException.Type.INVALID_CREDENTIALS, "Missing required parmameter: password");
        }
        
        User user = null;
        try {
            user = (new UserDAO()).findById(username);
        }
        catch (Error e) {
            throw new AuthenticationException(AuthenticationException.Type.SERVICE_NOT_AVAILABLE, e.getLocalizedMessage());
        }
        if (user == null) {
            throw new AuthenticationException(AuthenticationException.Type.INVALID_USERNAME, "User '"+username+"' is not registered.");
        }
        if (!isPasswordRequired) {
            return true;
        }
        byte[] encryptedPassword = null;
        try {
            encryptedPassword = EncryptionUtil.encrypt(new String(password));
        }
        catch (NoSuchAlgorithmException e) {
            throw new AuthenticationException(AuthenticationException.Type.SERVICE_NOT_AVAILABLE, e.getLocalizedMessage());
        }
        if (java.util.Arrays.equals(encryptedPassword, user.getPassword())) {
            return true;
        }
        
        throw new AuthenticationException(AuthenticationException.Type.INVALID_CREDENTIALS, "Incorrect password for user '"+username+"'");
    }

    /**
     * Get the IAuthenticationPlugin for this GenePattern Server.
     * @return
     */
    public IAuthenticationPlugin getAuthentication() {
        return authentication;
    }
    
    /**
     * Get the IGroupMembershipPlugin for this GenePattern Server.
     * @return
     */
    public IGroupMembershipPlugin getGroupMembership() {
        return groupMembership;
    }
    
    /**
     * If necessary reload user and groups information by reloading the IAuthenticationPlugin and IGroupMembershipPlugins.
     * This supports one specific use-case: when GP default group membership is used, and an admin edits the configuration file,
     * it cause the GP server to reload group membership information from the config file.
     */
    public synchronized void refreshUsersAndGroups() {
        p_refreshProperties();
        p_refreshUsersAndGroups();
    }

    private void p_refreshProperties() {
        String prop = System.getProperty("create.account.allowed", "true").toLowerCase();
        this.createAccountAllowed = 
            prop.equals("true") ||  prop.equals("y") || prop.equals("yes");
        
        prop = System.getProperty("show.registration.link", "true").toLowerCase();
        this.showRegistrationLink = 
            prop.equals("true") ||  prop.equals("y") || prop.equals("yes");
    }

    //don't know if this is necessary, but it is here because it is called from the constructor
    //    and from refreshUsersAndGroups (which is synchronized).
    private void p_refreshUsersAndGroups() {
        this.authentication = null;
        this.groupMembership = null;
        String customAuthenticationClass = System.getProperty(PROP_AUTHENTICATION_CLASS);
        String customGroupMembershipClass = System.getProperty(PROP_GROUP_MEMBERSHIP_CLASS);
        loadAuthentication(customAuthenticationClass);

        //check for special case: 
        //    use the same instance for both Authentication and GroupMembership 
        //    if and only if both are set to the same class
        if (this.authentication instanceof IGroupMembershipPlugin &&
            customAuthenticationClass != null && 
            !"".equals(customAuthenticationClass) && 
            customAuthenticationClass.equals(customGroupMembershipClass))
        {
            this.groupMembership = (IGroupMembershipPlugin) this.authentication;
        }
        else {
            loadGroupMembership(customGroupMembershipClass);            
        }
        this.groupMembership = new GroupMembershipWrapper(this.groupMembership);
    }
    
    private void loadAuthentication(String customAuthenticationClass) {
        log.debug("loading IAuthenticationPlugin, customAuthenticationClass="+customAuthenticationClass);
        if (customAuthenticationClass == null) {
            log.debug("initializing default ...");
            this.authentication = new DefaultGenePatternAuthentication();
        }
        else {
            log.debug("initializing custom ...");
            try {
                this.authentication = (IAuthenticationPlugin) Class.forName(customAuthenticationClass).newInstance();
            } 
            catch (final Exception e) {
                log.error("Failed to load custom authentication class: "+customAuthenticationClass, e);
                this.authentication = new NoAuthentication(e);
            } 
        }
        if (this.authentication == null) {
            log.error("this.authentication==null");
        }
        else {
            log.debug("authentication.class="+this.authentication.getClass().getCanonicalName());
        }
    }
    
    private void loadGroupMembership(String customGroupMembershipClass) {
        if (customGroupMembershipClass == null) {
            if (userGroups != null) {
                this.groupMembership = new XmlGroupMembership(userGroups); 
            }
            else {
                this.groupMembership = new XmlGroupMembership(); 
            }
        }
        else {
            try {
                this.groupMembership = (IGroupMembershipPlugin) Class.forName(customGroupMembershipClass).newInstance();
            }
            catch (Exception e) {
                log.error("Failed to load custom group membership class: "+customGroupMembershipClass, e);
                this.groupMembership = new DefaultGroupMembership();
            }
        }
    }
    
}
