package org.genepattern.server.gs;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import org.richfaces.component.UITree;
import org.richfaces.model.TreeNode;
import org.richfaces.model.TreeNodeImpl;

/**
 * TODO: need documentation, including the purpose and scope of this bean.
 * @author TODO: need author
 *
 */
public class GenomeSpaceBean {
    private static Logger log = Logger.getLogger(GenomeSpaceBean.class);

    public static String GS_SESSION_KEY = "GS_SESSION";
    public static String GS_USER_KEY = "GS_USER";
    public static String GS_DIRECTORIES_KEY = "GS_DIRECTORIES";
    public static String GSFILEMETADATAS = "GSFILEMETADATAS";

    private String username;
    private String password;
    private String regPassword;
    private String regEmail;
    private boolean unknownUser = false;
    private boolean invalidPassword = false;
    private boolean loginError = false;
    private String currentTaskLsid;
    private TaskInfo currentTaskInfo;
    private boolean genomeSpaceEnabled = false;

    private Map<String, Set<TaskInfo>> kindToModules;
    private Map<String, List<GsClientUrl>> clientUrls = new HashMap<String, List<GsClientUrl>>();
    private Map<String, List<String>> gsClientTypes = new HashMap<String, List<String>>();
    private List<WebToolDescriptorWrapper> toolWrappers = null;

    public GenomeSpaceBean() {
        String userId = UIBeanHelper.getUserId();

        //this is a costly operation, do we want to do this when genomeSpaceEnabled is false?
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
        Object gsSessionObj = httpSession.getAttribute(GS_SESSION_KEY);
        return GsClientFactory.getGsClientUtil().isLoggedIn(gsSessionObj);
    }

    public String submitLogin() {
        if (username == null) {
            unknownUser = true;
            return "home";
        } 
        String env = UIBeanHelper.getRequest().getParameter("envSelect");
        if (env == null) {
            log.error("Environment for GenomeSpace not set");
            env = "test";
        }
        try {
            unknownUser = true;
            GsLoginResponse login = GsClientFactory.getGsClientUtil().submitLogin(env, username, password);
            unknownUser = login.unknownUser;
            
            HttpSession httpSession = UIBeanHelper.getSession();
            for(Entry<String,Object> entry : login.attrs.entrySet()) {
                httpSession.setAttribute(entry.getKey(), entry.getValue());
                GenomeSpaceJobHelper.updateDatabase(UIBeanHelper.getUserId(), login.gsAuthenticationToken);
            }
            this.setMessageToUser("Signed in to GenomeSpace as " + login.gsUsername);
            return "home";
        }
        catch (GsClientException e) {
            unknownUser = false;
            this.setMessageToUser(e.getLocalizedMessage());
            return "genomeSpaceLoginFailed";
        }
    }

    public String submitRegistration() {
        String env = UIBeanHelper.getRequest().getParameter("envSelect");
        if (env == null) {
            log.error("Environment for GenomeSpace not set");
            env = "test";
        }
        
        if (username == null) {
            this.setMessageToUser("GenomeSpace username is blank");
            return "genomeSpaceRegFailed";
        }
        if (! regPassword.equals(password)) {
            UIBeanHelper.setInfoMessage("GenomeSpace password does not match");
            invalidPassword = true;
            return "genomeSpaceRegFailed";
        }
    
        try {
            GsClientFactory.getGsClientUtil().registerUser(env, username, password, regEmail);
            invalidPassword = false;
            loginError = false;
            submitLogin();
        }
        catch (GsClientException e) {
            log.error(e);
            UIBeanHelper.setInfoMessage(e.getLocalizedMessage());
            loginError = true;
            return "genomeSpaceRegFailed";
        }
      
        return "home";
    }

    public String submitLogout() {
        HttpSession httpSession = UIBeanHelper.getSession();
        Object gsSessionObj = httpSession.getAttribute(GS_SESSION_KEY);
        
        GsClientFactory.getGsClientUtil().logout( gsSessionObj );
        httpSession.setAttribute(GS_USER_KEY,null);
        httpSession.setAttribute(GS_SESSION_KEY,null);
            
        this.setMessageToUser("Logged out of GenomeSpace.");
        this.setGenomeSpaceDirectories(null);
       
        return "home";
    }

    public void deleteFileFromGenomeSpace(ActionEvent ae) {
        String filenameParam = UIBeanHelper.getRequest().getParameter("filename");
        String dirnameParam = UIBeanHelper.getRequest().getParameter("dirname");
        
        GenomeSpaceFileInfo theFile = getFile(dirnameParam, filenameParam);
        HttpSession httpSession = UIBeanHelper.getSession();
        Object gsSessionObj = httpSession.getAttribute(GS_SESSION_KEY);
        
        try { 
            GsClientFactory.getGsClientUtil().deleteFile(gsSessionObj, theFile);
            this.setGenomeSpaceDirectories(null); // force a refresh
            this.setMessageToUser("Deleted from GS " + dirnameParam + "/" + filenameParam);
        }
        catch (GsClientException e) {
            this.setMessageToUser(e.getLocalizedMessage());
        }
    }

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

    public String getFileURL(String dirname, String filename) {
        if (filename == null) return null;
        GenomeSpaceFileInfo theFile = getFile(dirname, filename);
        return theFile.getUrl();
    }

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

    public String sendInputFileToGenomeSpace() {
        String pathParam = UIBeanHelper.getRequest().getParameter("path");
        File theFile = new File(pathParam);
        HttpSession httpSession = UIBeanHelper.getSession();
        
        saveFileToGenomeSpace(httpSession, theFile);
        return "home";
    }

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

    public Map<String, List<String>> getGsClientTypes() {
        if (gsClientTypes == null) {
            gsClientTypes = new HashMap<String, List<String>>();
        }
        if (gsClientTypes.size() == 0) {
            HttpSession httpSession = UIBeanHelper.getSession();
            Object gsSessionObj = httpSession.getAttribute(GS_SESSION_KEY);
            GsClientFactory.getGsClientUtil().initGsClientTypes(gsSessionObj, gsClientTypes);
        }
        return gsClientTypes;
    }

    public Map<String, List<GsClientUrl>>getClientUrls() {
        return clientUrls;
    }

    public void addToClientUrls(GenomeSpaceFileInfo file) {
        clientUrls.put(file.getKey(), getGSClientURLs(file));
    }

    public void sendGSFileToGSClient() throws IOException {
        String fileParam = UIBeanHelper.getRequest().getParameter("file");
        String toolParam = UIBeanHelper.getRequest().getParameter("tool");
        List<GsClientUrl> urls = clientUrls.get(fileParam);
        for (GsClientUrl i : urls) {
            if (i.getTool().equals(toolParam)) {
                UIBeanHelper.getResponse().sendRedirect(i.getUrl().toString());
                break;
            }
        }
    }

    public void sendInputFileToGSClient() throws IOException {
        String fileParam = UIBeanHelper.getRequest().getParameter("file");
        String toolParam = UIBeanHelper.getRequest().getParameter("tool");
        File file = new File(fileParam);
        if (!file.exists()) { 
            UIBeanHelper.setErrorMessage("Unable to upload input file to GenomeSpace");
            return;
        }
        GenomeSpaceFileInfo gsFile = saveFileToGenomeSpace(UIBeanHelper.getSession(), file);
        List<GsClientUrl> urls = getGSClientURLs(gsFile);

        for (GsClientUrl i : urls) {
            if (i.getTool().equals(toolParam)) {
                UIBeanHelper.getResponse().sendRedirect(i.getUrl().toString());
                break;
            }
        }
    }
    
    public List<GsClientUrl> getGSClientURLs(GenomeSpaceFileInfo file)  {
        HttpSession httpSession = UIBeanHelper.getSession();
        Object gsSessionObj = httpSession.getAttribute(GS_SESSION_KEY);
        return GsClientFactory.getGsClientUtil().getGSClientURLs(gsSessionObj, file);
    }
    
    
    public boolean openTreeNode(UITree tree) {
        return true;
    }
    
    public TreeNode<GenomeSpaceFileInfo> getFileTree() {
        // Set up the root node
        TreeNode<GenomeSpaceFileInfo> rootNode = new TreeNodeImpl<GenomeSpaceFileInfo>();
        
        GenomeSpaceFileInfo rootWrapper = new GenomeSpaceFileInfo(null, "GenomeSpace Files", GenomeSpaceFileInfo.DIRECTORY, new HashSet<String>(), null, null, gsClientTypes);
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
        GenomeSpaceFileInfo wrapper = new GenomeSpaceFileInfo(null, gsDir.getName(), GenomeSpaceFileInfo.DIRECTORY, new HashSet<String>(), null, null, gsClientTypes);
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
    
    public List<GenomeSpaceDirectory> getAvailableDirectories() {
        List<GenomeSpaceDirectory> availableDirectories = this.getGenomeSpaceDirectories(); 
        if ((availableDirectories == null) || (availableDirectories.size() == 0)) {
            availableDirectories = initUserDirs();
            //initialize the send to parameters
            for(GenomeSpaceDirectory dir : availableDirectories) {
                for(GenomeSpaceFileInfo file : dir.getGsFiles()) {
                    String type = file.getType();
                    List<ParameterInfo> sendToParams = this.getSendToParameters(type);
                    for(ParameterInfo p : sendToParams) {
                        file.addSendToParameter(p);
                    }
                }
            }
            this.setGenomeSpaceDirectories(availableDirectories);
        }
        return availableDirectories;
    }

    private List<GenomeSpaceDirectory> initUserDirs() {
        HttpSession httpSession = UIBeanHelper.getSession();
        Object gsSessionObj = httpSession.getAttribute(GS_SESSION_KEY);
        return GsClientFactory.getGsClientUtil().initUserDirs(gsSessionObj, kindToModules, gsClientTypes, clientUrls);
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

    public List<WebToolDescriptorWrapper> getToolWrappers() {
        if (toolWrappers == null) {
            toolWrappers = initToolWrappers();
        }
        return toolWrappers;
    } 

   //----- end GenomeSpaceBean implementation --------- 
    private List<WebToolDescriptorWrapper> initToolWrappers() {
        HttpSession httpSession = UIBeanHelper.getSession();
        Object gsSessionObj = httpSession.getAttribute(GS_SESSION_KEY);
        return GsClientFactory.getGsClientUtil().getToolWrappers(gsSessionObj, gsClientTypes);
    }
    
    /**
     * @param httpSession
     * @param in
     */
    private GenomeSpaceFileInfo saveFileToGenomeSpace(HttpSession httpSession, File in) {
        GenomeSpaceFileInfo gsFileInfo = null;
        try {
            Object gsSessionObj = httpSession.getAttribute(GS_SESSION_KEY);
            gsFileInfo = GsClientFactory.getGsClientUtil().saveFileToGenomeSpace(gsSessionObj, this.gsClientTypes,  in); 
            UIBeanHelper.setInfoMessage("File uploaded to GS " + gsFileInfo.getFilename());
            this.setGenomeSpaceDirectories(null); 
        } 
        catch (Exception e) {
            UIBeanHelper.setErrorMessage(e.getLocalizedMessage());
        }
        return gsFileInfo;
    } 

}
