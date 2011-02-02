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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.faces.event.ActionEvent;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.genepattern.server.UserAccountManager;
import org.genepattern.server.auth.AuthenticationException;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.executor.CommandProperties;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.webapp.LoginManager;
import org.genepattern.server.webapp.jsf.JobBean;
import org.genepattern.server.webapp.jsf.JobResultsWrapper;
import org.genepattern.server.webapp.jsf.KeyValuePair;
import org.genepattern.server.webapp.jsf.UIBeanHelper;
import org.genepattern.server.webapp.jsf.JobBean.OutputFileInfo;
import org.genepattern.server.webservice.server.dao.AdminDAO;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.util.SemanticUtil;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.WebServiceException;

import org.genomespace.client.exceptions.AuthorizationException;
import org.genomespace.datamanager.core.*;
import org.genomespace.client.DataManagerClient;
import org.genomespace.client.User;
import org.genomespace.client.GsSession;
import org.genomespace.client.exceptions.InternalServerException;
import org.genomespace.client.User;

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
    
    private boolean genomeSpaceEnabled = true;

    private Map<String, Collection<TaskInfo>> kindToModules;
    
    
    public GenomeSpaceBean() {
        String userId = UIBeanHelper.getUserId();
 //XXX       kindToModules = SemanticUtil.getKindToModulesMap(new AdminDAO().getLatestTasks(userId));
    
        Context userContext = Context.getContextForUser(userId);
        CommandProperties props = ServerConfiguration.Factory.instance().getGPProperties(userContext);
        boolean genomeSpaceEnabled = props.getBooleanProperty("genomeSpaceEnabled");

        if (!genomeSpaceEnabled) genomeSpaceEnabled = props.getBooleanProperty("gsEnabled");  
        System.out.println("\n\n======= GenomeSpace enabled = " + genomeSpaceEnabled + "\n\n");
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
        
        
        if (username == null) {
            unknownUser = true;
            return "home";
        }

       try {
           GsSession gsSession = new GsSession();
            User gsUser = gsSession.login(username, password);
            HttpSession httpSession = UIBeanHelper.getSession();
            httpSession.setAttribute(GS_USER_KEY,gsUser);
            httpSession.setAttribute(GS_SESSION_KEY,gsSession);
            
            this.setMessageToUser("Signed in to GenomeSpace as " + gsUser.getUsername()  );
            
            
        }  catch (AuthorizationException e) {
            e.printStackTrace();
             log.error(e);
       //     throw new RuntimeException(e); // @TODO -- wrap in gp system exception.
        
             // TODO figure out what went wrong and send error message to the user
             
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e);
            
        }
        return "home";
    }
    
    /**
     * register a user into GenomeSpace
     * @return
     */
  public String submitRegistration() {
        
        if (username == null) {
            invalidRegistration = true;
            return "home";
        }
        if (! regPassword.equals(password)) {
            invalidRegistration = true;
            return "home";
        }

       try {
           GsSession gsSession = new GsSession();
           User gsUser = gsSession.registerUser(username, password, regEmail);
           
            HttpSession httpSession = UIBeanHelper.getSession();
            httpSession.setAttribute(GS_USER_KEY,gsUser);
            httpSession.setAttribute(GS_SESSION_KEY,gsSession);
            
            this.setMessageToUser("Signed in to GenomeSpace as " + gsUser.getUsername()  );
            
            
        }   catch (Exception e) {
            e.printStackTrace();
            log.error(e);
            
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
        this.availableDirectories = null;
            
       
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
      this.availableDirectories = null; // force a refresh
      this.setMessageToUser( "Deleted from GS " + dirnameParam + "/" + filenameParam );
        
    }
    
    /**
     * lots of room for optimization and caching here
     * @param dirname
     * @param file
     * @return
     */
    public GenomeSpaceFileInfo getFile(String dirname, String file){
       for (GenomeSpaceDirectory dir: availableDirectories){
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
        HttpSession httpSession = UIBeanHelper.getSession();
      try {
            GsSession sess = (GsSession)httpSession.getAttribute(GS_SESSION_KEY);
        
            // URL s3Url = sess.getFileUrl(theFile);
            //return s3Url.toString();
           
     System.out.println("GS GET FILE URL NOT IMPLEMENTED ==================");
            if (!false) throw new InternalServerException("placeholder");
            return "http://www.yahoo.com";
        } catch (InternalServerException e){
            e.printStackTrace();
        }
        return null;
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
        String filenameParam = UIBeanHelper.getRequest().getParameter("filename");
        String dirnameParam = UIBeanHelper.getRequest().getParameter("dirname");
         
        String tmpDir = System.getProperty("java.io.tmpdir");
        
        File tmp = new File(tmpDir);
        File subDir = new File(tmp, dirnameParam);
        File theFile = new File(subDir, filenameParam);
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
        
            this.setMessageToUser( "File uploaded to GS " + in.getName() );
            this.availableDirectories = null;
            
        } catch (Exception e){
            e.printStackTrace();
            this.setMessageToUser( "There was a problem uploadeing the file to GS, " + in.getName() );
        }
    }
    
    
    
    public List<GenomeSpaceDirectory> availableDirectories;
    
    /**
     * get the list of directories in GenomeSpace this user can look at
     * @return
     */
    public List<GenomeSpaceDirectory> getAvailableDirectories(){
        
        if ((availableDirectories == null) || (availableDirectories.size() == 0)) {
            availableDirectories = new ArrayList<GenomeSpaceDirectory>();
            
            
          HttpSession httpSession = UIBeanHelper.getSession();
          GsSession gsSession = (GsSession)httpSession.getAttribute(GS_SESSION_KEY);
          User gsUser = (User)httpSession.getAttribute(GS_USER_KEY);
          if ((gsSession == null) || (! gsSession.isLoggedIn())) return availableDirectories;
            
          DataManagerClient dmClient = gsSession.getDataManagerClient();
          GSDirectoryListing rootDir = dmClient.listDefaultDirectory();
    
        
        
         
         GenomeSpaceDirectory userDir = new GenomeSpaceDirectory(rootDir, dmClient);
         
         availableDirectories.add(userDir);
//            
//            if (gsSession.isLoggedIn()){
//                GSDirectoryListing rootGSDir;
//                try {
//                    rootGSDir = dmClient.listDefaultDirectory();
//                    userDir.setGsFileList(rootGSDir, this.kindToModules, this);
//                    
//                    
//                } catch (Exception e1) {
//                    // TODO Auto-generated catch block
//                    e1.printStackTrace();
//                }
//                
//            }
        }
        return availableDirectories;
    }

  
    
    public void setSelectedModule(String selectedModule) {
        
        List<GenomeSpaceDirectory> dirs =  getAvailableDirectories();
        
        
        if (selectedModule == null || dirs == null || dirs.size() == 0) {
            return;
        }
        Map<String, List<KeyValuePair>> kindToInputParameters = new HashMap<String, List<KeyValuePair>>();

        TaskInfo taskInfo = null;
        try {
            taskInfo = new LocalAdminClient(UIBeanHelper.getUserId()).getTask(selectedModule);
        } catch (WebServiceException e) {
            log.error("Could not get module", e);
            return;
        }
        ParameterInfo[] inputParameters = taskInfo != null ? taskInfo.getParameterInfoArray() : null;
        List<KeyValuePair> unannotatedParameters = new ArrayList<KeyValuePair>();
        if (inputParameters != null) {
            for (ParameterInfo inputParameter : inputParameters) {
            if (inputParameter.isInputFile()) {
                List<String> fileFormats = SemanticUtil.getFileFormats(inputParameter);
                String displayValue = (String) inputParameter.getAttributes().get("altName");

                if (displayValue == null) {
                displayValue = inputParameter.getName();
                }
                displayValue = displayValue.replaceAll("\\.", " ");

                KeyValuePair kvp = new KeyValuePair();
                kvp.setKey(inputParameter.getName());
                kvp.setValue(displayValue);

                if (fileFormats.size() == 0) {
                unannotatedParameters.add(kvp);
                }
                for (String format : fileFormats) {
                List<KeyValuePair> inputParameterNames = kindToInputParameters.get(format);
                if (inputParameterNames == null) {
                    inputParameterNames = new ArrayList<KeyValuePair>();
                    kindToInputParameters.put(format, inputParameterNames);
                }
                inputParameterNames.add(kvp);
                }
            }
            }
        }

        // add unannotated parameters to end of list for each kind
        if (unannotatedParameters.size() > 0) {
            for (Iterator<String> it = kindToInputParameters.keySet().iterator(); it.hasNext();) {
            List<KeyValuePair> inputParameterNames = kindToInputParameters.get(it.next());
            inputParameterNames.addAll(unannotatedParameters);
            }
        }
            
//        for (GenomeSpaceDirectory aDir : dirs) {
//            List<GenomeSpaceFileInfo> outputFiles = aDir.getGsFiles();
//            if (outputFiles != null) {
//            for (GenomeSpaceFileInfo o : outputFiles) {
//                List<KeyValuePair> moduleInputParameters = kindToInputParameters.get(o.getKind());
//
//                if (moduleInputParameters == null) {
//                    moduleInputParameters = unannotatedParameters;
//                }
//                o.moduleInputParameters = moduleInputParameters;
//            }
//            }
//        }

        }
    
    
    
}
