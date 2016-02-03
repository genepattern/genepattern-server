/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.dm;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.genepattern.server.FileUtil;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.dm.jobresult.JobResultFile;
import org.genepattern.server.dm.serverfile.ServerFileObjFactory;
import org.genepattern.server.dm.tasklib.TasklibPath;
import org.genepattern.util.LSID;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoCache;

import com.google.common.base.Strings;

public class GpFileObjFactory {
    private static Logger log = Logger.getLogger(GpFileObjFactory.class);

    /**
     * Get the root user upload directory for the given user.
     * 
     * @param userContext
     * @return
     * @throws Exception
     * 
     * @deprecated pass in a valid GpConfig.
     */
    public static GpFilePath getUserUploadDir(GpContext userContext) throws Exception {
        GpConfig gpConfig=ServerConfigurationFactory.instance();
        return getUserUploadDir(gpConfig, userContext);
    }

    public static GpFilePath getUserUploadDir(GpConfig gpConfig, GpContext userContext) throws Exception {
        if (gpConfig == null) {
            log.error("gpConfig==null, using ServerConfigurationFactory.instance");
            gpConfig=ServerConfigurationFactory.instance();
        }

        File userUploadDir = gpConfig.getUserUploadDir(userContext);
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
        userUploadFile.setOwner(userContext.getUserId());
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
     * 
     * @deprecated pass in a valid GpConfig.
     */
    static public GpFilePath getUserUploadFile(GpContext userContext, File uploadFile) throws Exception {
        final GpConfig gpConfig=ServerConfigurationFactory.instance();
        return getUserUploadFile(gpConfig, userContext, uploadFile);
    }
    
    /**
     * Create a GpFileObj whose path is relative to the user's upload directory.
     * 
     * @param gpConfig, the server configuration instance.
     * @param userContext, must contain a valid user_id.
     * @param uploadFile, a relative path to the upload file, relative to the user's upload directory.
     * @return
     * @throws Exception
     */
    static public GpFilePath getUserUploadFile(GpConfig gpConfig, final GpContext userContext, final File uploadFile) throws Exception {
        if (gpConfig == null) {
            log.error("gpConfig==null, using ServerConfigurationFactory.instance");
            gpConfig=ServerConfigurationFactory.instance();
        }
        File userUploadDir = gpConfig.getUserUploadDir(userContext);
        return getUserUploadFile(userContext, userUploadDir, uploadFile);
    }

    static public GpFilePath getUserUploadFile(final GpContext userContext, final File userUploadDir, final File uploadFile) throws Exception {
        if (uploadFile == null) {
            throw new IllegalArgumentException("uploadFile must not be null");
        }
        if (uploadFile.isAbsolute()) {
            throw new Exception("user upload file must be a relative path, uploadFile="+uploadFile.getPath());
        }
        // quick and dirty way to prevent relative paths to forbidden parent directories
        if (uploadFile.getPath().startsWith("../")) {
            throw new Exception("uploadFile.path can't start with '../'");
        }
        if (uploadFile.getPath().contains("/../")) {
            throw new Exception("uploadFile.path can't contain '/../'");
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
            throw new IllegalArgumentException("A valid userId is required, userId='"+userContext.getUserId()+"'. Your userId contains a pathSeparator character ('/').");
        }

        //relativize the path by manually removing './'
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
        //Note: Java 7 Path.toUri may work
        File tmp = new File("/users/"+userContext.getUserId()+"/"+FileUtil.getPathForwardSlashed(new File(relativePath)));

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
        userUploadFile.setName( serverFile.getName() );
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
     * 
     * @param url, expecting a full url to a resource on the GP server
     * 
     * @return
     * @throws Exception
     */
    static public GpFilePath getRequestedGpFileObj(final GpConfig gpConfig, final URL url) throws Exception {
        URI uri = url.toURI();
        return getRequestedGpFileObj(gpConfig, uri);
    }

    /** @deprecated pass in a valid GpConfig. */
    public static GpFilePath getRequestedGpFileObj(String urlStr) throws Exception {
        return getRequestedGpFileObj(ServerConfigurationFactory.instance(), urlStr);
    }

    /**
     * Get the GpFilePath reference from a GP URL request.
     *
     * This method does the same stuff that the servlet engine is doing for us.
     * It extracts the servletPath and decoded pathInfo from the URL.
     *
     * The rest of the work is done by {@link GpFileObjFactory#getRequestedGpFileObj(GpConfig, String, String)}
     *
     * @param url, requires a valid url
     * @return a GpFilePath
     * @throws Exception
     */
    public static GpFilePath getRequestedGpFileObj(GpConfig gpConfig, String urlStr) throws Exception
    {
        return getRequestedGpFileObj(gpConfig, urlStr, (LSID)null);
    }

    protected static boolean startsWithIgnoreNull(final String str, final String prefix) {
        if (str==null) {
            return false;
        }
        if (Strings.isNullOrEmpty(prefix)) {
            return false;
        }
        return str.startsWith(prefix);
    }
    
    /**
     * Get the GpFilePath reference from a GP URL request.
     *
     * This method does the same stuff that the servlet engine is doing for us.
     * It extracts the servletPath and decoded pathInfo from the URL.
     *
     * The rest of the work is done by {@link GpFileObjFactory#getRequestedGpFileObj(GpConfig, String, String)}
     *
     * @param gpConfig
     * @param baseGpHref
     * @param urlStr, requires a valid url
     * @param lsid, the lsid of the task
     * @return a GpFilePath
     * @throws Exception
     */
    public static GpFilePath getRequestedGpFileObj(final GpConfig gpConfig, /* final String baseGpHref,*/ String urlStr, final LSID lsid) throws Exception {
        //special-case for <libdir> substitution
        if (urlStr.startsWith("<libdir>")) {
            final TaskInfo taskinfo = TaskInfoCache.instance().getTask(lsid.toString());
            //strip out the <libdir> substitution to get the relative path
            final String path = urlStr.replace("<libdir>", "");
            return new TasklibPath(taskinfo, path);
        }

        //special-case for <GenePatternURL> substitution
        if (urlStr.startsWith("<GenePatternURL>")) {
            String gpUrl=gpConfig.getGpUrl();
            String path=urlStr.substring("<GenePatternURL>".length());
            urlStr=gpUrl;
            if (urlStr.endsWith("/") && path.startsWith("/")) {
                //remove extra slash
                path=path.substring(1);
            }
            else if (!urlStr.endsWith("/") && !path.startsWith("/")) {
                //insert slash
                urlStr+="/";
            }
            urlStr+=path;
        }

//        //TODO: special-case: ignore external urls
//        boolean isLocal=false;
//        if (startsWithIgnoreNull(urlStr, gpConfig.getGpUrl())) {
//            isLocal=true;
//        }
//        else if (startsWithIgnoreNull(urlStr, baseGpHref)) {
//            isLocal=true;
//        }
//        if (!isLocal) {
//            throw new Exception("Not a local url: "+urlStr);
//        }

        //create a uri, which automatically decodes the url
        URI uri = null;
        try {
            uri = new URI(urlStr);
        }
        catch (URISyntaxException e) {
            log.error("Invalid url: "+urlStr, e);
            // hack fix for GP-5558
            urlStr=sanixize(gpConfig, urlStr);
            try {
                uri = new URI(urlStr);
            }
            catch (URISyntaxException e1) {
                log.error("Still invalid url: "+urlStr, e);
                throw new Exception("Invalid url: "+urlStr);
            }
        }
        return getRequestedGpFileObj(gpConfig, uri);
    }
    
    static public String sanixize(GpConfig gpConfig, String urlStr) {
        final String gpUrl=gpConfig.getGpUrl();
        if (urlStr.startsWith(gpUrl)) {
            //urlStr=urlStr.replace(" ", "%20");
            String tail=urlStr.substring(gpUrl.length());
            tail=UrlUtil.encodeURIcomponent(tail);
            urlStr=gpUrl+tail;
        }
        return urlStr;
    }

    static private GpFilePath getRequestedGpFileObj(final GpConfig gpConfig, final URI uri) throws Exception {
        final String[] split = UrlUtil.splitUri(gpConfig.getGpPath(), uri);
        final String servletPath = split[0];
        final String pathInfo = split[1];
        return getRequestedGpFileObj(gpConfig, servletPath, pathInfo);        
    }

    /** @deprecated pass in a valid GpConfig */
    public static GpFilePath getRequestedGpFileObj(String servletPath, String pathInfo) throws Exception {
        return getRequestedGpFileObj(ServerConfigurationFactory.instance(), servletPath, pathInfo);
    }
    
    /**
     * Note: When needed, the calling method must init metadata from file system, DB, or pathInfo depending on context. 
     * @param servletPath
     * @param pathInfo
     * @return
     * @throws Exception
     * 
     */
    static public GpFilePath getRequestedGpFileObj(final GpConfig gpConfig, final String servletPath, final String pathInfo) throws Exception {
         if ("/users".equals(servletPath)) {
            String userId = extractUserId(pathInfo);
            //drop the wrapping slashes from '/user_id/'
            String relativePath = pathInfo.substring( userId.length() + 2 );
            File uploadFilePath = new File(relativePath);
            GpContext userContext = GpContext.getContextForUser(userId);
            GpFilePath gpFileObj = GpFileObjFactory.getUserUploadFile(gpConfig, userContext, uploadFilePath);
            return gpFileObj;
        }
        if ("/data".equals(servletPath)) {
            File serverFile = extractServerFile(pathInfo);
            GpFilePath serverFileObj = ServerFileObjFactory.getServerFile(serverFile);
            return serverFileObj;
        }
        if ("/jobResults".equals(servletPath)) {
            JobResultFile jobResultFile = new JobResultFile(pathInfo);
            return jobResultFile;
        }
        
        //special-case for legacy web upload and tasklib paths
        //    http://127.0.0.1:8080/gp/getFile.jsp?task=&job=1222&file=test_run89....546.tmp/all_aml_test.gct
        //    <GenePatternURL>getFile.jsp?task=&job=<job_no>&file=<userid>_run<random_number>.tmp/<filename>
        //if (pathInfo.startsWith("/getFile.jsp?task=&job=")) {
        //    GpFilePath webUploadFileObj = WebUploadFileObjFactory.getWebUploadPath(pathInfo);
        //    return webUploadFileObj;
        //}
        throw new Exception("Invalid servletPath: "+servletPath);
    }
    
    /**
     * Get a JobResultFile, GpFilePath instance, for the given url. 
     * 
     * @deprecated Should call getRequestedGpFileObj instead, or just create a new JobResultFile directly 
     * @param urlStr
     * @return
     * @throws Exception
     */
    static public JobResultFile getRequestedJobResultFileObj(final GpConfig gpConfig, String urlStr) throws Exception {
        if (urlStr.startsWith("<GenePatternURL>")) {
            urlStr=urlStr.replaceFirst("<GenePatternURL>", 
                    ServerConfigurationFactory.instance().getGpPath() + "/");
        }
        URI uri = getUri(urlStr);
        final String[] split = UrlUtil.splitUri(gpConfig.getGpPath(), uri);
        String servletPath = split[0];
        String pathInfo = split[1];
        if ("/jobResults".equals(servletPath)) {
            JobResultFile jobResultFile = new JobResultFile(pathInfo);
            return jobResultFile;
        }
        throw new Exception("Invalid servletPath: "+servletPath);
    }
    
    static public URI getUri(String urlStr) throws URISyntaxException {
        //create a uri, which automatically decodes the url
        URI uri = new URI(urlStr);
        return uri;
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
    
    private static File extractServerFile(String pathInfo) throws Exception {
        if (!pathInfo.startsWith("/")) {
            throw new Exception("Unexpected input: "+pathInfo);
        }
        //skip past the first slash
        return new File( pathInfo.substring(1) );
    }

}
