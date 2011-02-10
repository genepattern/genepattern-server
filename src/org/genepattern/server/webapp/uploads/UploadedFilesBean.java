/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2009) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.
 
 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.webapp.uploads;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
import org.genepattern.server.webapp.jsf.KeyValuePair;
import org.genepattern.server.webapp.jsf.UIBeanHelper;
import org.genepattern.server.webservice.server.dao.AdminDAO;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.util.SemanticUtil;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.WebServiceException;

/**
 * Backing bean for login to GenomeSpace.
 * 
 * @author liefeld
 * 
 */
public class UploadedFilesBean {
    private static Logger log = Logger.getLogger(UploadedFilesBean.class);

    private Map<String, Set<TaskInfo>> kindToModules;

    public UploadedFilesBean() {
        String userId = UIBeanHelper.getUserId();
        TaskInfo[] ti = new AdminDAO().getLatestTasks(userId);
        kindToModules = SemanticUtil.getKindToModulesMap(Arrays.asList(ti));
    }

    public void setMessageToUser(String messageToUser) {
        UIBeanHelper.setInfoMessage(messageToUser);
    }

    /**
     * Delete a file from the user's home dire on GenomeSpace
     * 
     * @param ae
     */
    public void deleteFile(ActionEvent ae) {
        String filenameParam = UIBeanHelper.getRequest().getParameter("filename");
        String dirnameParam = UIBeanHelper.getRequest().getParameter("path");

        log.debug("Delete from uploads '" + dirnameParam + "/" + filenameParam +"'");

        String tmpDir = System.getProperty("java.io.tmpdir");
        File tmp = new File(tmpDir);
        File subDir = new File(tmp, dirnameParam);
        File theFile = new File(subDir, filenameParam);
        
        boolean success = false;
        if (theFile.canRead()) {
            success = theFile.delete();
        }
        if (success) {
            // force a refresh
            this.availableDirectories = null;
            this.setMessageToUser("Deleted from upload directories: '" + dirnameParam + "/" + filenameParam + "'");
        }
        else {
            this.setMessageToUser("Error deleting from upload directories: '" + dirnameParam + "/" + filenameParam + "'");            
        }
    }

    /**
     * lots of room for optimization and caching here
     * 
     * @param dirname
     * @param file
     * @return
     */
    public UploadFileInfo getFile(String dirname, String file) {
        for (UploadDirectory dir : availableDirectories) {
            if ((dir.getName().equals(dirname)) || (dirname == null)) {
                for (UploadFileInfo aFile : dir.getUploadFiles()) {
                    if (aFile.getFilename().equals(file))
                        return aFile;
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
    public String getFileURL(String dirname, String filename) {
        if (filename == null)
            return null;

        HttpSession httpSession = UIBeanHelper.getSession();
        // http://127.0.0.1:8080/gp/getFile.jsp?job=0&file=ted_run9065171030618019207.tmp%2Fsmall_mmrc.res

        return UIBeanHelper.getServer() + "/gp/getFile.jsp?task=&file=" + dirname + "/" + filename;

    }

    public String getGenePatternFileURL(String dirname, String filename) {
        if (filename == null)
            return null;

        HttpSession httpSession = UIBeanHelper.getSession();
        // http://127.0.0.1:8080/gp/getFile.jsp?job=0&file=ted_run9065171030618019207.tmp%2Fsmall_mmrc.res

        return UIBeanHelper.getServer() + "/gp/getFile.jsp?task=&file=" + dirname + "/" + filename;

    }

    /**
     * redirects to a time limited, one time use link to the file on S3
     * 
     * @param ae
     */
    public void saveFileLocally(ActionEvent ae) {
        String filenameParam = UIBeanHelper.getRequest().getParameter("filename");
        String dirnameParam = UIBeanHelper.getRequest().getParameter("path");
        System.out.println("Save file locally " + dirnameParam + "/" + filenameParam);

        try {
            String url = getFileURL(dirnameParam, filenameParam);
            HttpServletResponse response = UIBeanHelper.getResponse();
            response.sendRedirect(url.toString());

        }
        catch (IOException ioe) {
            ioe.printStackTrace();
        }

        this.setMessageToUser("Saved from uploads " + dirnameParam + "/" + filenameParam);
    }

    /**
     * to help us detect when the file list should be updated. Not very
     * efficient at this point
     * 
     * @return
     */
    public int getCurrentUserFileCount() {
        List<File> jobFiles = getJobFiles();
        List<File> uploadFiles = getUserUploadFiles();
        return jobFiles.size() + uploadFiles.size();
    }
    
    /**
     * Helper method which lists all of the files in the temp dir for jobs,
     * it handles the corner-case where the tmpdir is not available.
     * 
     * @return
     */
    public List<File> getJobFiles() {
        List<File> rval = new ArrayList<File>();
        String tmpDir = System.getProperty("java.io.tmpdir");
        File tmp = new File(tmpDir);
        File[] fileList = tmp.listFiles(nameFilt);
        if (fileList == null) {
            log.error("Error listing files in java.io.tmpdir="+tmpDir);
        }
        if (fileList != null) {
            for(File f : fileList) {
                rval.add(f);
            }
        }
        return rval;
    }
    
    public List<File> getUserUploadFiles() {
        List<File> toReturn = new ArrayList<File>();
        String uploadDir = System.getProperty("user.uploads.dir", System.getProperty("java.io.tmpdir") + "/uploads");
        File tmp = new File(uploadDir);
        File[] fileList = tmp.listFiles();
        if (fileList == null) {
            log.error("Error listing files in uploads dir: "+uploadDir);
        }
        if (fileList != null) {
            for(File f : fileList) {
                toReturn.add(f);
            }
        }
        return toReturn;
    }

    private static final FilenameFilter nameFilt = new FilenameFilter() {
        public boolean accept(File dir, String name) {
            final String userId = UIBeanHelper.getUserId();
            return name.startsWith(userId + "_");
        }
    };

    public List<UploadDirectory> availableDirectories;
    private static final Comparator<KeyValuePair> COMPARATOR = new KeyValueComparator();
    private static final int DEFAULT_upload_maxfiles = 50;

    /**
     * get the list of directories in GenomeSpace this user can look at
     * 
     * @return
     */
    public List<UploadDirectory> getAvailableDirectories() {

        int fileCount = 0;
        if (availableDirectories != null) {
            for (UploadDirectory ud : availableDirectories) {
                fileCount += ud.getUploadFiles().size();
            }
        }

        if ((availableDirectories == null) || (fileCount != getCurrentUserFileCount())) {

            availableDirectories = new ArrayList<UploadDirectory>();
            final String userId = UIBeanHelper.getUserId();

            /**
             * The directory layer is unnecessary for now but will be needed
             * once we have subfolders and shared folders in GS
             */
            int dirCount = 0;
            List<DirectoryFileListPair> dirs =  new ArrayList<DirectoryFileListPair>();
            dirs.add(new DirectoryFileListPair(new UploadDirectory("Job Files"), getJobFiles()));
            dirs.add(new DirectoryFileListPair(new UploadDirectory("Uploaded Files"), getUserUploadFiles()));
            
            for (DirectoryFileListPair pair : dirs) {
                UploadDirectory userDir = pair.dir;
                availableDirectories.add(userDir);
      
                List<UploadFileInfo> fileList = new ArrayList<UploadFileInfo>();
                List<File> originalList = pair.fileList;
                for (File f : originalList) {
                    File[] fList = f.listFiles();
                    if (fList == null) {
                        log.error("Error listing files in subdir="+f.getAbsolutePath());
                    }
                    if (fList != null) {
                        for (File aFile : f.listFiles()) {
                            UploadFileInfo ufi = new UploadFileInfo(aFile.getName());
                            ufi.setUrl(getFileURL(f.getName(), aFile.getName()));
                            ufi.setPath(f.getName());
                            ufi.setGenePatternUrl(getGenePatternFileURL(f.getName(), aFile.getName()));
                            ufi.setModified(aFile.lastModified());
                            fileList.add(ufi);
                        }
                    }
                }
                
                // Sort all files by last modified date
                Collections.sort(fileList, new FileModifiedComparator());
                
                int count = 0;
                Map<String,Boolean> usedFileNames = new HashMap<String, Boolean>();
                
                Context userContext = Context.getContextForUser(userId);
                int maxFiles = ServerConfiguration.instance().getGPIntegerProperty(userContext, "upload.maxfiles", DEFAULT_upload_maxfiles);
    
                for (UploadFileInfo aFile : fileList) {
                    if (count >= maxFiles) {
                        break;
                    }
                    if (usedFileNames.get(aFile.getFilename()) != null) {
                        continue;
                    }
                    
                    userDir.getUploadFiles().add(aFile);
                    Collection<TaskInfo> modules;
                    List<KeyValuePair> moduleMenuItems = new ArrayList<KeyValuePair>();
                    modules = kindToModules.get(aFile.getKind());
                    if (modules != null) {
                        for (TaskInfo t : modules) {
                            KeyValuePair mi = new KeyValuePair(t.getShortName(), UIBeanHelper.encode(t.getLsid()));
                            moduleMenuItems.add(mi);
                        }
                        Collections.sort(moduleMenuItems, COMPARATOR);
                    }
                    aFile.setModuleMenuItems(moduleMenuItems);                
                    usedFileNames.put(aFile.getFilename(), true);
                    count++;
                }
                dirCount++;
            }
        }

        return availableDirectories;
    }

    public void setSelectedModule(String selectedModule) {

        List<UploadDirectory> dirs = getAvailableDirectories();

        if (selectedModule == null || dirs == null || dirs.size() == 0) {
            return;
        }
        Map<String, List<KeyValuePair>> kindToInputParameters = new HashMap<String, List<KeyValuePair>>();

        TaskInfo taskInfo = null;
        try {
            taskInfo = new LocalAdminClient(UIBeanHelper.getUserId())
                    .getTask(selectedModule);
        }
        catch (WebServiceException e) {
            log.error("Could not get module", e);
            return;
        }
        ParameterInfo[] inputParameters = taskInfo != null ? taskInfo
                .getParameterInfoArray() : null;
        List<KeyValuePair> unannotatedParameters = new ArrayList<KeyValuePair>();
        if (inputParameters != null) {
            for (ParameterInfo inputParameter : inputParameters) {
                if (inputParameter.isInputFile()) {
                    List<String> fileFormats = SemanticUtil
                            .getFileFormats(inputParameter);
                    String displayValue = (String) inputParameter
                            .getAttributes().get("altName");

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
                        List<KeyValuePair> inputParameterNames = kindToInputParameters
                                .get(format);
                        if (inputParameterNames == null) {
                            inputParameterNames = new ArrayList<KeyValuePair>();
                            kindToInputParameters.put(format,
                                    inputParameterNames);
                        }
                        inputParameterNames.add(kvp);
                    }
                }
            }
        }

        // add unannotated parameters to end of list for each kind
        if (unannotatedParameters.size() > 0) {
            for (Iterator<String> it = kindToInputParameters.keySet()
                    .iterator(); it.hasNext();) {
                List<KeyValuePair> inputParameterNames = kindToInputParameters
                        .get(it.next());
                inputParameterNames.addAll(unannotatedParameters);
            }
        }

        for (UploadDirectory aDir : dirs) {
            List<UploadFileInfo> outputFiles = aDir.getUploadFiles();

            if (outputFiles != null) {
                for (UploadFileInfo o : outputFiles) {
                    List<KeyValuePair> moduleInputParameters = kindToInputParameters
                            .get(o.getKind());
                    if (moduleInputParameters == null) {
                        moduleInputParameters = unannotatedParameters;
                    }
                    o.moduleInputParameters = moduleInputParameters;
                }
            }
        }

    }

    private static class KeyValueComparator implements Comparator<KeyValuePair> {

        public int compare(KeyValuePair o1, KeyValuePair o2) {
            return o1.getKey().compareToIgnoreCase(o2.getKey());
        }

    }
    
    private class FileModifiedComparator implements Comparator<UploadFileInfo> {

        public int compare(UploadFileInfo arg0, UploadFileInfo arg1) {
            if (arg0.getModified() < arg1.getModified()) {
                return -1;
            }
            else if (arg0.getModified() < arg1.getModified()) {
                return 1;
            }
            else {
                return 0;
            }
        }
    }
    
    private class DirectoryFileListPair {
        public UploadDirectory dir;
        public List<File> fileList;
        
        public DirectoryFileListPair(UploadDirectory dir, List<File> fileList) {
            this.dir =dir;
            this.fileList = fileList;
        }
    }

}
