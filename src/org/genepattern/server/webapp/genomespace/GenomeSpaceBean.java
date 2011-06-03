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
import java.util.Iterator;
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
import org.genepattern.server.webapp.jsf.KeyValuePair;
import org.genepattern.server.webapp.jsf.UIBeanHelper;
import org.genepattern.server.webservice.server.dao.AdminDAO;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.util.SemanticUtil;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.WebServiceException;
import org.genomespace.client.ConfigurationUrls;
import org.genomespace.client.DataManagerClient;
import org.genomespace.client.GsSession;
import org.genomespace.client.User;
import org.genomespace.client.exceptions.AuthorizationException;
import org.genomespace.client.exceptions.InternalServerException;
import org.genomespace.datamanager.core.GSDirectoryListing;
import org.genomespace.datamanager.core.GSFileMetadata;

/**
 * Backing bean for login to GenomeSpace.
 * 
 * @author liefeld
 * 
 */
public class GenomeSpaceBean {
    private static Logger log = Logger.getLogger(GenomeSpaceBean.class);

    public static String GS_SESSION_KEY = "GS_SESSION";
    public static String GS_USER_KEY = "GS_USER";
    
    
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

    public boolean isGenomeSpaceEnabled(){
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
    
    public boolean isLoggedIn(){
        HttpSession httpSession = UIBeanHelper.getSession();
        GsSession gsSession = (GsSession)httpSession.getAttribute(GS_SESSION_KEY);
        return ((gsSession != null) && (gsSession.isLoggedIn()));
     }
    

    /**
     * Submit the user / password. For now this uses an action listener since we are redirecting to a page outside of
     * the JSF framework. This should be changed to an action to use jsf navigation in the future.
     * 
     * @param event --
     *                ignored
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
           this.setMessageToUser("Signed in to GenomeSpace as " + gsUser.getUsername()  );
            
           return "home";
            
        }  catch (AuthorizationException e) {
            log.info("Problem logging into GenomeSpace");
            unknownUser = true;
            this.setMessageToUser("Authentication error, please check your username and password." );
            return "genomeSpaceLoginFailed";
             
             
        } catch (Exception e) {
            log.error("Exception logging into GenomeSpace>: " + e.getMessage());
            unknownUser = true;
            this.setMessageToUser("An error occurred logging in to GenomeSpace.  Please contact the GenePattern administrator." );
            
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
        
        GsSession gsSession = (GsSession)httpSession.getAttribute("GS_SESSION");
            
        gsSession.logout();
        httpSession.setAttribute(GS_USER_KEY,null);
        httpSession.setAttribute(GS_SESSION_KEY,null);
            
        this.setMessageToUser( " Logged out of GenomeSpace." );
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
      
      GsSession sess = (GsSession)httpSession.getAttribute(GS_SESSION_KEY);
      sess.getDataManagerClient().delete(theFile.gsFile);
      this.setGenomeSpaceDirectories(null); // force a refresh
      this.setMessageToUser( "Deleted from GS " + dirnameParam + "/" + filenameParam );
        
    }
    
    /**
     * lots of room for optimization and caching here
     * @param dirname
     * @param file
     * @return
     */
    public GenomeSpaceFileInfo getFile(String dirname, String file){
       for (GenomeSpaceDirectory dir: this.getGenomeSpaceDirectories()){
            if ((dir.getName().equals(dirname)) || (dirname == null)){
                
                for (GenomeSpaceFileInfo aFile: dir.getGsFiles()){
                    if (aFile.getFilename().equals(file)) return aFile;
                }
             }
            
            for (GenomeSpaceDirectory aDir: dir.getGsDirectories()){
                if ((aDir.getName().equals(dirname)) || (dirname == null)){
                    
                    for (GenomeSpaceFileInfo aFile: aDir.getGsFiles()){
                        if (aFile.getFilename().equals(file)) return aFile;
                    }
                 }
            }
        }
        return null;
    }
    
    
    /**
     * gets a one time use link to the file on S3
     * 
     * @param ae
     */
    public String getFileURL(String dirname, String filename){
        if (filename == null) return null;
        GenomeSpaceFileInfo theFile = getFile(dirname, filename);
        return getFileURL(theFile.gsFile);
    }
    
    public String getFileURL(GSFileMetadata gsFile){
        if (gsFile == null) return null;
        HttpSession httpSession = UIBeanHelper.getSession();
        GsSession sess = (GsSession) httpSession.getAttribute(GS_SESSION_KEY);
        
        URL s3Url = sess.getDataManagerClient().getFileUrl(gsFile, null);
        return s3Url.toString();
    }
    
    /**
     * redirects to a time limited, one time use link to the file on S3
     * 
     * @param ae
     */
    public void saveFileLocally(ActionEvent ae){
        String filenameParam = UIBeanHelper.getRequest().getParameter("filename");
        String dirnameParam = UIBeanHelper.getRequest().getParameter("dirname");
        
        try {
            String s3Url = getFileURL(dirnameParam, filenameParam);
            HttpServletResponse response = UIBeanHelper.getResponse();
            response.sendRedirect(s3Url.toString());
         
        } catch (IOException ioe){
            ioe.printStackTrace();
        }
    }
    
    /**
     * Save a local GenePattern result file back to the GenomeSpace data repository
     * @return
     */
    public String sendInputFileToGenomeSpace(){
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
    public String sendToGenomeSpace(){
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
    private void saveFileToGenomeSpace(HttpSession httpSession, File in) {
       try {
            GsSession sess = (GsSession)httpSession.getAttribute(GS_SESSION_KEY);
            DataManagerClient dmClient = sess.getDataManagerClient();
            GSDirectoryListing rootDir = dmClient.listDefaultDirectory();
            dmClient.uploadFile(in, rootDir.getDirectory());
        
            UIBeanHelper.setInfoMessage("File uploaded to GS " + in.getName() );
            this.setGenomeSpaceDirectories(null);
            
        } catch (Exception e){
            e.printStackTrace();
            this.setMessageToUser( "There was a problem uploadeing the file to GS, " + in.getName() );
        }
    }
    
    /**
     * get the list of directories in GenomeSpace this user can look at
     * @return
     */
    public List<GenomeSpaceDirectory> getAvailableDirectories(){
        List<GenomeSpaceDirectory> availableDirectories = this.getGenomeSpaceDirectories();
        
        if ((availableDirectories == null) || (availableDirectories.size() == 0)) {
            availableDirectories = new ArrayList<GenomeSpaceDirectory>();
            
           
            HttpSession httpSession = UIBeanHelper.getSession();
            GsSession gsSession = (GsSession)httpSession.getAttribute(GS_SESSION_KEY);
            User gsUser = (User)httpSession.getAttribute(GS_USER_KEY);
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
        return (List<GenomeSpaceDirectory>) UIBeanHelper.getSession().getAttribute("GS_DIRECTORIES");
    }
    
    public void setGenomeSpaceDirectories(List<GenomeSpaceDirectory> dirs) {
        UIBeanHelper.getSession().setAttribute("GS_DIRECTORIES", dirs);
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
//        List<GenomeSpaceDirectory> dirs =  getAvailableDirectories();
//        
//        
//        if (selectedModule == null || dirs == null || dirs.size() == 0) {
//            return;
//        }
//        Map<String, List<KeyValuePair>> kindToInputParameters = new HashMap<String, List<KeyValuePair>>();
//
//        TaskInfo taskInfo = null;
//        try {
//            taskInfo = new LocalAdminClient(UIBeanHelper.getUserId()).getTask(selectedModule);
//        } catch (WebServiceException e) {
//            log.error("Could not get module", e);
//            return;
//        }
//        ParameterInfo[] inputParameters = taskInfo != null ? taskInfo.getParameterInfoArray() : null;
//        List<KeyValuePair> unannotatedParameters = new ArrayList<KeyValuePair>();
//        if (inputParameters != null) {
//            for (ParameterInfo inputParameter : inputParameters) {
//            if (inputParameter.isInputFile()) {
//                List<String> fileFormats = SemanticUtil.getFileFormats(inputParameter);
//                String displayValue = (String) inputParameter.getAttributes().get("altName");
//
//                if (displayValue == null) {
//                displayValue = inputParameter.getName();
//                }
//                displayValue = displayValue.replaceAll("\\.", " ");
//
//                KeyValuePair kvp = new KeyValuePair();
//                kvp.setKey(inputParameter.getName());
//                kvp.setValue(displayValue);
//
//                if (fileFormats.size() == 0) {
//                unannotatedParameters.add(kvp);
//                }
//                for (String format : fileFormats) {
//                List<KeyValuePair> inputParameterNames = kindToInputParameters.get(format);
//                if (inputParameterNames == null) {
//                    inputParameterNames = new ArrayList<KeyValuePair>();
//                    kindToInputParameters.put(format, inputParameterNames);
//                }
//                inputParameterNames.add(kvp);
//                }
//            }
//            }
//        }
//
//        // add unannotated parameters to end of list for each kind
//        if (unannotatedParameters.size() > 0) {
//            for (Iterator<String> it = kindToInputParameters.keySet().iterator(); it.hasNext();) {
//            List<KeyValuePair> inputParameterNames = kindToInputParameters.get(it.next());
//            inputParameterNames.addAll(unannotatedParameters);
//            }
//        }
//            
////        for (GenomeSpaceDirectory aDir : dirs) {
////            List<GenomeSpaceFileInfo> outputFiles = aDir.getGsFiles();
////            if (outputFiles != null) {
////            for (GenomeSpaceFileInfo o : outputFiles) {
////                List<KeyValuePair> moduleInputParameters = kindToInputParameters.get(o.getKind());
////
////                if (moduleInputParameters == null) {
////                    moduleInputParameters = unannotatedParameters;
////                }
////                o.moduleInputParameters = moduleInputParameters;
////            }
////            }
////        }
//
        }
    
    
    
}
