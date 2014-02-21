package org.genepattern.server.dm.tasklib;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.eula.InitException;
import org.genepattern.server.eula.LibdirLegacy;
import org.genepattern.server.eula.LibdirStrategy;
import org.genepattern.webservice.TaskInfo;

/**
 * A path to module or pipeline file stored in the tasklib.
 * 
 * @author pcarr
 */
public class TasklibPath extends GpFilePath {
    private static Logger log = Logger.getLogger(TasklibPath.class);

    private final File serverFile;
    private final File relativeFile;
    private final URI relativeUri;
    private final boolean isPublic;
    private final String taskOwner;
    
    public TasklibPath(final TaskInfo taskInfo, final String relativePath) {
        this(null, taskInfo, relativePath);
    }
    public TasklibPath(LibdirStrategy libdirStrategy, final TaskInfo taskInfo, final String relativePath) {
        if (taskInfo==null) {
            throw new IllegalArgumentException("taskInfo==null");
        }
        if (libdirStrategy == null) {
            libdirStrategy=new LibdirLegacy();
        }
        File taskLibDir=null;
        try {
            taskLibDir=libdirStrategy.getLibdir(taskInfo.getLsid());
        }
        catch (InitException e) {
            taskLibDir=null;
            log.error(e);
            throw new IllegalArgumentException("Error initializing libdir for task="+taskInfo.getLsid());
        }
        
        super.setOwner(taskInfo.getUserId());
        this.relativeFile = new File(relativePath); 
        this.serverFile = new File(taskLibDir, relativePath);
        
        this.isPublic = taskInfo.getAccessId() == 1;
        this.taskOwner = taskInfo.getUserId();
        this.relativeUri=initRelativeUri(taskInfo, relativePath);
    }
    
    private final URI initRelativeUri(final TaskInfo taskInfo, final String relativePath) {
        final String lsid=taskInfo.getLsid();
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
            return new URI(uriStr);
        }
        catch (URISyntaxException e) {
            log.error("Error constructiung uri from "+uriStr, e);
        }
        return null;
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
    public boolean canRead(final boolean isAdmin, final GpContext userContext) {
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
