package org.genepattern.server.filemanager;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.dm.UrlUtil;

public class GpFileObjFactory {
    private static Logger log = Logger.getLogger(GpFileObjFactory.class);

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
        if (uploadFile.isDirectory()) {
            throw new Exception("uploadFile can't be a directory, uploadFile="+uploadFile.getPath());
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
    static public GpFilePath getRequestedGpFileObj(String servletPath, String pathInfo) throws Exception {
        if ("/users".equals(servletPath)) {
            String userId = extractUserId(pathInfo);
            String relativePath = pathInfo.substring( userId.length() + 1 );
            File uploadFilePath = new File(relativePath);
            ServerConfiguration.Context userContext = ServerConfiguration.Context.getContextForUser(userId);
            GpFilePath gpFileObj = GpFileObjFactory.getUserUploadFile(userContext, uploadFilePath);
            return gpFileObj;
        }
        throw new Exception("Invalid servletPath: "+servletPath);
    }
    
    private static String extractUserId(String pathInfo) throws Exception {
        if (pathInfo.startsWith("/")) {
            throw new Exception("Unexpected input: "+pathInfo);
        }
        int endIndex = pathInfo.indexOf("/");
        if (endIndex < 0) {
            throw new Exception("Unexpected input: "+pathInfo);
        }
        String userId = pathInfo.substring(0, endIndex);
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
