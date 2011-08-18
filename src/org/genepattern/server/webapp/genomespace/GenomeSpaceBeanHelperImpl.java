/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2011) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.
 
 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.webapp.genomespace;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.event.ActionEvent;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.webapp.jsf.UIBeanHelper;
import org.genepattern.server.webservice.server.dao.AdminDAO;
import org.genepattern.util.SemanticUtil;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genomespace.client.GsSession;

import org.genomespace.atm.model.FileParameter;
import org.genomespace.atm.model.WebToolDescriptor;
import org.genomespace.client.ConfigurationUrls;
import org.genomespace.client.DataManagerClient;
import org.genomespace.client.FileParameterWrapper;
import org.genomespace.client.User;
import org.genomespace.client.exceptions.AuthorizationException;
import org.genomespace.client.exceptions.InternalServerException;
import org.genomespace.datamanager.core.GSDataFormat;
import org.genomespace.datamanager.core.GSDirectoryListing;
import org.genomespace.datamanager.core.GSFileMetadata;
import org.genomespace.datamanager.core.impl.GSFileMetadataImpl;

import org.richfaces.component.UITree;
import org.richfaces.model.TreeNode;
import org.richfaces.model.TreeNodeImpl;

/**
 * Backing bean for login to GenomeSpace.
 */
public class GenomeSpaceBeanHelperImpl implements GenomeSpaceBeanHelper {
    private static Logger log = Logger.getLogger(GenomeSpaceBeanHelperImpl.class);

    public static String GS_SESSION_KEY = "GS_SESSION";
    public static String GS_USER_KEY = "GS_USER";
    public static String GS_DIRECTORIES_KEY = "GS_DIRECTORIES";
    
    private String username;
    private String password;
    private String regPassword;
    private String regEmail;
    private boolean unknownUser = false;
    private boolean invalidPassword = false;
    private boolean invalidRegistration = false;
    private boolean loginError = false;
    private String currentTaskLsid;
    private TaskInfo currentTaskInfo;
    private boolean genomeSpaceEnabled = false;

    private Map<String, Set<TaskInfo>> kindToModules;
    private Map<String, List<GSClientUrl>> clientUrls = new HashMap<String, List<GSClientUrl>>();
    private Map<String, List<String>> gsClientTypes = null;

    public GenomeSpaceBeanHelperImpl() {
        String userId = UIBeanHelper.getUserId();
        
        TaskInfo[] moduleArray = new AdminDAO().getLatestTasks(userId);
        List<TaskInfo> allModules = Arrays.asList(moduleArray);
        kindToModules = SemanticUtil.getKindToModulesMap(allModules);
    
        Context userContext = Context.getContextForUser(userId);
        String prop = ServerConfiguration.instance().getGPProperty(userContext, "genomeSpaceEnabled");
        genomeSpaceEnabled = Boolean.parseBoolean(prop);
        log.info("\n\n======= genomeSpaceEnabled=" + genomeSpaceEnabled + " for userId="+userId+"\n\n");
    }

    /* (non-Javadoc)
     * @see org.genepattern.server.webapp.genomespace.GenomeSpaceBeanHelper#isGenomeSpaceEnabled()
     */
    public boolean isGenomeSpaceEnabled() {
        return this.genomeSpaceEnabled;
    }
    
    /* (non-Javadoc)
     * @see org.genepattern.server.webapp.genomespace.GenomeSpaceBeanHelper#setGenomeSpaceEnabled(boolean)
     */
    public void setGenomeSpaceEnabled(boolean genomeSpaceEnabled) {
        this.genomeSpaceEnabled = genomeSpaceEnabled;
    }

    /* (non-Javadoc)
     * @see org.genepattern.server.webapp.genomespace.GenomeSpaceBeanHelper#getRegPassword()
     */
    public String getRegPassword() {
        return regPassword;
    }

    /* (non-Javadoc)
     * @see org.genepattern.server.webapp.genomespace.GenomeSpaceBeanHelper#setRegPassword(java.lang.String)
     */
    public void setRegPassword(String regPassword) {
        this.regPassword = regPassword;
    }

    /* (non-Javadoc)
     * @see org.genepattern.server.webapp.genomespace.GenomeSpaceBeanHelper#getRegEmail()
     */
    public String getRegEmail() {
        return regEmail;
    }

    /* (non-Javadoc)
     * @see org.genepattern.server.webapp.genomespace.GenomeSpaceBeanHelper#setRegEmail(java.lang.String)
     */
    public void setRegEmail(String regEmail) {
        this.regEmail = regEmail;
    }

    /* (non-Javadoc)
     * @see org.genepattern.server.webapp.genomespace.GenomeSpaceBeanHelper#getPassword()
     */
    public String getPassword() {
        return this.password;
    }

    /* (non-Javadoc)
     * @see org.genepattern.server.webapp.genomespace.GenomeSpaceBeanHelper#getUsername()
     */
    public String getUsername() {
        return username;
    }
    
    /* (non-Javadoc)
     * @see org.genepattern.server.webapp.genomespace.GenomeSpaceBeanHelper#setMessageToUser(java.lang.String)
     */
    public void setMessageToUser(String messageToUser) {
        UIBeanHelper.setInfoMessage(messageToUser);
    }

    /* (non-Javadoc)
     * @see org.genepattern.server.webapp.genomespace.GenomeSpaceBeanHelper#isInvalidPassword()
     */
    public boolean isInvalidPassword() {
        return invalidPassword;
    }
    
    /* (non-Javadoc)
     * @see org.genepattern.server.webapp.genomespace.GenomeSpaceBeanHelper#isLoginError()
     */
    public boolean isLoginError() {
        return loginError;
    }

    /* (non-Javadoc)
     * @see org.genepattern.server.webapp.genomespace.GenomeSpaceBeanHelper#isUnknownUser()
     */
    public boolean isUnknownUser() {
        return unknownUser;
    }

    /* (non-Javadoc)
     * @see org.genepattern.server.webapp.genomespace.GenomeSpaceBeanHelper#setPassword(java.lang.String)
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /* (non-Javadoc)
     * @see org.genepattern.server.webapp.genomespace.GenomeSpaceBeanHelper#setUsername(java.lang.String)
     */
    public void setUsername(String username) {
        this.username = username;
    }
    
    /* (non-Javadoc)
     * @see org.genepattern.server.webapp.genomespace.GenomeSpaceBeanHelper#isLoggedIn()
     */
    public boolean isLoggedIn() {
        HttpSession httpSession = UIBeanHelper.getSession();
        GsSession gsSession = (GsSession) httpSession.getAttribute(GS_SESSION_KEY);
        return ((gsSession != null) && (gsSession.isLoggedIn()));
     }
    
    /* (non-Javadoc)
     * @see org.genepattern.server.webapp.genomespace.GenomeSpaceBeanHelper#submitLogin()
     */
    public String submitLogin() {
        String env = UIBeanHelper.getRequest().getParameter("envSelect");
        if (env == null) {
            log.error("Environment for GenomeSpace not set");
            env = "test";
        }
        
        if (username == null) {
            unknownUser = true;
            return "home";
        }

        try {
           ConfigurationUrls.init(env);
           GsSession gsSession = new GsSession();
           User gsUser = gsSession.login(username, password);
           HttpSession httpSession = UIBeanHelper.getSession();
           httpSession.setAttribute(GS_USER_KEY, gsUser);
           httpSession.setAttribute(GS_SESSION_KEY, gsSession);
           GenomeSpaceJobHelper.updateDatabase(UIBeanHelper.getUserId(), gsSession.getAuthenticationToken());
           unknownUser = false;
           this.setMessageToUser("Signed in to GenomeSpace as " + gsUser.getUsername());
            
           return "home";
            
        }  
        catch (AuthorizationException e) {
            log.info("Problem logging into GenomeSpace");
            unknownUser = true;
            this.setMessageToUser("Authentication error, please check your username and password.");
            return "genomeSpaceLoginFailed";
        } 
        catch (Exception e) {
            log.error("Exception logging into GenomeSpace: " + e.getMessage());
            unknownUser = true;
            this.setMessageToUser("An error occurred logging in to GenomeSpace.  Please contact the GenePattern administrator.");
            
            return "genomeSpaceLoginFailed";
        }
    }
    
    /* (non-Javadoc)
     * @see org.genepattern.server.webapp.genomespace.GenomeSpaceBeanHelper#submitRegistration()
     */
    public String submitRegistration() {
        String env = UIBeanHelper.getRequest().getParameter("envSelect");
        if (env == null) {
            log.error("Environment for GenomeSpace not set");
            env = "test";
        }
        
        if (username == null) {
            this.setMessageToUser("GenomeSpace username is blank");
            invalidRegistration = true;
            return "genomeSpaceRegFailed";
        }
        if (! regPassword.equals(password)) {
            UIBeanHelper.setInfoMessage("GenomeSpace password does not match");
            invalidRegistration = true;
            invalidPassword = true;
            return "genomeSpaceRegFailed";
        }
    
        try {
            ConfigurationUrls.init(env);
            GsSession gsSession = new GsSession();
            gsSession.registerUser(username, password, regEmail);
            invalidRegistration = false;
            invalidPassword = false;
            loginError = false;
            submitLogin();
        }
        catch (Exception e) {
            UIBeanHelper.setInfoMessage("Error logging into GenomeSpace");
            invalidRegistration = true;
            loginError = true;
            log.error("Error logging into GenomeSpace" + e.getMessage());
            return "genomeSpaceRegFailed";
        }
      
        return "home";
    }

    /* (non-Javadoc)
     * @see org.genepattern.server.webapp.genomespace.GenomeSpaceBeanHelper#submitLogout()
     */
    public String submitLogout() {
        HttpSession httpSession = UIBeanHelper.getSession();
        
        GsSession gsSession = (GsSession) httpSession.getAttribute(GS_SESSION_KEY);
            
        gsSession.logout();
        httpSession.setAttribute(GS_USER_KEY,null);
        httpSession.setAttribute(GS_SESSION_KEY,null);
            
        this.setMessageToUser("Logged out of GenomeSpace.");
        this.setGenomeSpaceDirectories(null);
            
       
        return "home";
    }
    
    /* (non-Javadoc)
     * @see org.genepattern.server.webapp.genomespace.GenomeSpaceBeanHelper#deleteFileFromGenomeSpace(javax.faces.event.ActionEvent)
     */
    public void deleteFileFromGenomeSpace(ActionEvent ae) {
        String filenameParam = UIBeanHelper.getRequest().getParameter("filename");
        String dirnameParam = UIBeanHelper.getRequest().getParameter("dirname");
        
        GenomeSpaceFileInfo theFile = getFile(dirnameParam, filenameParam);
        HttpSession httpSession = UIBeanHelper.getSession();
      
        GsSession sess = (GsSession) httpSession.getAttribute(GS_SESSION_KEY);
        sess.getDataManagerClient().delete(theFile.gsFile);
        this.setGenomeSpaceDirectories(null); // force a refresh
        this.setMessageToUser("Deleted from GS " + dirnameParam + "/" + filenameParam);
        
    }
    
    /* (non-Javadoc)
     * @see org.genepattern.server.webapp.genomespace.GenomeSpaceBeanHelper#getFile(java.lang.String, java.lang.String)
     */
    public GenomeSpaceFileInfo getFile(String dirname, String file) {
       for (GenomeSpaceDirectory dir: this.getGenomeSpaceDirectories()) {
            if ((dir.getName().equals(dirname)) || (dirname == null)) {
                
                for (GenomeSpaceFileInfo aFile: dir.getGsFiles()) {
                    if (aFile.getFilename().equals(file)) return aFile;
                }
             }
            
            for (GenomeSpaceDirectory aDir: dir.getGsDirectories()) {
                if ((aDir.getName().equals(dirname)) || (dirname == null)) {
                    
                    for (GenomeSpaceFileInfo aFile: aDir.getGsFiles()) {
                        if (aFile.getFilename().equals(file)) return aFile;
                    }
                 }
            }
        }
        return null;
    }
    
    
    /* (non-Javadoc)
     * @see org.genepattern.server.webapp.genomespace.GenomeSpaceBeanHelper#getFileURL(java.lang.String, java.lang.String)
     */
    public String getFileURL(String dirname, String filename) {
        if (filename == null) return null;
        GenomeSpaceFileInfo theFile = getFile(dirname, filename);
        return getFileURL(theFile.gsFile);
    }
    
    /* (non-Javadoc)
     * @see org.genepattern.server.webapp.genomespace.GenomeSpaceBeanHelper#getFileURL(org.genomespace.datamanager.core.GSFileMetadata)
     */
    private String getFileURL(GSFileMetadata gsFile) {
        if (gsFile == null) return null;
        HttpSession httpSession = UIBeanHelper.getSession();
        GsSession sess = (GsSession) httpSession.getAttribute(GS_SESSION_KEY);
        
        URL s3Url = sess.getDataManagerClient().getFileUrl(gsFile, null);
        return s3Url.toString();
    }
    
    /* (non-Javadoc)
     * @see org.genepattern.server.webapp.genomespace.GenomeSpaceBeanHelper#saveFileLocally(javax.faces.event.ActionEvent)
     */
    public void saveFileLocally(ActionEvent ae) {
        String filenameParam = UIBeanHelper.getRequest().getParameter("filename");
        String dirnameParam = UIBeanHelper.getRequest().getParameter("dirname");
        
        try {
            String s3Url = getFileURL(dirnameParam, filenameParam);
            HttpServletResponse response = UIBeanHelper.getResponse();
            response.sendRedirect(s3Url.toString());
         
        } 
        catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
    
    /* (non-Javadoc)
     * @see org.genepattern.server.webapp.genomespace.GenomeSpaceBeanHelper#sendInputFileToGenomeSpace()
     */
    public String sendInputFileToGenomeSpace() {
        String pathParam = UIBeanHelper.getRequest().getParameter("path");
        File theFile = new File(pathParam);
        HttpSession httpSession = UIBeanHelper.getSession();
        
        saveFileToGenomeSpace(httpSession, theFile);
        return "home";
        
    }
    
    /* (non-Javadoc)
     * @see org.genepattern.server.webapp.genomespace.GenomeSpaceBeanHelper#sendToGenomeSpace()
     */
    public String sendToGenomeSpace() {
        String filenameParam = UIBeanHelper.getRequest().getParameter("jobFileName");
        String jobFileName = UIBeanHelper.decode(filenameParam);
         
        int idx = jobFileName.indexOf('/');
        
        String jobNumber = jobFileName.substring(0, idx);
        String filename = jobFileName.substring(idx+1);
        
        HttpSession httpSession = UIBeanHelper.getSession();
        File in = new File(GenePatternAnalysisTask.getJobDir(jobNumber), filename);
        
        saveFileToGenomeSpace(httpSession, in);
        return "home";     
    }

    /**
     * @param httpSession
     * @param in
     */
    private GSFileMetadata saveFileToGenomeSpace(HttpSession httpSession, File in) {
        GSFileMetadata metadata = null;
        try {
            GsSession sess = (GsSession) httpSession.getAttribute(GS_SESSION_KEY);
            DataManagerClient dmClient = sess.getDataManagerClient();
            GSDirectoryListing rootDir = dmClient.listDefaultDirectory();
            metadata = dmClient.uploadFile(in, rootDir.getDirectory());
        
            UIBeanHelper.setInfoMessage("File uploaded to GS " + in.getName());
            this.setGenomeSpaceDirectories(null);
            
        } 
        catch (Exception e) {
            e.printStackTrace();
            UIBeanHelper.setErrorMessage("There was a problem uploading the file to GS, " + in.getName());
         }
        return metadata;
    }
    
    private List<WebToolDescriptor> getGSClients() {
        HttpSession httpSession = UIBeanHelper.getSession();
        GsSession gsSession = (GsSession) httpSession.getAttribute(GS_SESSION_KEY);
        List<WebToolDescriptor> tools;
        try {
            tools = gsSession.getAnalysisToolManagerClient().getWebTools();
        }
        catch (InternalServerException e) {
            log.error("Error getting getAnalysisToolManagerClient().getWebTools().  Session: " + gsSession + " Message: " + e.getMessage());
            return new ArrayList<WebToolDescriptor>();
        }
        WebToolDescriptor gp = null;  // Remove GenePattern from the list
        for (WebToolDescriptor i : tools) { 
            if (i.getName().equals("GenePattern")) {
                gp = i;
            }
        }
        tools.remove(gp);
        
        if (gsClientTypes == null) {
            initGSClientTypesMap(tools);
        }
        
        return tools;
    }
    
    /* (non-Javadoc)
     * @see org.genepattern.server.webapp.genomespace.GenomeSpaceBeanHelper#getToolWrappers()
     */
    public List<WebToolDescriptorWrapper> getToolWrappers() {
        List<WebToolDescriptorWrapper> wrappers = new ArrayList<WebToolDescriptorWrapper>();
        for (WebToolDescriptor i : getGSClients()) {
            wrappers.add(new WebToolDescriptorWrapper(i.getName(), this));
        }
        return wrappers;
    }
    
    private void initGSClientTypesMap(List<WebToolDescriptor> tools) {
        gsClientTypes = new HashMap<String, List<String>>();
        for (WebToolDescriptor i : tools) {
            List<String> types = prepareTypesFilter(i.getFileParameters());
            gsClientTypes.put(i.getName(), types);
        }
    }
    
    /* (non-Javadoc)
     * @see org.genepattern.server.webapp.genomespace.GenomeSpaceBeanHelper#getGsClientTypes()
     */
    public Map<String, List<String>> getGsClientTypes() {
        return gsClientTypes;
    }
    
    private void testClientMap() {
        for (String i : gsClientTypes.keySet()) {
            log.info("TOOL: " + i);
            log.info("\tTYPES: ");
            for (String j : gsClientTypes.get(i)) {
                log.info("\t\t" + j);
            }
        }
    }
    
    /* (non-Javadoc)
     * @see org.genepattern.server.webapp.genomespace.GenomeSpaceBeanHelper#getClientUrls()
     */
    public Map<String, List<GSClientUrl>>getClientUrls() {
        return clientUrls;
    }
    
    /* (non-Javadoc)
     * @see org.genepattern.server.webapp.genomespace.GenomeSpaceBeanHelper#addToClientUrls(org.genepattern.server.webapp.genomespace.GenomeSpaceFileInfo)
     */
    public void addToClientUrls(GenomeSpaceFileInfo file) {
        clientUrls.put(file.getKey(), getGSClientURLs(file));
    }
    
    /* (non-Javadoc)
     * @see org.genepattern.server.webapp.genomespace.GenomeSpaceBeanHelper#sendGSFileToGSClient()
     */
    public void sendGSFileToGSClient() throws IOException {
        String fileParam = UIBeanHelper.getRequest().getParameter("file");
        String toolParam = UIBeanHelper.getRequest().getParameter("tool");
        List<GSClientUrl> urls = clientUrls.get(fileParam);
        for (GSClientUrl i : urls) {
            if (i.getTool().equals(toolParam)) {
                UIBeanHelper.getResponse().sendRedirect(i.getUrl().toString());
                break;
            }
        }
    }
    
    /* (non-Javadoc)
     * @see org.genepattern.server.webapp.genomespace.GenomeSpaceBeanHelper#sendInputFileToGSClient()
     */
    public void sendInputFileToGSClient() throws IOException {
        String fileParam = UIBeanHelper.getRequest().getParameter("file");
        String toolParam = UIBeanHelper.getRequest().getParameter("tool");
        File file = new File(fileParam);
        if (!file.exists()) { 
            UIBeanHelper.setErrorMessage("Unable to upload input file to GenomeSpace");
            return;
        }
        GSFileMetadata metadata = saveFileToGenomeSpace(UIBeanHelper.getSession(), file);
        GenomeSpaceFileInfo gsFile = new GenomeSpaceFileInfo(metadata, null);
        List<GSClientUrl> urls = getGSClientURLs(gsFile);

        for (GSClientUrl i : urls) {
            if (i.getTool().equals(toolParam)) {
                UIBeanHelper.getResponse().sendRedirect(i.getUrl().toString());
                break;
            }
        }
    }
    
    private List<String> prepareTypesFilter(List<FileParameter> params) {
        Set<GSDataFormat> superset = new HashSet<GSDataFormat>();
        List<String> types = new ArrayList<String>();
        for (FileParameter i : params) {
            superset.addAll(i.getFormats());
        }
        for (GSDataFormat i : superset) {
            types.add(i.getName());
        }
        return types;
    }
    
    private List<FileParameterWrapper> prepareFileParameterWrappers(List<FileParameter> params, GSFileMetadata metadata) {
        List<FileParameterWrapper> wrappers = new ArrayList<FileParameterWrapper>();
        for (FileParameter i : params) {
            wrappers.add(new FileParameterWrapper(i, metadata));
        }
        return wrappers;
    }
    
    private boolean typeMatchesTool(String type, WebToolDescriptor tool) {
        List<String> toolTypes = gsClientTypes.get(tool.getName());
        for (String i : toolTypes) {
            if (i.equals(type)) {
                return true;
            }
        }
        return false;
    }
    
    /* (non-Javadoc)
     * @see org.genepattern.server.webapp.genomespace.GenomeSpaceBeanHelper#getGSClientURLs(org.genepattern.server.webapp.genomespace.GenomeSpaceFileInfo)
     */
    public List<GSClientUrl> getGSClientURLs(GenomeSpaceFileInfo file)  {
        GSFileMetadata metadata = file.gsFile;
        HttpSession httpSession = UIBeanHelper.getSession();
        GsSession gsSession = (GsSession) httpSession.getAttribute(GS_SESSION_KEY);
        List<WebToolDescriptor> tools = getGSClients();
        List<GSClientUrl> urls = new ArrayList<GSClientUrl>();
        for (WebToolDescriptor i : tools) {
            List<FileParameterWrapper> wrappers = prepareFileParameterWrappers(i.getFileParameters(), metadata);
            URL url = null;
            try {
                url = gsSession.getAnalysisToolManagerClient().getWebToolLaunchUrl(i, wrappers);
            }
            catch (InternalServerException e) {
                log.error("Error getting gs url. Session: " + gsSession + " WebToolDescriptor: " + i + " FileParameterWrappers: " + wrappers + " Message: " + e.getMessage());
            }
            urls.add(new GSClientUrl(i.getName(), url));
        }
        return urls;
    }
    
    /* (non-Javadoc)
     * @see org.genepattern.server.webapp.genomespace.GenomeSpaceBeanHelper#openTreeNode(org.richfaces.component.UITree)
     */
    public boolean openTreeNode(UITree tree) {
        return true;
    }
    
    /* (non-Javadoc)
     * @see org.genepattern.server.webapp.genomespace.GenomeSpaceBeanHelper#getFileTree()
     */
    public TreeNode<GenomeSpaceFileInfo> getFileTree() {
        // Set up the root node
        TreeNode<GenomeSpaceFileInfo> rootNode = new TreeNodeImpl<GenomeSpaceFileInfo>();
        GSFileMetadata rootFileFacade = new GSFileMetadataImpl("GenomeSpace Files", null, UIBeanHelper.getServer(), UIBeanHelper.getUserId(), 0, null, null, true);
        GenomeSpaceFileInfo rootWrapper = new GenomeSpaceFileInfo(rootFileFacade, null);
        rootNode.setData(rootWrapper);
        
        // Add component trees
        List<GenomeSpaceDirectory> dirs = getAvailableDirectories();
        int count = 0;
        for (GenomeSpaceDirectory i : dirs) {
            rootNode.addChild(count, getGenomeSpaceFilesTree(i));
            count++;
        }
        
        return rootNode;
    }
    
    /* (non-Javadoc)
     * @see org.genepattern.server.webapp.genomespace.GenomeSpaceBeanHelper#getGenomeSpaceFilesTree(org.genepattern.server.webapp.genomespace.GenomeSpaceDirectory)
     */
    public TreeNode<GenomeSpaceFileInfo> getGenomeSpaceFilesTree(GenomeSpaceDirectory gsDir) {
        GSFileMetadata metadataFacade = new GSFileMetadataImpl(gsDir.getName(), null, UIBeanHelper.getServer(), UIBeanHelper.getUserId(), 0, null, null, true);
        GenomeSpaceFileInfo wrapper = new GenomeSpaceFileInfo(metadataFacade, null);
        TreeNode<GenomeSpaceFileInfo> rootNode = new TreeNodeImpl<GenomeSpaceFileInfo>();
        rootNode.setData(wrapper);
        int count = 0;
        
        // Add subdirectories
        for (GenomeSpaceDirectory i : gsDir.getGsDirectories()) {
            rootNode.addChild(count, getGenomeSpaceFilesTree(i));
            count++;
        }
        
        // Add child files
        for (GenomeSpaceFileInfo i : gsDir.getGsFiles()) {
            TreeNode<GenomeSpaceFileInfo> fileNode = new TreeNodeImpl<GenomeSpaceFileInfo>();
            fileNode.setData(i);
            rootNode.addChild(count, fileNode);
            count++;
        }

        return rootNode;
    }
    
    /* (non-Javadoc)
     * @see org.genepattern.server.webapp.genomespace.GenomeSpaceBeanHelper#getGsDirectories()
     */
    public List<GenomeSpaceDirectory> getGsDirectories() {
        return getAvailableDirectories().get(0).getGsDirectories();
    }
    
    /* (non-Javadoc)
     * @see org.genepattern.server.webapp.genomespace.GenomeSpaceBeanHelper#getGsFiles()
     */
    public List<GenomeSpaceFileInfo> getGsFiles() {
        return getAvailableDirectories().get(0).getGsFiles();
    }
    
    /* (non-Javadoc)
     * @see org.genepattern.server.webapp.genomespace.GenomeSpaceBeanHelper#getAvailableDirectories()
     */
    public List<GenomeSpaceDirectory> getAvailableDirectories() {
        List<GenomeSpaceDirectory> availableDirectories = this.getGenomeSpaceDirectories();
        
        if ((availableDirectories == null) || (availableDirectories.size() == 0)) {
            availableDirectories = new ArrayList<GenomeSpaceDirectory>();
            
           
            HttpSession httpSession = UIBeanHelper.getSession();
            GsSession gsSession = (GsSession) httpSession.getAttribute(GS_SESSION_KEY);
            User gsUser = (User) httpSession.getAttribute(GS_USER_KEY);
            if ((gsSession == null) || (! gsSession.isLoggedIn())) return availableDirectories;
            
            DataManagerClient dmClient = gsSession.getDataManagerClient();
            GSDirectoryListing rootDir = dmClient.listDefaultDirectory();
            
            GenomeSpaceDirectory userDir = new GenomeSpaceDirectory(rootDir, dmClient, kindToModules, this);
         
            availableDirectories.add(userDir);
            this.setGenomeSpaceDirectories(availableDirectories);
        }
        return availableDirectories;
    }

    /* (non-Javadoc)
     * @see org.genepattern.server.webapp.genomespace.GenomeSpaceBeanHelper#getGenomeSpaceDirectories()
     */
    public List<GenomeSpaceDirectory> getGenomeSpaceDirectories() {
        return (List<GenomeSpaceDirectory>) UIBeanHelper.getSession().getAttribute(GS_DIRECTORIES_KEY);
    }
    
    /* (non-Javadoc)
     * @see org.genepattern.server.webapp.genomespace.GenomeSpaceBeanHelper#setGenomeSpaceDirectories(java.util.List)
     */
    public void setGenomeSpaceDirectories(List<GenomeSpaceDirectory> dirs) {
        UIBeanHelper.getSession().setAttribute(GS_DIRECTORIES_KEY, dirs);
    }
    
    /* (non-Javadoc)
     * @see org.genepattern.server.webapp.genomespace.GenomeSpaceBeanHelper#getSendToParameters(java.lang.String)
     */
    public List<ParameterInfo> getSendToParameters(String type) {
        if (currentTaskInfo == null && currentTaskLsid != null && currentTaskLsid.length() != 0) {
            initCurrentLsid();
        }
        else if (currentTaskInfo == null && (currentTaskLsid == null || currentTaskLsid.length() == 0)) {
            return null;
        }
        return currentTaskInfo._getSendToParameterInfos(type);
    }
    
    /* (non-Javadoc)
     * @see org.genepattern.server.webapp.genomespace.GenomeSpaceBeanHelper#initCurrentLsid()
     */
    public void initCurrentLsid() {
        String currentUser = UIBeanHelper.getUserId();
        AdminDAO adminDao = new AdminDAO();
        this.currentTaskInfo = adminDao.getTask(currentTaskLsid, currentUser);
    }
    
    /* (non-Javadoc)
     * @see org.genepattern.server.webapp.genomespace.GenomeSpaceBeanHelper#setSelectedModule(java.lang.String)
     */
    public void setSelectedModule(String selectedModule) {
        this.currentTaskLsid = selectedModule;
        initCurrentLsid();
    }
}
