package org.genepattern.server.webapp;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.genepattern.server.auth.AuthenticationException;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.config.Value;
import org.genepattern.server.dm.GpFileObjFactory;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.webapp.jsf.AuthorizationHelper;
import org.genepattern.server.webapp.jsf.UIBeanHelper;

/**
 * Access to GP data files which are on the server file path. 
 * This servlet was added as part of IGV support. 
 * 
 * Use-case: Run the IGV module in GenePattern, specifying a server file path as input to the module.
 *     The IGV module converts the server file path to a url to this servlet.
 *     The IGV client requests data from this servlet.
 * 
 * Requirements:
 *     Must support HTTP Basic Authentication.
 *     Must support partial GET.
 *     Only allow authenticated users with permission to browser server file paths access to the data files.
 *     
 * Response codes:
 *     401-Unauthorized, if the request is not yet authenticated, e.g. not able to map the request to a GP user
 *     401-Unauthorized, if the valid gp user does not have permission to view the file.
 *     404-Not Found, if the file is not on the server. 
 * 
 * Notes:
 *     http://stackoverflow.com/questions/132052/servlet-for-serving-static-content
 *     http://stackoverflow.com/questions/1478401/wrap-the-default-servlet-but-override-the-default-webapp-path
 *     http://tomcat.apache.org/tomcat-6.0-doc/funcspecs/fs-default.html
 *     http://balusc.blogspot.com/2009/02/fileservlet-supporting-resume-and.html
 *     http://www.kuligowski.pl/java/rest-style-urls-and-url-mapping-for-static-content-apache-tomcat,5
 *     
 * For debugging, use this from the command line:
 *     curl --basic -u "test:test" -H Range:bytes=0-10 --dump-header - http://127.0.0.1:8080/gp/data//Users/pcarr/tmp/test.txt
 *
 * 
 * @author pcarr
 */
public class DataServlet extends HttpServlet implements Servlet {
    private static Logger log = Logger.getLogger(DataServlet.class);

    private enum HTTPMethod {
        DELETE, GET, HEAD, POST, PUT
    }

    public DataServlet() {
        super();
    }
    
    public void init() throws ServletException {
        super.init();
    }
    
    public void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        processRequest(req, resp, HTTPMethod.HEAD);
    }

    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        processRequest(req, resp, HTTPMethod.GET);
    }
    
    //read-only servlet, override PUT, POST, DELETE
    public void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException  {
        resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }
    
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException  {
        resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }
    
    public void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException  {
        resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }
    
    private URL getRequestUrl(HttpServletRequest request) {
        //final String requestUrl = request.getRequestURL().toString();

        final String scheme = request.getScheme();
        final String serverName = request.getServerName();
        final int serverPort = request.getServerPort();
        final String contextPath = request.getContextPath();
        final String servletPath = request.getServletPath();
        final String pathInfo = request.getPathInfo();
        final String queryString = request.getQueryString();

        String u = scheme + "://" + serverName;
        if (serverPort > 0) {
            u += (":" +serverPort);
        }
        u += contextPath + servletPath;
        if (pathInfo != null) {
            u+= pathInfo;
        }
        if (queryString != null) {
            u+= ("?" + queryString);
        }
        
        URL url = null;
        try {
            url = new URL(u);
        }
        catch (MalformedURLException e) {
            log.error(e);
        }
        return url;
    }
    
    /**
     * Map the request to a local file path, authorize the current user, and stream the file back in response.
     * 
     * Note: with Basic Authentication it is not common for clients to logout from a session
     *     This can be a problem when different GP user accounts are requesting content from the 
     *     data servlet
     *     To support this scenario, this method must validate that the current GP user has
     *     access to the requested file, and then send a 401 SC_UNAUTHORIZED if it doesn't
     * 
     * @param request
     * @param response
     * @param serveContent, if false, only respond with the header
     * @throws IOException
     */
    private void processRequest(HttpServletRequest request, HttpServletResponse response, HTTPMethod httpMethod) throws IOException {
        if (log.isDebugEnabled()) {
            //for debugging
            final String requestUrl = request.getRequestURL().toString();
            final String userAgent = request.getHeader("User-Agent");
            log.debug(request.getMethod()+" "+requestUrl+" from "+userAgent);

            final String scheme = request.getScheme();
            final String serverName = request.getServerName();
            final int serverPort = request.getServerPort();
            final String contextPath = request.getContextPath();
            final String servletPath = request.getServletPath();
            final String pathInfo = request.getPathInfo();
            final String queryString = request.getQueryString();

            String u = scheme + "://" + serverName;
            if (serverPort > 0) {
                u += (":" +serverPort);
            }
            u += contextPath + servletPath + "/";
            if (pathInfo != null) {
                u+= pathInfo;
            }
            if (queryString != null) {
                u+= ("?" + queryString);
            }

            log.debug(request.getMethod()+" "+u);
        }
        
        if (isUserUploadRequest(request)) {
            processUserUploadRequest(request, response, httpMethod);
            return;
        }

        //1) require an authenticated GP user account
        String gpUserId = null;
        try {
            gpUserId = BasicAuthUtil.getAuthenticatedUserId(request, response);
        }
        catch (AuthenticationException e) {
            BasicAuthUtil.requestAuthentication(response, e.getLocalizedMessage());
            return;
        }
        if (gpUserId == null) {
            log.error("Unexpected null gpUserId, AuthenticationException should have been thrown.");
            BasicAuthUtil.requestAuthentication(request, response);
            return;
        }

        //2) require a valid server file
        File fileObj = getRequestedFile(request); 
        if (fileObj == null || fileObj.isDirectory()) {
            log.debug("Directory listings are forbidden: "+fileObj);
            //not implementing directory browser 
            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid request: "+fileObj);
            return;
        }
        if (!fileObj.exists()) {
            log.debug("File does not exist: "+fileObj.getPath());
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found: "+fileObj.getPath());
            return;
        }
        if (!fileObj.canRead()) {
            log.debug("Server can't read file: "+fileObj.getPath());
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found: "+fileObj.getPath());
            return;
        }
        
        //3) require the GP user account is authorized to read the file
        boolean gpUserCanRead = gpUserCanRead(gpUserId, fileObj);
        if (!gpUserCanRead) {
            String message = "The user '"+gpUserId+"' does not have permission to read the file '"+fileObj.getPath()+"'";
            log.debug(message);
            BasicAuthUtil.requestAuthentication(response, message);
            return;
        }

        // Accept ranges header
        response.setHeader("Accept-Ranges", "bytes");
        serveFile(request, response, httpMethod, fileObj);
    }

    /**
     * Is this a request for a user upload file? Based on our naming convention, '/users'.
     * @param request
     * @return
     */
    private boolean isUserUploadRequest(HttpServletRequest request) {
        return "/users".equals( request.getServletPath() );
    }
    
    /**
     * Serve the requested user upload file, checking for permission first.
     * 
     * Examples,
     *     http://127.0.0.1:8080/gp     /users  /   test    /   test.cls
     *     http://127.0.0.1:8080/gp     /users  /   test    /   tutorial/all_aml_test.cls
     * 
     * @param request
     * @param response
     * @param httpMethod
     */
    private void processUserUploadRequest(HttpServletRequest request, HttpServletResponse response, HTTPMethod httpMethod) throws IOException {  
        final URL requestUrl = getRequestUrl( request );
        GpFilePath requestedFile = null;
        try {
            //TODO: delete the following commented out line(s), it is here to illustrate
            //    how to get a GpFileObj from an arbitrary GP URL
            //requestedFile = GpFileObjFactory.getRequestedGpFileObj(requestUrl);
            requestedFile = GpFileObjFactory.getRequestedGpFileObj(request);
        }
        catch (Throwable t) {
            log.error(t);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, t.getLocalizedMessage());
            return;
        }
        if (requestedFile == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Error processing request: "+request.getPathInfo());
            return;
        }
        
        //1) require an authenticated GP user account
        String gpUserId = null;
        try {
            gpUserId = BasicAuthUtil.getAuthenticatedUserId(request, response);
        }
        catch (AuthenticationException e) {
            BasicAuthUtil.requestAuthentication(response, e.getLocalizedMessage());
            return;
        }
        if (gpUserId == null) {
            log.error("Unexpected null gpUserId, AuthenticationException should have been thrown.");
            BasicAuthUtil.requestAuthentication(request, response);
            return;
        }

        //2) check if the current user is authorized to read the file
        boolean canRead = false;
        boolean isAdmin = AuthorizationHelper.adminJobs(gpUserId);
        if (isAdmin) {
            //admin users can read all files
            canRead = true;
        }
        else {
            //otherwise, only the owner of a file can read it
            //TODO: implement sharing for user upload files
            canRead = gpUserId != null && gpUserId.equals( requestedFile.getOwner() );
        }
        
        if (!canRead) {
            String message = "The user '"+gpUserId+"' does not have permission to read the file '"+requestedFile.getRelativeUri()+"'";
            log.debug(message);
            BasicAuthUtil.requestAuthentication(response, message);
            return;
        }
        
        // Accept ranges header
        response.setHeader("Accept-Ranges", "bytes");
        serveFile(request, response, httpMethod, requestedFile.getServerFile());
    }

    /**
     * Map the requested resource to a server file path.
     * @param request
     * @return a File, or null if the path is not specified in the request.
     */
    private File getRequestedFile(HttpServletRequest request) {
        String path = request.getPathInfo();
        if (path == null) {
            return null;
        }
        
        //the server file path is taken directly from request#pathInfo
        File fileObj = new File(path);
        return fileObj;
    }

    private void serveFile(HttpServletRequest request, HttpServletResponse response, HTTPMethod httpMethod, File fileObj) 
    throws IOException
    {
        boolean serveContent = true;
        if (httpMethod == HTTPMethod.HEAD) {
            serveContent = false;
        }
        log.debug(""+httpMethod+" "+request.getRequestURI());
        FileDownloader.serveFile(this.getServletContext(), request, response, serveContent, fileObj);    
    }

    /**
     * Check permissions for the given file. This is implemented based on the rules for file access circa GP 3.3.2.
     * Note: These rules will change as we make improvements to how server file paths are managed.
     * 
     * @param userid, the current user.
     * @param fileObj, a file on the server's file system.
     * @return true iff the current user has permission to read the file.
     */
    public static boolean gpUserCanRead(String userid, File fileObj) {
        //admin users can read all files
        //TODO: come up with an improved policy for ACL for admin users
        boolean isAdmin = false;
        isAdmin = AuthorizationHelper.adminJobs(userid);
        if (isAdmin) {
            return true;
        }

        GpContext userContext = GpContext.getContextForUser(userid); 
        boolean isInUploadDir = isInUserUploadDir(userContext, fileObj);
        if (isInUploadDir) {
            return true;
        }
        
        boolean canReadServerFile = canReadServerFile(userContext, fileObj);
        if (canReadServerFile) {
            return true;
        }
        return false;
    }

    /**
     * @param userContext
     * @param fileObj
     * @return true if the given file is in the user upload directory
     */
    private static boolean isInUserUploadDir(GpContext userContext, File fileObj) {
        File userUploadDir = ServerConfigurationFactory.instance().getUserUploadDir(userContext);
        return isDescendant(userUploadDir, fileObj);
    }
    
    /**
     * 
     * @param userContext
     * @param fileObj
     * @return true of the given user has permission to read the file on the server file path.
     */
    public static boolean canReadServerFile(GpContext userContext, File fileObj) {
        Value value = ServerConfigurationFactory.instance().getValue(userContext, "server.browse.file.system.root");
        if (value == null) {
            //Note: by default, all files on the server's file system are readable
            //final String DEFAULT_ROOT = "/";            
            //value = new CommandProperties.Value(DEFAULT_ROOT);
            return true;
        }
        
        List<String> filepaths = value.getValues();
        for(String filepath : filepaths) {
            File rootFile = new File(filepath);
            //if the fileObj is a descendant of the root file, return true
            if (isDescendant(rootFile, fileObj)) {
                return true;
            }
        }
        return false; 
    }

    /**
     * Is the given child file a descendant of the parent file,
     * based on converting to a URI and using {@link URI#relativize(URI)}.
     * 
     *     Note: does not (and should not) consider alternate paths such as symbolic links.
     * 
     * @param parentDir
     * @param childFile
     * @return true if the childFile is a descendant of the parentDir, based on the pathname.
     */
    public static boolean isDescendant(final File parentDir, final File childFile) {
        if (parentDir==null) {
            return false;
        }
        if (!parentDir.isDirectory()) {
            return false;
        }
        if (childFile==null) {
            return false;
        }
        
        final URI parentURI = parentDir.toURI();
        final URI childURI = childFile.toURI();
        final URI relative = parentURI.relativize(childURI);
        final boolean isAbsolute=relative.isAbsolute();
        return !isAbsolute;
    }
    
    public static String getDataServlertUrl() {
        return UIBeanHelper.getServer() + "/data/";
    }

    public static GpFilePath getFileFromUrl(String url) {
        try {
            return GpFileObjFactory.getRequestedGpFileObj(url);
        }
        catch (Exception e) {
            log.error("Error getting the filePath for " + UIBeanHelper.getUserId() + "'s file " + url);
            return null;
        }
    }

}
