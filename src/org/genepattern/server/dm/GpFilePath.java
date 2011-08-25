package org.genepattern.server.dm;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import org.apache.log4j.Logger;

/**
 * Represents a path to a GenePattern datafile, with the ability to generate a representation for use in various contexts such as: 
 * URI for presentation in web client, File path on the server's file system, entry in database.
 * Several types of files should be supported:
 *     UserUpload, JobResult, JobInput, TasklibInput, ServerFile
 * @author pcarr
 */
abstract public class GpFilePath implements Comparable<GpFilePath> {
    private static Logger log = Logger.getLogger(GpFilePath.class);
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
    
    public int compareTo(GpFilePath o) {
        return getRelativeUri().getPath().compareTo( o.getRelativeUri().getPath() );
    }
    
    public int hashCode() {
        return getRelativeUri().getPath().hashCode();
    }
    
    public boolean equals(Object o) {
        if (!(o instanceof GpFilePath)) {
            return false;
        }
        GpFilePath gpFilePath = (GpFilePath) o;
        return getRelativeUri().getPath().equals( gpFilePath.getRelativeUri().getPath() );
    }

    
    public static void init() {
        Comparable<GpFilePath> c = new Comparable<GpFilePath>() {

            @Override
            public int compareTo(GpFilePath o) {
                // TODO Auto-generated method stub
                return 0;
            }
        };
    }
    
    /**
     * Get the fully qualified URL to this file.
     * @return
     * @throws Exception
     */
    public URL getUrl() throws Exception {
        URL gpUrl = getGenePatternUrl();
        URI uri = getRelativeUri();
        
        URI gpUri = gpUrl.toURI();
        String newPath = gpUri.getPath() + uri.getPath();
        File file = new File(newPath);
        newPath = UrlUtil.encodeFilePath(file);
        
        if (isDirectory()) {
            newPath = newPath + "/";
        }
        URI full = gpUri.resolve( newPath );
        return full.toURL();
    }

    /**
     * Same as {@link java.io.File#isFile()}.
     * @return
     */
    public boolean isFile() {
        return getRelativeFile().isFile();
    }

    /**
     * Same as {@link java.io.File#isDirectory()}.
     */
    public boolean isDirectory() {
        return getRelativeFile().isDirectory();
    }
    
    /**
     * Get the relative path, converting, if necessary, all path separators to the forward slash ('/').
     * @return the relative path
     */
    public String getRelativePath() {
        File file = getRelativeFile();
        if (file == null) {
            return "";
        }
        String path = file.getPath();
        String r = path.replace( File.separator, "/");
        if (file.isDirectory() && file.getName() != null && file.getName().length() > 0) {
            r = r + "/";
        }
        return r;
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
     * Get the relative path to the File, for example, a user upload file's relative path is relative to the user's upload directory,
     * a job result file's relative path is relative to the result dir for the job.
     * @return
     */
    abstract public File getRelativeFile();
    
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
