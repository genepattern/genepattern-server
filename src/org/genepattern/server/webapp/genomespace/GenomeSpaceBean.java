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
import org.genomespace.atm.model.FileParameter;
import org.genomespace.atm.model.WebToolDescriptor;
import org.genomespace.client.ConfigurationUrls;
import org.genomespace.client.DataManagerClient;
import org.genomespace.client.FileParameterWrapper;
import org.genomespace.client.GsSession;
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
public class GenomeSpaceBean {
    private static Logger log = Logger.getLogger(GenomeSpaceBean.class);

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
    
    public GenomeSpaceBean() {
        String userId = UIBeanHelper.getUserId();
        
        TaskInfo[] moduleArray = new AdminDAO().getLatestTasks(userId);
        List<TaskInfo> allModules = Arrays.asList(moduleArray);
        kindToModules = SemanticUtil.getKindToModulesMap(allModules);
    
        Context userContext = Context.getContextForUser(userId);
        String prop = ServerConfiguration.instance().getGPProperty(userContext, "genomeSpaceEnabled");
        genomeSpaceEnabled = Boolean.parseBoolean(prop);
        log.info("\n\n======= genomeSpaceEnabled=" + genomeSpaceEnabled + " for userId="+userId+"\n\n");
    }

    public boolean isGenomeSpaceEnabled() {
        return this.genomeSpaceEnabled;
    }
    
    public void setGenomeSpaceEnabled(boolean genomeSpaceEnabled) {
        this.genomeSpaceEnabled = genomeSpaceEnabled;
    }

    public String getRegPassword() {
        return regPassword;
    }

    public void setRegPassword(String regPassword) {
        this.regPassword = regPassword;
    }

    public String getRegEmail() {
        return regEmail;
    }

    public void setRegEmail(String regEmail) {
        this.regEmail = regEmail;
    }

    public String getPassword() {
        return this.password;
    }

    public String getUsername() {
        return username;
    }
    
    public void setMessageToUser(String messageToUser) {
        UIBeanHelper.setInfoMessage(messageToUser);
    }

    public boolean isInvalidPassword() {
        return invalidPassword;
    }
    
    public boolean isLoginError() {
        return loginError;
    }

    public boolean isUnknownUser() {
        return unknownUser;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setUsername(String username) {
        this.username = username;
    }
    
    public boolean isLoggedIn() {
        HttpSession httpSession = UIBeanHelper.getSession();
        GsSession gsSession = (GsSession) httpSession.getAttribute(GS_SESSION_KEY);
        return ((gsSession != null) && (gsSession.isLoggedIn()));
     }
    
    /**
     * Submit the user / password. For now this uses an action listener since we are redirecting to a page outside of
     * the JSF framework. This should be changed to an action to use jsf navigation in the future.
     * @param event -- ignored        
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
           GenomeSpaceJobHelper.updateDatabase(UIBeanHelper.getUserId(), gsSession);
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
            log.error("Exception logging into GenomeSpace>: " + e.getMessage());
            unknownUser = true;
            this.setMessageToUser("An error occurred logging in to GenomeSpace.  Please contact the GenePattern administrator.");
            
            return "genomeSpaceLoginFailed";
        }
    }
    
    /**
     * register a user into GenomeSpace
     * @return
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
    
    /**
     * Delete a file from the user's home dir on GenomeSpace
     * @param ae
     */
    public void deleteFileFromGenomeSpace(ActionEvent ae) throws InternalServerException{
        String filenameParam = UIBeanHelper.getRequest().getParameter("filename");
        String dirnameParam = UIBeanHelper.getRequest().getParameter("dirname");
        
        GenomeSpaceFileInfo theFile = getFile(dirnameParam, filenameParam);
        HttpSession httpSession = UIBeanHelper.getSession();
      
        GsSession sess = (GsSession) httpSession.getAttribute(GS_SESSION_KEY);
        sess.getDataManagerClient().delete(theFile.gsFile);
        this.setGenomeSpaceDirectories(null); // force a refresh
        this.setMessageToUser("Deleted from GS " + dirnameParam + "/" + filenameParam);
        
    }
    
    /**
     * lots of room for optimization and caching here
     * @param dirname
     * @param file
     * @return
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
    
    
    /**
     * gets a one time use link to the file on S3
     * @param ae
     */
    public String getFileURL(String dirname, String filename) {
        if (filename == null) return null;
        GenomeSpaceFileInfo theFile = getFile(dirname, filename);
        return getFileURL(theFile.gsFile);
    }
    
    public String getFileURL(GSFileMetadata gsFile) {
        if (gsFile == null) return null;
        HttpSession httpSession = UIBeanHelper.getSession();
        GsSession sess = (GsSession) httpSession.getAttribute(GS_SESSION_KEY);
        
        URL s3Url = sess.getDataManagerClient().getFileUrl(gsFile, null);
        return s3Url.toString();
    }
    
    /**
     * redirects to a time limited, one time use link to the file on S3
     * @param ae
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
    
    /**
     * Save a local GenePattern result file back to the GenomeSpace data repository
     * @return
     */
    public String sendInputFileToGenomeSpace() {
        String pathParam = UIBeanHelper.getRequest().getParameter("path");
        File theFile = new File(pathParam);
        HttpSession httpSession = UIBeanHelper.getSession();
        
        saveFileToGenomeSpace(httpSession, theFile);
        return "home";
        
    }
    
    /**
     * Save a local GenePattern result file back to the GenomeSpace data repository
     * @return
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
    
    public List<WebToolDescriptor> getGSClients() throws InternalServerException {
        HttpSession httpSession = UIBeanHelper.getSession();
        GsSession gsSession = (GsSession) httpSession.getAttribute(GS_SESSION_KEY);
        List<WebToolDescriptor> tools = gsSession.getAnalysisToolManagerClient().getWebTools();
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
    
    public List<WebToolDescriptorWrapper> getToolWrappers() throws InternalServerException {
        List<WebToolDescriptorWrapper> wrappers = new ArrayList<WebToolDescriptorWrapper>();
        for (WebToolDescriptor i : getGSClients()) {
            wrappers.add(new WebToolDescriptorWrapper(i));
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
    
    private void testClientMap() {
        for (String i : gsClientTypes.keySet()) {
            log.info("TOOL: " + i);
            log.info("\tTYPES: ");
            for (String j : gsClientTypes.get(i)) {
                log.info("\t\t" + j);
            }
        }
    }
    
    public Map<String, List<GSClientUrl>>getClientUrls() {
        return clientUrls;
    }
    
    public void addToClientUrls(GenomeSpaceFileInfo file) {
        try {
            clientUrls.put(file.getKey(), getGSClientURLs(file));
        }
        catch (InternalServerException e) {
            log.error("Error adding GS file to map:" + file);
        }
    }
    
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
    
    public void sendInputFileToGSClient() throws IOException, InternalServerException {
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
    
    public List<GSClientUrl> getGSClientURLs(GenomeSpaceFileInfo file) throws InternalServerException {
        GSFileMetadata metadata = file.gsFile;
        HttpSession httpSession = UIBeanHelper.getSession();
        GsSession gsSession = (GsSession) httpSession.getAttribute(GS_SESSION_KEY);
        List<WebToolDescriptor> tools = getGSClients();
        List<GSClientUrl> urls = new ArrayList<GSClientUrl>();
        for (WebToolDescriptor i : tools) {
            List<FileParameterWrapper> wrappers = prepareFileParameterWrappers(i.getFileParameters(), metadata);
            URL url = gsSession.getAnalysisToolManagerClient().getWebToolLaunchUrl(i, wrappers);
            urls.add(new GSClientUrl(i.getName(), url));
        }
        return urls;
    }
    
    public boolean openTreeNode(UITree tree) {
        return true;
    }
    
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
    
    public List<GenomeSpaceDirectory> getGsDirectories() {
        return getAvailableDirectories().get(0).getGsDirectories();
    }
    
    public List<GenomeSpaceFileInfo> getGsFiles() {
        return getAvailableDirectories().get(0).getGsFiles();
    }
    
    /**
     * get the list of directories in GenomeSpace this user can look at
     * @return
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

    public List<GenomeSpaceDirectory> getGenomeSpaceDirectories() {
        return (List<GenomeSpaceDirectory>) UIBeanHelper.getSession().getAttribute(GS_DIRECTORIES_KEY);
    }
    
    public void setGenomeSpaceDirectories(List<GenomeSpaceDirectory> dirs) {
        UIBeanHelper.getSession().setAttribute(GS_DIRECTORIES_KEY, dirs);
    }
    
    public List<ParameterInfo> getSendToParameters(String type) {
        if (currentTaskInfo == null && currentTaskLsid != null && currentTaskLsid.length() != 0) {
            initCurrentLsid();
        }
        else if (currentTaskInfo == null && (currentTaskLsid == null || currentTaskLsid.length() == 0)) {
            return null;
        }
        return currentTaskInfo._getSendToParameterInfos(type);
    }
    
    public void initCurrentLsid() {
        String currentUser = UIBeanHelper.getUserId();
        AdminDAO adminDao = new AdminDAO();
        this.currentTaskInfo = adminDao.getTask(currentTaskLsid, currentUser);
    }
    
    public void setSelectedModule(String selectedModule) {
        this.currentTaskLsid = selectedModule;
        initCurrentLsid();
    }
    
    public class GSClientUrl {
        String tool;
        URL url;
        
        GSClientUrl(String tool, URL url) {
            this.tool = tool;
            this.url = url;
        }
        
        public String getTool() {
            return tool;
        }
        
        public void setTool(String tool) {
            this.tool = tool;
        }
        
        public URL getUrl() {
            return url;
        }
        
        public void setUrl(URL url) {
            this.url = url;
        }
    }
    
    public class WebToolDescriptorWrapper {
        WebToolDescriptor tool;
        Map<String, Boolean> typeMap = new HashMap<String, Boolean>();
        boolean init = false;
        
        WebToolDescriptorWrapper(WebToolDescriptor tool) {
            this.tool = tool;
        }
        
        public WebToolDescriptor getTool() {
            return tool;
        }
        
        public void setTool(WebToolDescriptor tool) {
            this.tool = tool;
        }
        
        public Map<String, Boolean> getTypeMap() {
            if (!init) {
                List<String> types = gsClientTypes.get(tool.getName());
                for (String i : types) {
                    typeMap.put(i, true);
                }
                init = true;
            }
            
            return typeMap;
        }
        
        public void setTypeMap(Map<String, Boolean> typeMap) {
            this.typeMap = typeMap;
        }
    }
}
