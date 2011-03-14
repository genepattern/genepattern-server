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

    public final String RECENT_JOBS = "recentJobs";
    public final String UPLOADS = "uploads";

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
        
        Context context = Context.getContextForUser(UIBeanHelper.getUserId());
        File userUploadDir = ServerConfiguration.instance().getUserUploadDir(context);
        File subDir = new File(userUploadDir, dirnameParam);
        File theFile = new File(subDir, filenameParam);
        boolean success = false;
        if (theFile.canRead()) {
            success = theFile.delete();
        }
        if (success) {
            log.debug("Deleted from upload directories: '" + dirnameParam + "/" + filenameParam + "'");
            // force a refresh
            this.availableDirectories = null;
            this.setMessageToUser("Deleted from upload directories: '" + filenameParam + "'");
            
            // delete empty tmp parent folders
            try {
                if (subDir.canWrite() && subDir.isDirectory()) {
                    boolean subDirDeleted = false;
                    //special-case: don't delete dedicated FTP upload dir
                    boolean ignore = false;
                    if (subDir.getName().endsWith("_ftp")) {
                        ignore = true;
                    }
                    if (!ignore) {
                        String[] contents = subDir.list();
                        if (contents != null && contents.length == 0) {
                            subDirDeleted = subDir.delete();
                        }
                        if (subDirDeleted) {
                            log.debug("deleted empty tmp directory from upload directories: " + subDir.getPath());
                        }
                    }
                }
            }
            catch (Throwable t) {
                log.error("Unexpected exception deleting empty tmp directory from upload directories", t);
            }
        }
        else {
            log.error("Error deleting from upload directories: '" + dirnameParam + "/" + filenameParam + "'");
            this.setMessageToUser("Error deleting from upload directories: '" + filenameParam + "'");            
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
        return UIBeanHelper.getServer() + "/getFile.jsp?task=&file=" + dirname + "/" + filename;

    }

    public String getGenePatternFileURL(String dirname, String filename) {
        if (filename == null)
            return null;
        return UIBeanHelper.getServer() + "/getFile.jsp?task=&file=" + dirname + "/" + filename;

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
        Context context = Context.getContextForUser(UIBeanHelper.getUserId());
        String tmpDir = ServerConfiguration.instance().getGPProperty(context, "java.io.tmpdir");
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
        final String userId = UIBeanHelper.getUserId();
        File[] fileList = null;
        try {
            Context context = Context.getContextForUser(userId);
            File userUploadDir = ServerConfiguration.instance().getUserUploadDir(context);
            if (userUploadDir != null && userUploadDir.canRead()) {
                fileList = userUploadDir.listFiles(nameFilt);
            }
            if (fileList != null) {
                for(File f : fileList) {
                    toReturn.add(f);
                }
            }
        }
        catch (Throwable t) {
            log.error("Error listing uploaded files for user: "+userId, t);
        }
        return toReturn;
    }

    private static final FilenameFilter nameFilt = new FilenameFilter() {
        public boolean accept(File dir, String name) {
            final String userId = UIBeanHelper.getUserId();
            return name.startsWith(userId + "_");
        }
    };
    
    public String encodeFilename(String filename) {
        return filename.replaceAll(" ", "%20");
    }
    
    public boolean getBatchUploadEnabled() {
        String userId = UIBeanHelper.getUserId();
        Context userContext = Context.getContextForUser(userId);
        return ServerConfiguration.instance().getGPBooleanProperty(userContext, "allow.batch.process", false);
    }

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
            String userId = UIBeanHelper.getUserId();
            Context userContext = Context.getContextForUser(userId);
            boolean batchEnabled = ServerConfiguration.instance().getGPBooleanProperty(userContext, "allow.batch.process", false);

            /**
             * The directory layer is unnecessary for now but will be needed
             * once we have subfolders and shared folders in GS
             */
            int dirCount = 0;
            List<DirectoryFileListPair> dirs =  new ArrayList<DirectoryFileListPair>();
            dirs.add(new DirectoryFileListPair(new UploadDirectory("Job Input Files"), getJobFiles()));
            
            //include listing of uploaded files even if allow.batch.process is disabled,
            //    so that FTP transfers will show up on the Uploaded Files tab
            List<File> userUploadFiles = getUserUploadFiles();
            if (userUploadFiles != null && userUploadFiles.size() > 0) {
                UploadDirectory userUploadDir = new UploadDirectory("Uploaded Files");
                DirectoryFileListPair userUploadRecords = new DirectoryFileListPair(userUploadDir, userUploadFiles, true);
                dirs.add(userUploadRecords);
            }
            
            for (DirectoryFileListPair pair : dirs) {
                UploadDirectory userDir = pair.dir;
      
                List<UploadFileInfo> fileList = new ArrayList<UploadFileInfo>();
                List<File> originalList = pair.fileList;
                for (File f : originalList) {
                    File[] fList = f.listFiles();
                    if (fList == null) {
                        log.error("Error listing files in subdir="+f.getAbsolutePath());
                    }
                    if (fList != null) {
                        for (File aFile : f.listFiles()) {
                            if (f.getName().equalsIgnoreCase("parts")) {
                                // Ignore the partially-uploaded file directory
                                continue;
                            }
                            UploadFileInfo ufi = new UploadFileInfo(aFile.getName());
                            ufi.setUrl(getFileURL(f.getName(), encodeFilename(aFile.getName())));
                            ufi.setPath(encodeFilename(f.getName()));
                            ufi.setGenePatternUrl(getGenePatternFileURL(f.getName(), encodeFilename(aFile.getName())));
                            ufi.setModified(aFile.lastModified());
                            if (pair.directUploadList) {
                                ufi.setDirectUpload(true);
                            }
                            fileList.add(ufi);
                        }
                    }
                } 

                // Sort all files by last modified date
                Collections.sort(fileList, new FileModifiedComparator());
                
                int count = 0;
                Map<String,Boolean> usedFileNames = new HashMap<String, Boolean>();
                
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
                
                //don't include empty 'folders' in the list
                int num = userDir.getUploadFiles().size();
                if (num > 0) {
                    availableDirectories.add(userDir);
                }
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
        public boolean directUploadList = false;
        
        public DirectoryFileListPair(UploadDirectory dir, List<File> fileList) {
            this.dir =dir;
            this.fileList = fileList;
        }
        
        public DirectoryFileListPair(UploadDirectory dir, List<File> fileList, boolean direct) {
            this(dir, fileList);
            this.directUploadList = direct;
        }
    }
    
    public String getSelectedTab() {
        String attr = (String) UIBeanHelper.getSession().getAttribute("selectedTab");
        if (attr == null) {
            return RECENT_JOBS;
        }
        else {
            return attr;
        }
    }
    
    public void setSelectedTab(String selected) {
        UIBeanHelper.getSession().setAttribute("selectedTab", selected);
    }
    
    public int getPartitionLength() {
        Context context = Context.getContextForUser(UIBeanHelper.getUserId());
        return ServerConfiguration.instance().getGPIntegerProperty(context, "upload.partition.size", 5000000);
    }

}
