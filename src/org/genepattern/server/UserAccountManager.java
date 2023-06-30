/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server;

import java.io.File;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.log4j.Logger;
import org.genepattern.server.auth.AuthenticationException;
import org.genepattern.server.auth.DefaultGenePatternAuthentication;
import org.genepattern.server.auth.GroupMembershipWrapper;
import org.genepattern.server.auth.IAuthenticationPlugin;
import org.genepattern.server.auth.IGroupMembershipPlugin;
import org.genepattern.server.auth.NoAuthentication;
import org.genepattern.server.auth.UserGroups;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.user.User;
import org.genepattern.server.user.UserDAO;
import org.genepattern.server.webservice.server.dao.UsageStatsDAO;

import com.google.common.base.Strings;

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
     * The 'username.regex' is a regular expression which matches all valid usernames 
     * and which must not match any invalid usernames. This pattern is used to validate
     * prospective usernames via the java.util.regex.Pattern class.
     * 
     * @see "https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html" 
     * 
     * Java code:
     * <pre>
     * Pattern pattern = Pattern.compile(usernameRegex);
       return pattern.matcher(username).matches();
     * </pre>
     * 
     * <p>
     * Example usage:
     * <pre>
     *     # match all alphanumeric characters and the '@' symbol
     *     username.regex: "[A-Za-z0-9_@.\\- ]+"
     *     
     * update 11/30/2021  remove the space character    
     * </pre>
     */
    public static final String PROP_USERNAME_REGEX="username.regex";
    public static final String DEFAULT_USERNAME_REGEX="[A-Za-z0-9_@.\\-]+";

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
    private UserGroups userGroups = null;
    
    //this property is optionally (when set) used in the default group membership class, UserGroups
    private File userGroupsXml=null;

    /**
     * private constructor requires call to {@link #instance()}.
     */
    private UserAccountManager() {
        p_refreshUsersAndGroups();
    }

    /**
     * Optionally set a non-default location for the users and groups file.
     * You must call refreshUsersAndGroups before this change takes effect.
     * 
     * Note: this only has effect if you are using the default (UserGroups) implementation
     * of the IGroupMembershipPlugin interface.
     * 
     * @param userGroups
     */
    public void setUserGroupsXml(final File userGroupsXml) {
        this.userGroupsXml=userGroupsXml;
    }

    public static long getTotalUserCount(){
        
        final HibernateSessionManager mgr = org.genepattern.server.database.HibernateUtil.instance();
        long count = (new UserDAO(HibernateUtil.instance())).getAllUsersCount();
       
        return count;
       
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
        validateUsername(gpConfig, username);
        //2) is it a unique username
        User user = (new UserDAO(mgr)).findByIdIgnoreCase(username);
        if (user != null) {
            throw new AuthenticationException(AuthenticationException.Type.INVALID_USERNAME,
                    "User already registered: "+user.getUserId());
        }
        //3) can create user dir for user
        try {
            final boolean initIsAdmin=false;
            final GpContext userContext = GpContext.getContextForUser(username, initIsAdmin);
            final File userDir = gpConfig.getUserDir(userContext);
            log.info("creating user dir: "+userDir.getPath());
        }
        catch (Throwable t) {
            throw new AuthenticationException(AuthenticationException.Type.INVALID_USERNAME, 
                    "Error creating 'gp.user.dir' for username="+username+", error: " + t.getLocalizedMessage() );
        }
    }

    /**
     * Validate the username with the {@code username.regex}
     * Throw an exception if the username  does not match the pattern.
     * {@link #PROP_USERNAME_REGEX}
     * 
     * 
     * @throws AuthenticationException if the username is not valid
     * 
     * @see "https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html"
     */
    public static boolean validateUsernameFromRegex(final GpConfig gpConfig, final GpContext serverContext, final String username) 
    throws AuthenticationException
    {
        final String regex;
        if (gpConfig==null) {
            regex=DEFAULT_USERNAME_REGEX;
        }
        else {
            final String prop=gpConfig.getGPProperty(serverContext, PROP_USERNAME_REGEX, DEFAULT_USERNAME_REGEX);
            if (Strings.nullToEmpty(prop).trim().isEmpty()) {
                regex=DEFAULT_USERNAME_REGEX;
                log.warn("ignoring empty property: "+PROP_USERNAME_REGEX+"='"+regex+"'");
            }
            else {
                regex=prop;
            }
        }
        try {
            final Pattern pattern = Pattern.compile(regex);
            //return pattern.matcher(username).matches();
            final Matcher m = pattern.matcher(username);
            final boolean valid=m.matches();
            if (!valid) {
                // remove matching characters
                final String invalidChars=m.replaceAll("");
                if (!Strings.isNullOrEmpty(invalidChars)) {
                    throw new AuthenticationException(AuthenticationException.Type.INVALID_USERNAME, 
                        "Invalid username: '"+username+"': characters not allowed: '"+invalidChars+"'");
                }
                else {
                    throw new AuthenticationException(AuthenticationException.Type.INVALID_USERNAME, 
                        "Invalid username: '"+username+"': does not match 'username.regex'");
                }
            }
            return valid;
        }
        catch (PatternSyntaxException e) {
            log.error("Invalid regex pattern, "+PROP_USERNAME_REGEX+"='"+regex+"'", e);
            throw new AuthenticationException(AuthenticationException.Type.SERVICE_NOT_AVAILABLE,
                "Server configuration error, invalid regex pattern");
        }
        catch (Throwable t) {
            throw new AuthenticationException(AuthenticationException.Type.SERVICE_NOT_AVAILABLE,
                "Unexpected server error: "+t.getLocalizedMessage());
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
    static public void validateUsername(final GpConfig gpConfig, final String username) throws AuthenticationException {
        if (username == null) {
            throw new AuthenticationException(AuthenticationException.Type.INVALID_USERNAME,
                    "Username is null");
        }
        if (username.trim().length()==0) {
            throw new AuthenticationException(AuthenticationException.Type.INVALID_USERNAME,
                    "Username not set (empty string)");
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
        if (username.contains("\n")) {
            throw new AuthenticationException(AuthenticationException.Type.INVALID_USERNAME,
                    "Invalid username: '"+username+"': Can't contain a NEWLINE character.");
        }
        if (username.contains(";")) {
            throw new AuthenticationException(AuthenticationException.Type.INVALID_USERNAME,
                    "Invalid username: '"+username+"': Can't contain a semicolon (';') character.");
        }
        if (username.startsWith(".")) {
            throw new AuthenticationException(AuthenticationException.Type.INVALID_USERNAME,
                    "Invalid username: '"+username+"': Can't start with the dot ('.') character.");
        }
        if (username.startsWith("@")) {
            throw new AuthenticationException(AuthenticationException.Type.INVALID_USERNAME,
                    "Invalid username: '"+username+"': Can't start with the at ('@') character.");
        }
        // match the allowed username regex
        validateUsernameFromRegex(gpConfig, GpContext.getServerContext(), username);

        if (contains2(username, '@')) {
            throw new AuthenticationException(AuthenticationException.Type.INVALID_USERNAME,
                    "Invalid username: '"+username+"': Can't contain more than one '@' character");            
        }
    }
    
    /**
     * Helper method, return true if the given string contains two or more of the given characters.
     */
    protected static boolean contains2(final String str, final char c) {
        if (str==null || str.length()<2) {
            return false;
        }
        int i=str.indexOf(c);
        if (i>=0) {
            i=str.indexOf(c,i+1);
            if (i>=0) {
                return true;
            }
        }
        return false;
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

    /** @deprecated pass in a valid GpConfig and Hibernate session */
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
            user = (new UserDAO(HibernateUtil.instance())).findById(username);
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
    
    public UserGroups getUserGroups() {
        return userGroups;
    }
    
    /**
     * If necessary reload user and groups information by reloading the IAuthenticationPlugin and IGroupMembershipPlugins.
     * This supports one specific use-case: when GP default group membership is used, and an admin edits the configuration file,
     * it cause the GP server to reload group membership information from the config file.
     */
    public synchronized void refreshUsersAndGroups() {
        p_refreshUsersAndGroups();
    }

    //don't know if this is necessary, but it is here because it is called from the constructor
    //    and from refreshUsersAndGroups (which is synchronized).
    private void p_refreshUsersAndGroups() {
        p_refreshUsersAndGroups(ServerConfigurationFactory.instance(), GpContext.getServerContext());
    }
    
    private void p_refreshUsersAndGroups(final GpConfig gpConfig, final GpContext serverContext) {
        this.authentication = null;
        this.groupMembership = null;
        final String customAuthenticationClass = gpConfig.getGPProperty(serverContext, PROP_AUTHENTICATION_CLASS, null);
        final String customGroupMembershipClass = gpConfig.getGPProperty(serverContext, PROP_GROUP_MEMBERSHIP_CLASS, null);
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
            loadGroupMembership(gpConfig, customGroupMembershipClass);            
        }
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
            log.debug(PROP_AUTHENTICATION_CLASS+"="+this.authentication.getClass().getCanonicalName());
        }
    }
    
    private void loadGroupMembership(final GpConfig gpConfig, String customGroupMembershipClass) {
        if (customGroupMembershipClass == null) { 
            if (userGroupsXml != null) {
                this.userGroups = UserGroups.initFromXml(userGroupsXml);
            }
            else {
                this.userGroups = UserGroups.initFromConfig(gpConfig);
            }
            this.groupMembership = userGroups;
        }
        else {
            try {
                final IGroupMembershipPlugin customGroupMembership = 
                        (IGroupMembershipPlugin) Class.forName(customGroupMembershipClass).newInstance();
                this.groupMembership = new GroupMembershipWrapper(customGroupMembership);
            }
            catch (Exception e) {
                log.error("Failed to load custom group membership class: "+customGroupMembershipClass, e);
                this.groupMembership = UserGroups.initDefault();
            }
        }
    }
    
}
