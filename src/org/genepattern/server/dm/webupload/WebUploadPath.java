package org.genepattern.server.dm.webupload;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.dm.GpFilePath;

/**
 * Represent web upload files, files which were uploaded to the server from an HTTP client job submit form.
 * @author pcarr
 *
 */
public class WebUploadPath extends GpFilePath {
    private static Logger log = Logger.getLogger(WebUploadPath.class);

    private String jobId;
    private String filepath;
    private File relativeFile;
    private File serverFile;
    private URI relativeUri;
    
    public WebUploadPath(String jobId, String userid, String filepath) { 
        this.jobId = jobId;
        super.setOwner(userid);
        this.filepath = filepath;
        String str = new GpConfig.Builder().build().getTempDir(null).getAbsolutePath();
        File rootWebUploadDir = new File(str);
        
        this.relativeFile = new File(filepath);
        this.serverFile = new File(rootWebUploadDir, filepath);
        
        String encodedFilepath = filepath;
        try {
            encodedFilepath = URLEncoder.encode(filepath, "UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            log.error("Error encoding "+filepath, e);
            encodedFilepath = URLEncoder.encode(filepath);
        }
        String uriStr = "/getFile.jsp?task=&job="+jobId+"&file="+ encodedFilepath;
        try {
            relativeUri = new URI(uriStr);
        }
        catch (URISyntaxException e) {
            log.error("Error constructiung uri from "+uriStr, e);
        }
    }

    /**
     * Get the link to this file, relative to the GenePatternURL, e.g.
     *     /getFile.jsp?task=&job=1222&file=test_run89....546.tmp/all_aml_test.gct
     *     /getFile.jsp?task=&job=<job_no>&file=<userid>_run<random_number>.tmp/<filename>
     */
    public URI getRelativeUri() {
        return relativeUri;
    }

    public File getServerFile() {
        return serverFile;
    }

    /**
     * @return the path relative to the system temp directory to which all user uploads are added
     * 
     */
    public File getRelativeFile() {
        return relativeFile;
    }

    public boolean canRead(final boolean isAdmin, final GpContext userContext) {
        if (isAdmin) {
            return true;
        }
        //TODO: need to implement permissions for web uploaded files in shared jobs
        return userContext != null && userContext.getUserId() != null && userContext.getUserId().equals(owner);
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
