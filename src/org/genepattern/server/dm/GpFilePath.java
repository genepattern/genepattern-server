package org.genepattern.server.dm;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpContext;
import org.genepattern.util.SemanticUtil;

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
     * Get the GenePatternURL. For example,
     * <pre>
       http://127.0.0.1:8080/gp
     * </pre>
     * This should never include the trailing slash.
     * 
     * TODO: refactor into a Utility class.
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
    
    /**
     * replace all separators with the forward slash ('/').
     * @param file
     * @return
     */
    static public String getPathForwardSlashed(File file) {
        String path = file.getPath();
        String r = path.replace( File.separator, "/");
        if (file.isDirectory() && file.getName() != null && file.getName().length() > 0) {
            r = r + "/";
        }
        return r;
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
    
    protected String owner = "";
    /**
     * Get the GP userid for the owner of the file.
     * this can be set to the empty string if there is no de facto owner for the file,
     * for example server files are not owned by a particular user.
     * @return
     */
    public String getOwner() {
        return owner;
    }
    
    public void setOwner(String owner) {
        this.owner = owner;
    }

    /**
     * Get the fully qualified URL to this file.
     * @return
     * @throws Exception
     */
    public URL getUrl() throws Exception {
        String str="";
        final URL gpUrl = getGenePatternUrl();
        if (gpUrl==null) {
            log.error("GenePatternURL is null");
        }
        else {
            //expected
            str += gpUrl.toString();
        }
        
        final URI relativeUri = getRelativeUri();
        if (relativeUri==null) {
            //null relativeUri means use the GP URL
            log.debug("known error: uri is null");
        }
        else {
            str += relativeUri.toString();
        }
        if (isDirectory() && !str.endsWith("/")) {
            str = str + "/";
        }
        URL url = new URL(str);
        return url;
    }

    /**
     * Same as {@link java.io.File#isFile()}.
     * @return
     */
    public boolean isFile() {
        final File serverFile=getServerFile();
        if (serverFile==null) {
            log.debug("server file is null");
            return false;
        }
        return serverFile.isFile();
    }

    /**
     * Same as {@link java.io.File#isDirectory()}.
     */
    public boolean isDirectory() {
        final File serverFile=getServerFile();
        if (serverFile==null) {
            log.debug("server file is null");
            return false;
        }
        return serverFile.isDirectory();
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
        return getPathForwardSlashed(file);
    }
    
    public void initMetadata() {
        File file = getServerFile();
        if (file != null) {
            this.name = file.getName();
            this.extension = SemanticUtil.getExtension(file);
            if (this.extension != null && this.extension.length()>0) {
                this.kind=this.extension;
            }
        }
        if (file != null && file.exists()) {
            this.lastModified = new Date(file.lastModified());
            this.fileLength = file.length();
            if (file.isDirectory()) {
                this.kind = "directory";
            }
            else {
                this.kind = SemanticUtil.getKind(file);
            }
        }
    }

    //cached file metadata
    private String name;
    private Date lastModified;
    private long fileLength;
    private String extension;
    private String kind;

    //required for partial uploads
    private int numParts = 1;    
    private int numPartsRecd = 0;
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public long getFileLength() {
        return fileLength;
    }

    public void setFileLength(long fileLength) {
        this.fileLength = fileLength;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }
    public int getNumParts() {
        return numParts;
    }
    public void setNumParts(int numParts) {
        this.numParts = numParts;
    }
    public int getNumPartsRecd() {
        return numPartsRecd;
    }
    public void setNumPartsRecd(int numPartsRecd) {
        this.numPartsRecd = numPartsRecd;
    }

    //support for directory listings
    private List<GpFilePath> children;
    public List<GpFilePath> getChildren() {
        if (children == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(children);
    }
    
    public void addChild(GpFilePath child) {
        if (children == null) {
            children = new ArrayList<GpFilePath>();
        }
        children.add(child);
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
     * Get the read access permission flag for this file path.
     * 
     * TODO: implement isAdmin in the ServerConfiguration Context class.
     * 
     * @param isAdmin, true if the current user has admin privileges.
     * @param userContext
     * @return true iff the currentUser has permission to read this file.
     */
    abstract public boolean canRead(boolean isAdmin, GpContext userContext);
    
    /**
     * Get the string literal to use as an input form value in a job submit form, when this file is to be specified as an input
     * to a module.
     * @return
     */
    abstract public String getFormFieldValue();
    
    /**
     * Get the string literal to use when serializing a job result into the ANALYSIS_JOB.PARAMETER_INFO CLOB.
     * @return
     */
    abstract public String getParamInfoValue();
    
}
