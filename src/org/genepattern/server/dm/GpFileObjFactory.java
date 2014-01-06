package org.genepattern.server.dm;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.dm.jobresult.JobResultFile;
import org.genepattern.server.dm.serverfile.ServerFileObjFactory;
import org.genepattern.server.dm.tasklib.TasklibPath;
import org.genepattern.util.LSID;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoCache;

public class GpFileObjFactory {
    private static Logger log = Logger.getLogger(GpFileObjFactory.class);

//    experimental code, not tested for production server
//    static public GpFilePath getUserHomeDir(final ServerConfiguration.Context userContext) throws Exception {
//        if (userContext == null) {
//            throw new IllegalArgumentException("userContext == null");
//        }
//        if (userContext.getUserId() == null) {
//            throw new IllegalArgumentException("userContext.userId == null");
//        }
//        
//        final File serverFile = ServerConfiguration.instance().getUserDir(userContext);
//        final File relativeFile = new File("users/"+userContext.getUserId());
//
//        final VirtualFilePath homeDir = new VirtualFilePath();
//        homeDir.setServerFile(serverFile);
//        homeDir.setRelativeFile(relativeFile);
//        homeDir.setKind("directory");
//        homeDir.setIsDirectory(true);
//
//        return homeDir;
//    }

//    static public String substituteGpUrl(final String urlStrIn) {
//        if (urlStrIn==null) {
//            log.error("Invalid null arg");
//            return urlStrIn;
//        }
//        if (urlStrIn.contains("<GenePatternURL>")) {
//            final String pattern=Pattern.quote("<GenePatternURL>");
//            final String gpUrlStr=GpFilePath.getGenePatternUrl().toExternalForm();
//            return urlStrIn.replaceAll(pattern, gpUrlStr);
//        }
//        return urlStrIn;
//    }

//    static public boolean isGpFile(final String urlStrIn) {
//        if (urlStrIn.startsWith("<GenePatternURL>")) {
//            return true;
//        }
//        if (urlStrIn.startsWith(GpFilePath.getGenePatternUrl().toExternalForm())) {
//            return true;
//        }
//        return false;
//    }
    
//    /**
//     * 
//     * @param urlStr
//     * @return true, if the given url is to a file on the GP server, as opposed to an external url.
//     */
//    static public boolean isGpUrlPath(final String urlStrIn) {
//        if (urlStrIn==null) {
//            log.error("Invalid null arg");
//            return false;
//        }
//        if (urlStrIn.length()==0) {
//            log.error("Invalid empty arg");
//            return false;
//        }
//        
//        URL gpUrl = ServerConfiguration.instance().getGenePatternURL();
//
//        String urlStr;
//        boolean isGpUrl=false;
//        if (urlStrIn.startsWith("<GenePatternURL>")) {
//            //special-case for <GenePatternURL> substitution
//            isGpUrl=true;
//            Pattern.quote("<GenePatternURL>");
//            urlStr=urlStrIn.replaceFirst(Pattern.quote("<GenePatternURL>"), Pattern.quote(gpUrl.toString())); 
//        }
//        else {
//            if (urlStrIn.startsWith(gpUrl.toString())) {
//                isGpUrl=true;
//            }
//            urlStr=urlStrIn;
//        }
//        
//        //check for valid paths
//        
//        
//        
//        String urlStr;
//        if (urlStrIn.startsWith("<GenePatternURL>")) {
//            //URL url = ServerConfiguration.instance().getGenePatternURL();
//            //String gpUrl=url.toString();
//            String path=urlStrIn.substring("<GenePatternURL>".length());
//            urlStr=gpUrl.toString();
//            //if (urlStr.endsWith("/") && path.startsWith("/")) {
//            //    //remove extra slash
//            //    path=path.substring(1);
//            //}
//            //else if (!urlStr.endsWith("/") && !path.startsWith("/")) {
//            //    //insert slash
//            //    urlStr+="/";
//            //}
//            urlStr+=path;
//        }
//        
//        //URL must either be 
//    }
//
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
     */
    static public GpFilePath getUserUploadFile(ServerConfiguration.Context userContext, File uploadFile) throws Exception {
        if (uploadFile == null) {
            throw new IllegalArgumentException("uploadFile must not be null");
        }
        if (uploadFile.isAbsolute()) {
            throw new Exception("user upload file must be a relative path, uploadFile="+uploadFile.getPath());
        }
        //TODO: quick and dirty way to prevent relative paths to forbidden parent directories
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
    static public GpFilePath getRequestedGpFileObj(URL url) throws Exception {
        URI uri = url.toURI();
        return getRequestedGpFileObj(uri);
    }

    /**
     * Get the GpFilePath reference from a GP URL request.
     *
     * This method does the same stuff that the servlet engine is doing for us.
     * It extracts the servletPath and decoded pathInfo from the URL.
     *
     * The rest of the work is done by {@link GpFileObjFactory#getRequestedGpFileObj(String, String)}
     *
     * @param url, requires a valid url
     * @return a GpFilePath
     * @throws Exception
     */
    static public GpFilePath getRequestedGpFileObj(String urlStr) throws Exception
    {
        return getRequestedGpFileObj(urlStr, (LSID)null);
    }

    /**
     * Get the GpFilePath reference from a GP URL request.
     *
     * This method does the same stuff that the servlet engine is doing for us.
     * It extracts the servletPath and decoded pathInfo from the URL.
     *
     * The rest of the work is done by {@link GpFileObjFactory#getRequestedGpFileObj(String, String)}
     *
     * @param url, requires a valid url
     * @param lsid, the lsid of the task
     * @return a GpFilePath
     * @throws Exception
     */
    static public GpFilePath getRequestedGpFileObj(String urlStr, final LSID lsid) throws Exception {
        //special-case for <libdir> substitution
        if (urlStr.startsWith("<libdir>")) {
            final TaskInfo taskinfo = TaskInfoCache.instance().getTask(lsid.toString());
            //strip out the <libdir> substitution to get the relative path
            final String path = urlStr.replace("<libdir>", "");
            return new TasklibPath(taskinfo, path);
        }

        //special-case for <GenePatternURL> substitution
        if (urlStr.startsWith("<GenePatternURL>")) {
            URL url = ServerConfiguration.instance().getGenePatternURL();
            String gpUrl=url.toString();
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

        //create a uri, which automatically decodes the url
        URI uri = null;
        try {
            uri = new URI(urlStr);
        }
        catch (URISyntaxException e) {
            log.error("Invalid url: "+urlStr, e);
            throw new Exception("Invalid url: "+urlStr);
        }
        return getRequestedGpFileObj(uri);
    }

    static private GpFilePath getRequestedGpFileObj(URI uri) throws Exception {
        final String[] split = splitUri(uri);
        final String servletPath = split[0];
        final String pathInfo = split[1];
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
            
            //TODO: init from either file system or DB depending on context, currently client must do this 
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
        //TODO: implement this properly, in most cases the String literal '<GenePatternURL>' is passed in rather than the actual GenePatternURL
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
     * This method is a temporary helper method until  {@link #getRequestedGpFileObj(String, String)} is fully implemented for the '/jobResults/' type. 
     * 
     * TODO: remove this method when possible
     * 
     * @deprecated - as soon  as {@link #getRequestedGpFileObj(String, String)} is fully implemented for the '/jobResults/' type
     * @param urlStr
     * @return
     * @throws Exception
     */
    static public JobResultFile getRequestedJobResultFileObj(String urlStr) throws Exception {
        URI uri = getUri(urlStr);
        String[] split = splitUri(uri);
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
    static private String[] splitUri(URI uri) {
        String servletPathPlus = uri.getPath();
        //1) chop off the servlet context (e.g. '/gp')
        String gpPath = System.getProperty("GP_Path", "/gp");
        if (servletPathPlus.startsWith(gpPath)) {
            servletPathPlus = servletPathPlus.substring( gpPath.length() );
        }
        
        //2) extract the servletPath and the remaining pathInfo
        String servletPath = servletPathPlus;
        String pathInfo = "";
        int idx = servletPathPlus.indexOf("/", 1);
        if (idx > 0) {
            servletPath = servletPathPlus.substring(0, idx);
            pathInfo = servletPathPlus.substring(idx);
        }
        
        return new String[]{ servletPath, pathInfo };
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
