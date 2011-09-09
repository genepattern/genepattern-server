package org.genepattern.server.dm;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfiguration;

public class GpFileObjFactory {
    private static Logger log = Logger.getLogger(GpFileObjFactory.class);
    
    
    /**
     * TODO: implement this method
     * Get the working directory for a newly created job instance. This method assumes that this is for a job which is about to 
     * be added to the queue, once a job is added to the queue its directory is stored in the DB.
     * 
     * @param jobContext
     * @return
     * @throws Exception
     */
    static public GpFilePath getWorkingDirectoryForNewJob(ServerConfiguration.Context userContext, String jobId) throws Exception {
        throw new Exception("Not implemented");
        //File jobDir = ServerConfiguration.instance().getRootJobDir(userContext);
    }
    
    /**
     * Get the root user upload directory for the given user.
     * 
     * @param userContext
     * @return
     * @throws Exception
     */
    static public GpFilePath getUserUploadDir(ServerConfiguration.Context userContext) throws Exception {
        File userUploadDir = ServerConfiguration.instance().getUserUploadDir(userContext);
        //1) construct a file reference to the server file
        //   e.g. serverFile=<user.upload.dir>/relativePath
        
        //2) construct a URI from the file, to get the relative uri path
        //   e.g. uriPath=/users/<user_id>[/relativeParentDir]/filename
        //Note: creating a File obj so that we can use UrlUtil methods to encode the path
        File tmp = new File("/users/"+userContext.getUserId()+"/");
        String tmpPath = UrlUtil.encodeFilePath(tmp);
        tmpPath = tmpPath + "/";
        URI relativeUri = null;
        try {
            relativeUri = new URI(tmpPath);
        }
        catch (URISyntaxException e) {
            log.error("Invalid URI: "+tmpPath, e);
        }
        UserUploadFile userUploadFile = new UserUploadFile( relativeUri );
        userUploadFile.setServerFile( userUploadDir );
        userUploadFile.setRelativeFile( new File("./") );
        userUploadFile.setKind("directory");
        userUploadFile.setName(userUploadFile.getServerFile().getName());
        userUploadFile.setLastModified(new Date(userUploadFile.getServerFile().lastModified()));
        return userUploadFile;
    }
    
    /**
     * Create a GpFileObj whose path is relative to the user's upload directory.
     * 
     * @param userContext, must contain a valid user_id.
     * @param uploadFile, a relative path to the upload file, relative to the user's upload directory.
     * @return
     * @throws Exception
     */
    static public GpFilePath getUserUploadFile(ServerConfiguration.Context userContext, File uploadFile) throws Exception {
        if (uploadFile == null) {
            throw new IllegalArgumentException("uploadFile must not be null");
        }
        if (uploadFile.isAbsolute()) {
            throw new Exception("user upload file must be a relative path, uploadFile="+uploadFile.getPath());
        }
        if (uploadFile.getPath().contains("..")) {
            //TODO: quick and dirty way to prevent relative paths to forbidden parent directories
            throw new Exception("uploadFile.path can't contain '..'");
        }
        
        if (userContext == null) {
            throw new IllegalArgumentException("userContext must not be null");
        }
        if (userContext.getUserId() == null) {
            throw new IllegalArgumentException("A valid userId is required, userId=null");
        }
        
        //Note: see UserAccountManager class. If the system is running as expected, the user's home directory is created
        //    before the account is created, therefore we can assume the userId is a valid directory name
        if (userContext.getUserId().contains("/") || userContext.getUserId().contains(File.pathSeparator)) {
            throw new IllegalArgumentException("A valid userId is required, userId='"+userContext.getUserId()+"'. Your userId contains a pathSeparator.");
        }
        File userUploadDir = ServerConfiguration.instance().getUserUploadDir(userContext);

        //TODO: relativize the path, at the moment, just manually removing './'
        String relativePath = uploadFile.getPath();
        if (relativePath.startsWith("./") || relativePath.startsWith("."+File.separator)) {
            relativePath = relativePath.substring(2);
        }
        //1) construct a file reference to the server file
        //   e.g. serverFile=<user.upload.dir>/relativePath
        File serverFile = new File(userUploadDir, relativePath);
        
        //2) construct a URI from the file, to get the relative uri path
        //   e.g. uriPath=/users/<user_id>[/relativeParentDir]/filename
        //Note: creating a File obj so that we can use UrlUtil methods to encode the path
        File tmp = new File("/users/"+userContext.getUserId()+"/"+getPathForwardSlashed(new File(relativePath)));

        String tmpPath = UrlUtil.encodeFilePath(tmp);
        URI relativeUri = null;
        try {
            relativeUri = new URI(tmpPath);
        }
        catch (URISyntaxException e) {
            log.error("Invalid URI: "+tmpPath, e);
        }
        UserUploadFile userUploadFile = new UserUploadFile( relativeUri );
        userUploadFile.setServerFile( serverFile );
        userUploadFile.setRelativeFile( new File(relativePath) );
        userUploadFile.setOwner( userContext.getUserId() );
        return userUploadFile;
    }
    
    static public GpFilePath getRequestedGpFileObj(HttpServletRequest request) throws Exception {
        try {
            String servletPath = request.getServletPath();
            String pathInfo = request.getPathInfo();
            return getRequestedGpFileObj(servletPath, pathInfo);
        }
        catch (Throwable t) {
            throw new Exception("Unable to find gpFileObj for request: "+request.toString());
        }
    }
    
    /**
     * Get the GpFilePath reference from a GP URL request.
     * @param url
     * @return
     * @throws Exception
     */
    static public GpFilePath getRequestedGpFileObj(String url) throws Exception {
        return getRequestedGpFileObj(new URL(url));
    }
    
    /**
     * Get the GpFilePath reference from a GP URL request.
     * 
     * @param url, expecting a full url to a resource on the GP server
     * 
     * @return
     * @throws Exception
     */
    static public GpFilePath getRequestedGpFileObj(URL url) throws Exception {
        //1) get the /servletPath/pathInfo from the url
        String urlStr = url.toExternalForm();
        String genePatternUrl = System.getProperty("GenePatternURL");
        if (genePatternUrl.endsWith("/")) {
            genePatternUrl = genePatternUrl.substring(0, genePatternUrl.length() - 1);
        }
        String servletPathPlus = urlStr.substring( genePatternUrl.length() );
        
        String servletPath = servletPathPlus;
        String pathInfo = "";
        int idx = servletPathPlus.indexOf("/", 1);
        if (idx > 0) {
            servletPath = servletPathPlus.substring(0, idx);
            pathInfo = servletPathPlus.substring(idx);
        }
        return getRequestedGpFileObj(servletPath, pathInfo);
    }
    
    static public GpFilePath getRequestedGpFileObj(String servletPath, String pathInfo) throws Exception {
        if ("/users".equals(servletPath)) {
            String userId = extractUserId(pathInfo);
            //drop the wrapping slashes from '/user_id/'
            String relativePath = pathInfo.substring( userId.length() + 2 );
            File uploadFilePath = new File(relativePath);
            ServerConfiguration.Context userContext = ServerConfiguration.Context.getContextForUser(userId);
            GpFilePath gpFileObj = GpFileObjFactory.getUserUploadFile(userContext, uploadFilePath);
            return gpFileObj;
        }
        if ("/data".equals(servletPath)) {
            throw new Exception("/data/ paths not immplemented!");
        }
        throw new Exception("Invalid servletPath: "+servletPath);
    }
    
    private static String extractUserId(String pathInfo) throws Exception {
        if (!pathInfo.startsWith("/")) {
            //pathInfo should start with a '/'
            throw new Exception("Unexpected input: "+pathInfo);
        }
        int endIndex = pathInfo.indexOf("/", 1);
        if (endIndex < 0) {
            throw new Exception("Unexpected input: "+pathInfo);
        }
        String userId = pathInfo.substring(1, endIndex);
        return userId;
    }

    /**
     * replace all separators with the forward slash ('/').
     * @param file
     * @return
     */
    static private String getPathForwardSlashed(File file) {
        String path = file.getPath();
        String r = path.replace( File.separator, "/");
        return r;
    }

}
