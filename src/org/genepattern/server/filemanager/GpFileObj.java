package org.genepattern.server.filemanager;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import org.apache.log4j.Logger;
import org.genepattern.server.dm.UrlUtil;

/**
 * Reference to a GenePattern datafile. Several types of files should be supported:
 *     UserUpload, JobResult, JobInput, TasklibInput, 
 * @author pcarr
 */
abstract public class GpFileObj {
    private static Logger log = Logger.getLogger(GpFileObj.class);
    private static URL gpUrl = null;
    /**
     * Get the GenePatternURL, e.g.
     * <pre>
       http://127.0.0.1:8080/gp
     * </pre>
     * This should never include the trailing slash.
     * @return
     * 
     * @see org.genepattern.server.webapp.StartupServlet#setServerURLs, which initializes the GenePatternURL.
     */
    static public URL getGenePatternUrl() {
        final String defaultGpUrlProp = "http://127.0.0.1:8080/gp";
        if (gpUrl == null) {
            log.info("Initializing GenePatternURL ...");
            String gpUrlProp = System.getProperty("GenePatternURL", defaultGpUrlProp);
            if (gpUrlProp.endsWith("/")) {
                gpUrlProp = gpUrlProp.substring(0, gpUrlProp.length() - 1);
            }
            try {
                gpUrl = new URL(gpUrlProp);
            }
            catch (MalformedURLException e) {
                log.error("Invalid System.property, GenePatternURL="+gpUrlProp);
                try {
                    gpUrl = new URL(defaultGpUrlProp);
                }
                catch (MalformedURLException e2) {
                    //ignore
                }
            }
            log.info("GenePatternURL="+gpUrl.toExternalForm());
        }
        return gpUrl;
    }
    
    public URL getUrl() throws Exception {
        URL gpUrl = getGenePatternUrl();
        URI uri = getRelativeUri();
        
        URI gpUri = gpUrl.toURI();
        String newPath = gpUri.getPath() + uri.getPath();
        File file = new File(newPath);
        newPath = UrlUtil.encodeFilePath(file);
        URI full = gpUri.resolve( newPath );
        return full.toURL();
    }
    
    /**
     * Get the relative URI to this file, the path is specified relative to the GenePatternURL.
     * @return
     */
    abstract public URI getRelativeUri();
    
    /**
     * Get the File object for use in the GP server runtime. Relative paths are relative to <gp.home>, if <gp.home> is not defined, then 
     * a relative path is relative to the working directory of the gp server.
     * @return
     */
    abstract public File getServerFile();
    
    /**
     * Get the string literal to use as an input form value in a job submit form, when this file is to be specified as an input
     * to a module.
     * @return
     */
    abstract String getFormFieldValue();
    
    /**
     * Get the string literal to use when serializing a job result into the ANALYSIS_JOB.PARAMETER_INFO CLOB.
     * @return
     */
    abstract String getParamInfoValue();
    
    /**
     * Get the string literal to use when serializing a module into the TASK_MASTER table.
     * @return
     */
    abstract String getTasklibValue();
}
