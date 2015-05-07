/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.genomespace;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.genepattern.server.UserAccountManager;
import org.genepattern.server.auth.AuthenticationException;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.webapp.LoginManager;
import org.genepattern.server.webapp.ParameterInfoWrapper;
import org.genepattern.server.webapp.jsf.UIBeanHelper;
import org.genepattern.server.webservice.server.dao.AdminDAO;
import org.genepattern.util.SemanticUtil;
import org.genepattern.webservice.TaskInfo;

/**
 * This bean handles the information necessary for logging into GenomeSpace
 * from GenePattern and managing the GenomeSpace session.  It is session-scoped.
 * @author tabor
 *
 */
public class GenomeSpaceBean {
    private static Logger log = Logger.getLogger(GenomeSpaceBean.class);
    public static String HOME_NAVIGATION_KEY = "home";
    public static String LOGIN_FAIL_NAVIGATION_KEY = "genomeSpaceLoginFailed";
    public static String REG_FAIL_NAVIGATION_KEY = "genomeSpaceRegFailed";
    
    public static final String GENOMESPACE_REQUEST = "/gp/pages/genomespace/receiveFile.jsf";
    public static final String GENOMESPACE_OPENID = "/gp/GenomeSpaceOpenID";

    /**
     * Determine whether GenomeSpace is enabled when the bean is created.  This makes sure that GenomeSpace
     * classes aren't loaded unless it is enabled.  It also means that if GenomeSpace is enabled in the 
     * config.yaml file then the user will need to log out and log back in before the GenomeSpace options are visible.
     */
    public GenomeSpaceBean() {
        HttpSession session = UIBeanHelper.getSession();
        boolean enabled = GenomeSpaceClientFactory.isGenomeSpaceEnabled(UIBeanHelper.getUserContext());
        GenomeSpaceManager.setGenomeSpaceEnabled(session, enabled);
    }

    /**
     * Clears all errors after a page has been displayed that will show the errors.
     * Returns a String so that call can be embeded in a JSF page.
     * @return
     */
    public String getClearErrors() {
        HttpSession session = UIBeanHelper.getSession();
        GenomeSpaceManager.clearSessionParameters(session);
        return "";
    }

    public String getReferAndForward() throws IOException {
        HttpServletRequest request = UIBeanHelper.getRequest();
        HttpServletResponse response = UIBeanHelper.getResponse();

        String referrer = LoginManager.getReferrer(request);
        if (referrer != null) {
            // LoginManager#getReferrer removes the 'origin' attribute from the session
            // calling that method from this bean should not do that, so put it back
            request.getSession().setAttribute("origin", referrer);
        }

        if (referrer != null && referrer.contains(GENOMESPACE_REQUEST)){
            response.sendRedirect(GENOMESPACE_OPENID);
        }

        return "OK";
    }
    
    /**
     * Returns whether the GenomeSpace token has expired
     * @return
     */
    public boolean getTokenExpired() {
        HttpSession session = UIBeanHelper.getSession();
        return GenomeSpaceManager.isTokenExpired(session);
    }

    /**
     * Returns the set of child files for the given directory URL 
     * (passed into the request as the "directory" parameter)
     * @return Child files of the requested directory
     */
    public Set<GenomeSpaceFile> getFilesRequested() {HttpSession session = UIBeanHelper.getSession();

        String dirUrl = UIBeanHelper.getRequest().getParameter("directory");
        GenomeSpaceFile directory = null;
        if (dirUrl != null) {
            directory = GenomeSpaceManager.getDirectory(session, dirUrl);
        }
        else {
            directory = GenomeSpaceManager.getFileTreeLazy(session).get(0);
        }
        
        return directory.getChildFiles();
    }
    
    /**
     * Handle form submission for the GenomeSpace login page
     * @return The JSF page navigation rule to go to next
     */
    public String submitLogin() {
        HttpSession session = UIBeanHelper.getSession();
        boolean genomeSpaceEnabled = GenomeSpaceManager.isGenomeSpaceEnabled(session);
        String genomeSpaceUsername = GenomeSpaceManager.getGenomeSpaceUsername(session);

        if (!genomeSpaceEnabled) {
            log.error("GenomeSpace is not enabled at GenomeSpace login");
            return HOME_NAVIGATION_KEY;
        }
        
        if (UIBeanHelper.getUserId() == null) {
            return HOME_NAVIGATION_KEY;
        } 
        
        genomeSpaceUsername = UIBeanHelper.getRequest().getParameter("username");
        String genomeSpacePassword = UIBeanHelper.getRequest().getParameter("password");
        String env = ServerConfigurationFactory.instance().getGPProperty(UIBeanHelper.getUserContext(), "genomeSpaceEnvironment", "prod");
        if (env == null || genomeSpaceUsername == null || genomeSpacePassword == null) {
            log.error("Error getting login criteria for GenomeSpace. Username: " + genomeSpaceUsername + " Password: " + genomeSpacePassword + " Environment: " + env);
            this.setMessageToUser("Error logging into GenomeSpace");
            GenomeSpaceManager.setLoggedIn(session, true);
            return LOGIN_FAIL_NAVIGATION_KEY;
        }
        
        try {
            boolean loggedIn = GenomeSpaceLoginManager.loginFromUsername(env, genomeSpaceUsername, genomeSpacePassword, UIBeanHelper.getSession());
            GenomeSpaceManager.setLoggedIn(session, loggedIn);
            
            if (loggedIn) {
                this.setMessageToUser("Signed in to GenomeSpace as " + genomeSpaceUsername);
                GenomeSpaceManager.setLoginFailed(session, false);
                GenomeSpaceManager.setTokenExpired(session, false);
                return HOME_NAVIGATION_KEY;
            }
            else {
                log.error("GenomeSpaceLogin was null loging into GenomeSpace");
                GenomeSpaceManager.setLoginFailed(session, true);
                this.setMessageToUser("Error logging into GenomeSpace");
                return LOGIN_FAIL_NAVIGATION_KEY;
            }
        } 
        catch (Throwable e) {
            GenomeSpaceManager.setLoginFailed(session, true);
            this.setMessageToUser(e.getMessage());
            return LOGIN_FAIL_NAVIGATION_KEY;
        }
    }

    public boolean isAutoCreateEnabled() {
        return ServerConfigurationFactory.instance().getGPBooleanProperty(UIBeanHelper.getUserContext(), "genomeSpaceAutoCreate", true);
    }
    
    /**
     * Determine whether the user is logged into GenomeSpace
     * @return
     */
    public boolean isLoggedIn() {
        HttpSession session = UIBeanHelper.getSession();
        boolean genomeSpaceEnabled = GenomeSpaceManager.isGenomeSpaceEnabled(session);
        Boolean loggedIn = GenomeSpaceManager.getLoggedIn(session);

        if (!genomeSpaceEnabled) {
            return false;
        }
        
        if (loggedIn != null && loggedIn) {
            return true;
        }
        else {
            HttpSession httpSession = UIBeanHelper.getSession();
            Object gsSessionObj = httpSession.getAttribute(GenomeSpaceLoginManager.GS_SESSION_KEY);
            
            if (gsSessionObj == null) {
                return false;
            }
            else {
                loggedIn = GenomeSpaceClientFactory.instance().isLoggedIn(gsSessionObj);
                GenomeSpaceManager.setLoggedIn(session, loggedIn);
            }
        }

        return loggedIn;
    }
    
    /**
     * Handle submission of the GenomeSpace logout form.
     * Clear the GenomeSpace session variables.
     * @return
     */
    public String submitLogout() {
        HttpSession session = UIBeanHelper.getSession();
        boolean genomeSpaceEnabled = GenomeSpaceManager.isGenomeSpaceEnabled(session);

        if (!genomeSpaceEnabled) {
            log.error("GenomeSpace is not enabled");
            return HOME_NAVIGATION_KEY;
        }
        
        HttpSession httpSession = UIBeanHelper.getSession();
        Object gsSession = httpSession.getAttribute(GenomeSpaceLoginManager.GS_SESSION_KEY);
        GenomeSpaceClientFactory.instance().logout(gsSession);
        httpSession.setAttribute(GenomeSpaceLoginManager.GS_USER_KEY, null);
        httpSession.setAttribute(GenomeSpaceLoginManager.GS_SESSION_KEY, null);
        GenomeSpaceManager.clearSessionParameters(httpSession);
        GenomeSpaceManager.setLoggedIn(session, null);
        setMessageToUser("Logged out of GenomeSpace.");
       
        return HOME_NAVIGATION_KEY;
    }
    
    /**
     * Handle submission of the GenomeSpace registration form.
     * Then log the user into their new GenomeSpace account
     * @deprecated - We no longer directly handle GenomeSpace registration
     * @return
     */
    @Deprecated
    public String submitRegistration() {
        HttpSession session = UIBeanHelper.getSession();
        boolean genomeSpaceEnabled = GenomeSpaceManager.isGenomeSpaceEnabled(session);

        if (!genomeSpaceEnabled) {
            UIBeanHelper.setErrorMessage("GenomeSpace is not enabled");
            log.error("GenomeSpace is not enabled");
            return REG_FAIL_NAVIGATION_KEY;
        }
        
        String genomeSpaceUsername = UIBeanHelper.getRequest().getParameter("username");
        GenomeSpaceManager.setGenomeSpaceUsername(session, genomeSpaceUsername);

        String genomeSpacePassword = UIBeanHelper.getRequest().getParameter("password");
        String regPassword = UIBeanHelper.getRequest().getParameter("regPassword");
        String regEmail = UIBeanHelper.getRequest().getParameter("email");
        String env = ServerConfigurationFactory.instance().getGPProperty(UIBeanHelper.getUserContext(), "genomeSpaceEnvironment", "prod");
        if (env == null || genomeSpaceUsername == null || genomeSpacePassword == null || regPassword == null || regEmail == null) {
            log.error("Field null when trying to register for GenomeSpace " + genomeSpaceUsername + " " + genomeSpacePassword + 
                    " " + regPassword + " " + regEmail + " " + env);
            this.setMessageToUser("Error Registering With GenomeSpace");
            GenomeSpaceManager.setLoginFailed(session, true);
            return REG_FAIL_NAVIGATION_KEY;
        }
        
        if (genomeSpaceUsername == null) {
            this.setMessageToUser("GenomeSpace username is blank");
            GenomeSpaceManager.setLoginFailed(session, true);
            return REG_FAIL_NAVIGATION_KEY;
        }
        if (! regPassword.equals(genomeSpacePassword)) {
            UIBeanHelper.setInfoMessage("GenomeSpace password does not match");
            GenomeSpaceManager.setLoginFailed(session, true);
            return REG_FAIL_NAVIGATION_KEY;
        }
    
        try {
            GenomeSpaceClientFactory.instance().registerUser(env, genomeSpaceUsername, genomeSpacePassword, regEmail);
            GenomeSpaceManager.setLoginFailed(session, false);
            submitLogin();
        }
        catch (GenomeSpaceException e) {
            log.error(e);
            setMessageToUser(e.getLocalizedMessage());
            GenomeSpaceManager.setLoginFailed(session, true);
            return REG_FAIL_NAVIGATION_KEY;
        }
      
        return HOME_NAVIGATION_KEY;
    }

    /**
     * Send an info message to the user when is displayed when the next page loads.
     * @param messageToUser
     */
    public void setMessageToUser(String messageToUser) {
        UIBeanHelper.setInfoMessage(messageToUser);
    }
    
    /**
     * Returns a map of file kinds to a set of modules that accept files of the kind in question.
     * This set is iterated over in the JSF for different files.
     * @return
     */
    public Map<String, List<TaskInfo>> getKindToModules() {
        HttpSession session = UIBeanHelper.getSession();
        Map<String, List<TaskInfo>> kindToModules = GenomeSpaceManager.getKindToModules(session);

        if (kindToModules == null) {
            // Attain a copy of the kindToModules map
            TaskInfo[] moduleArray = new AdminDAO().getLatestTasks(UIBeanHelper.getUserId());
            List<TaskInfo> allModules = Arrays.asList(moduleArray);
            Map<String, Set<TaskInfo>> baseMap = SemanticUtil.getKindToModulesMap(allModules);
            GenomeSpaceManager.setKindToModules(session, kindToModules);
            
            for (Map.Entry<String, Set<TaskInfo>> i : baseMap.entrySet()) {
                List<TaskInfo> list = new ArrayList<TaskInfo>();
                list.addAll(i.getValue());
                Collections.sort(list, new Comparator<TaskInfo>() {
                    public int compare(TaskInfo a, TaskInfo b) {
                        return String.CASE_INSENSITIVE_ORDER.compare(a.getName(), b.getName());
                    }
                });
                kindToModules.put(i.getKey(), list);
            }
        }
        
        return kindToModules;
    }
    
    /**
     * Returns whether GenomeSpace is enabled or not
     * @return
     */
    public boolean isGenomeSpaceEnabled() {
        HttpSession session = UIBeanHelper.getSession();
        return GenomeSpaceManager.isGenomeSpaceEnabled(session);
    }
    
    /**
     * Returns  current GenomeSpace username
     * @return
     */
    public String getUsername() {
        HttpSession session = UIBeanHelper.getSession();
        String genomeSpaceUsername = GenomeSpaceManager.getGenomeSpaceUsername(session);

        if (genomeSpaceUsername == null) {
            // Lazily initialize
            genomeSpaceUsername = (String) UIBeanHelper.getSession().getAttribute(GenomeSpaceLoginManager.GS_USER_KEY);
            GenomeSpaceManager.setGenomeSpaceUsername(session, genomeSpaceUsername);

        }
        return genomeSpaceUsername;
    }
    
    /**
     * The loginFailed flag is used to signal the JSF form that something went wrong with the
     * GenomeSpace login.
     * @return
     */
    public boolean isLoginFailed() {
        HttpSession session = UIBeanHelper.getSession();
        return GenomeSpaceManager.isLoginFailed(session);
    }

    /**
     * Handles submission from a GenomeSpace file menu to delete a GenomeSpace file.
     * Then signals the bean to rebuild the file tree next load, since the tree has changed.
     */
    public void deleteFile() {
        HttpSession session = UIBeanHelper.getSession();
        boolean genomeSpaceEnabled = GenomeSpaceManager.isGenomeSpaceEnabled(session);

        if (!genomeSpaceEnabled) {
            this.setMessageToUser("GenomeSpace is not enabled");
            return;
        }
        
        String url = UIBeanHelper.getRequest().getParameter("url");
        
        GenomeSpaceFile file = GenomeSpaceManager.getFile(session, url);
        if (file == null) {
            file = GenomeSpaceManager.getDirectory(session, url);
        }
        HttpSession httpSession = UIBeanHelper.getSession();
        Object gsSessionObject = httpSession.getAttribute(GenomeSpaceLoginManager.GS_SESSION_KEY);
        
        try { 
            boolean success = GenomeSpaceClientFactory.instance().deleteFile(gsSessionObject, file);
            GenomeSpaceManager.forceFileRefresh(session); // force a refresh
            if (success) { 
                setMessageToUser("Deleted from GenomeSpace " + file.getName());
            }
            else {
                setMessageToUser("Unable to delete in GenomeSpace " + file.getName());
            }
        }
        catch (GenomeSpaceException e) {
            setMessageToUser(e.getLocalizedMessage());
        }
    }
    
    /**
     * Handles submission from a GenomeSpace file menu to create a directory in the targeted parent directory.
     * Then signals the bean to rebuild the file tree next load, since the tree has changed.
     */
    public void createDirectory() {
        HttpSession session = UIBeanHelper.getSession();
        boolean genomeSpaceEnabled = GenomeSpaceManager.isGenomeSpaceEnabled(session);

        if (!genomeSpaceEnabled) {
            this.setMessageToUser("GenomeSpace is not enabled");
            return;
        }
        
        // Find the name of the new directory
        String dirName = null;
        for (Object i : UIBeanHelper.getRequest().getParameterMap().keySet()) {
            if (((String) i).contains("dirName")) {
                String potentialName = UIBeanHelper.getRequest().getParameter((String) i);
                if (potentialName.length() > 0) {
                    dirName = potentialName;
                    break;
                }
            }
        }
        if (dirName == null || dirName.length() == 0) {
            UIBeanHelper.setErrorMessage("Please enter a valid subdirectory name");
            return;
        }
        
        // Get the parent directory
        String url = UIBeanHelper.getRequest().getParameter("parentUrl");
        GenomeSpaceFile parentDir = GenomeSpaceManager.getDirectory(session, url);
        
        HttpSession httpSession = UIBeanHelper.getSession();
        Object gsSessionObject = httpSession.getAttribute(GenomeSpaceLoginManager.GS_SESSION_KEY);
        
        try { 
            GenomeSpaceClientFactory.instance().createDirectory(gsSessionObject, dirName, parentDir);
            GenomeSpaceManager.forceFileRefresh(session); // force a refresh
            setMessageToUser("Created directory " + dirName);
        }
        catch (GenomeSpaceException e) {
            setMessageToUser(e.getLocalizedMessage());
        }
    }
    
    /**
     * Handles submission from the GenomeSpace file menu to save a GenomeSpace file locally.
     */
    public void saveFile() {
        HttpSession session = UIBeanHelper.getSession();
        String url = UIBeanHelper.getRequest().getParameter("url");
        
        try {
            GenomeSpaceFile file = GenomeSpaceManager.getFile(session, url);
            HttpServletResponse response = UIBeanHelper.getResponse();
            response.sendRedirect(file.getUrl().toString());
         
        } 
        catch (Exception e) {
            log.error("Error saving file: " + url + "Message: " + e.getMessage());
        }
    }
    
    /**
     * Returns a map of file kinds to a list of parameters of the current module.
     * This is iterated over to display send to parameters.
     * @return
     */
    public Map<String, List<ParameterInfoWrapper>> getSendToParameters() {
        HttpSession session = UIBeanHelper.getSession();
        TaskInfo currentTaskInfo = GenomeSpaceManager.getCurrentTaskInfo(session);

        if (currentTaskInfo != null) {
            return currentTaskInfo._getKindToParameterInfoMap();
        }
        else {
            return null;
        }
    }

    /**
     * Handles form submission for sending a GenomeSpace file to a GenomeSpace tool (Cytoscape, IGV, etc.).  Then 
     * forwards the user to the URL necessary to send the selected file to the selected tool.
     */
    public void forwardToTool() {
        HttpSession session = UIBeanHelper.getSession();
        String filePath = UIBeanHelper.getRequest().getParameter("file");
        String tool = UIBeanHelper.getRequest().getParameter("tool");
        HttpSession httpSession = UIBeanHelper.getSession();
        Object gsSessionObject = httpSession.getAttribute(GenomeSpaceLoginManager.GS_SESSION_KEY);
        
        if (filePath == null || tool == null) {
            log.error("Null value forwarding to the GenomeSpace tool URL: " + filePath + " " + tool);
        }

        try {
            GenomeSpaceFile file = GenomeSpaceManager.getFile(session, filePath);
            URL url = GenomeSpaceClientFactory.instance().getSendToToolUrl(gsSessionObject, file, tool);
            HttpServletResponse response = UIBeanHelper.getResponse();
            response.sendRedirect(url.toString());
        }
        catch (Exception e) {
            log.error("Error forwarding to the GenomeSpace tool URL: " + e.getMessage());
        }
    }

    /**
     * Associates a GenomeSpace account with a GenePattern account upon successful authentication with
     * the GenePattern account.  Then redirects to the GenePattern index page if successful.
     * @throws IOException
     */
    public void associateAccounts() throws IOException {
        HttpServletRequest request = UIBeanHelper.getRequest();
        HttpServletResponse response = UIBeanHelper.getResponse();
        HttpSession session = UIBeanHelper.getSession();
        
        try {
            log.debug("authenticating from HTTP request...");
            String gpUsername = UserAccountManager.instance().getAuthentication().authenticate(request, response);
            if (log.isDebugEnabled()) {
                if (gpUsername == null) {
                    log.debug("not authenticated (IAuthenticationPlugin.authenticate returned null)");
                }
                else {
                    log.debug("authenticated user='"+gpUsername+"'");
                }
            }
            if (gpUsername == null) {
                throw new AuthenticationException(AuthenticationException.Type.INVALID_USERNAME, "Username was null");
            }
            
            // Associate accounts in database
            LoginManager.instance().addUserIdToSession(request, gpUsername);
            GenomeSpaceLoginManager.loginFromSession(session);
            
            // Forward to index
            String referrer = (String) request.getSession().getAttribute("origin");
            request.getSession().removeAttribute("origin");
            if (referrer == null || referrer.length() == 0) {
                referrer = request.getParameter("origin");
            }
            if (referrer == null || referrer.length() == 0) {
                referrer = request.getContextPath() + "/pages/index.jsf";
            }
            response.sendRedirect(referrer); 
        }
        catch (AuthenticationException e) {
            GenomeSpaceManager.setLoginFailed(session, true);
        }
        catch (GenomeSpaceException e) {
            log.error("GenomeSpaceException in associateAccounts(): " + e.getMessage());
            GenomeSpaceManager.setLoginFailed(session, true);
        }
    }
    
    /**
     * Handles the automatic creation of a GenePattern account and its association with a GenomeSpace 
     * account, if this option is selected from the associate account page.  Then redirects the user
     * to the index page is successful, or back to the associate account page if an error.
     * @throws IOException
     */
    public void autoCreateAccount() throws IOException {
        HttpServletRequest request = UIBeanHelper.getRequest();
        HttpServletResponse response = UIBeanHelper.getResponse();
        HttpSession session = UIBeanHelper.getSession();
        String gsUsername = (String) session.getAttribute(GenomeSpaceLoginManager.GS_USER_KEY);
        String gsEmail = (String) session.getAttribute(GenomeSpaceLoginManager.GS_EMAIL_KEY);
        if (gsUsername == null) {
            log.error("GenomeSpace username was null in autoCreateAccount()");
            gsUsername = GenomeSpaceLoginManager.generatePassword();
        }
        String username = GenomeSpaceLoginManager.generateUsername(gsUsername);
        String password = GenomeSpaceLoginManager.generatePassword();
        GenomeSpaceLoginManager.createGenePatternAccount(username, password, gsEmail);

        try {
            // Associate accounts in database
            LoginManager.instance().addUserIdToSession(request, username);
            GenomeSpaceLoginManager.loginFromSession(session);
            
            // Forward to index
            String referrer = (String) request.getSession().getAttribute("origin");
            request.getSession().removeAttribute("origin");
            if (referrer == null || referrer.length() == 0) {
                referrer = request.getParameter("origin");
            }
            if (referrer == null || referrer.length() == 0) {
                referrer = request.getContextPath() + "/pages/index.jsf";
            }
            response.sendRedirect(referrer);
        }
        catch (GenomeSpaceException e) {
            log.error("GenomeSpaceException in autoCreateAccount(): " + e.getMessage());
            GenomeSpaceManager.setLoginFailed(session, true);
        }
    }
    
    /**
     * Returns if the user has successfully logged in through OpenID
     * @return
     */
    public boolean isOpenID() {
        HttpSession session = UIBeanHelper.getSession();
        Boolean openID = (Boolean) session.getAttribute(GenomeSpaceLoginManager.GS_OPENID_KEY);
        if (openID == null) openID = false;
        return openID;
    }
}