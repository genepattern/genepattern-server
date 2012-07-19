package org.genepattern.server.dm.tasklib;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;

import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.webservice.server.DirectoryManager;
import org.genepattern.webservice.TaskInfo;

/**
 * A path to module or pipeline file stored in the tasklib.
 * 
 * @author pcarr
 */
public class TasklibPath extends GpFilePath {
    private static Logger log = Logger.getLogger(TasklibPath.class);

    private String lsid;
    private String relativePath;
    private File serverFile;
    private File relativeFile;
    private URI relativeUri;
    private boolean isPublic = false;
    private String taskOwner = "";
    
    public TasklibPath(TaskInfo taskInfo, String relativePath) {
        this.lsid = taskInfo.getLsid();
        this.relativePath = relativePath;
        this.relativeFile = new File(relativePath); 
        super.setOwner(taskInfo.getUserId());
        File taskLibDir = new File(DirectoryManager.getTaskLibDir(taskInfo)); 
        this.serverFile = new File(taskLibDir, relativePath);
        
        String encodedFilepath = relativePath;
        try {
            encodedFilepath = URLEncoder.encode(relativePath, "UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            log.error("Error encoding "+relativePath, e);
            encodedFilepath = URLEncoder.encode(relativePath);
        }
        String uriStr = "/getFile.jsp?task="+lsid+"&file="+encodedFilepath;
        try {
            relativeUri = new URI(uriStr);
        }
        catch (URISyntaxException e) {
            log.error("Error constructiung uri from "+uriStr, e);
        }
        
        this.isPublic = taskInfo != null && taskInfo.getAccessId() == 1;
        this.taskOwner = taskInfo.getUserId();
    }

    /**
     * Return a URI relative to the gp server,
     * /getFile.jsp?task=urn:lsid:8080.gp-trunk-dev.120.0.0.1:genepatternmodules:85:2&file=all_aml_test.cls
     */
    public URI getRelativeUri() {
        return relativeUri;
    }

    /**
     * Return a path to the file.
     */
    public File getServerFile() {
        return serverFile;
    }

    /**
     * Return a path to the file, relative to the installation directory of the module or pipeline.
     */
    public File getRelativeFile() {
        return relativeFile;
    }

    /**
     * Admin users can read all files, all users can read all files in public modules, otherwise 
     * only the owner of the module can read the file.
     */
    public boolean canRead(boolean isAdmin, Context userContext) {
        if (isAdmin) {
            return true;
        }
        
        if (isPublic) {
            return true;
        }
        
        if (userContext == null || userContext.getUserId() == null) {
            return false;
        }
        return userContext.getUserId().equals( taskOwner );
    }

    public String getFormFieldValue() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getParamInfoValue() {
        // TODO Auto-generated method stub
        return null;
    }

}
