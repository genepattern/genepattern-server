package org.genepattern.server.webapp;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.executor.CommandProperties;

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
            String requestUrl = request.getRequestURL().toString();
            String userAgent = request.getHeader("User-Agent");
            log.debug(request.getMethod()+" "+requestUrl+" from "+userAgent);
        }

        //1) require an authenticated GP user account
        String gpUserId = BasicAuthUtil.getAuthenticatedUserId(request, response);
        if (gpUserId == null) {
            BasicAuthUtil.requestAuthentication(response);
            return;
        }

        //2) require a valid server file
        File fileObj = getRequestedFile(request);        
        if (fileObj == null || fileObj.isDirectory()) {
            //not implementing directory browser 
            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid request: "+fileObj);
            return;
        }
        if (!fileObj.canRead()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found: "+fileObj.getPath());
            return;
        }
        
        //3) require the GP user account is authorized to read the file
        boolean gpUserCanRead = gpUserCanRead(gpUserId, fileObj);
        if (!gpUserCanRead) {
            BasicAuthUtil.requestAuthentication(response);
            return;
        }

        // Accept ranges header
        response.setHeader("Accept-Ranges", "bytes");
        serveFile(request, response, httpMethod, fileObj);
    }

//    /**
//     * Check the servlet request for an authenticated user.
//     * First check for a gp userid from the session, then
//     * do basic HTTP Authentication.
//     * 
//     * @param req
//     * @param resp
//     * @return a valid userid, or null if not authenticated.
//     * @throws IOException
//     */
//    private String getAuthenticatedUserId(HttpServletRequest req, HttpServletResponse resp) throws IOException {
//        String userIdFromSession = LoginManager.instance().getUserIdFromSession(req);
//
//        // Get Authorization header
//        String userIdFromAuthorizationHeader = null;
//        byte[] password = null;
//        String auth = req.getHeader("Authorization");
//        if (auth != null) {
//            String[] up = getUsernamePassword(auth);
//            userIdFromAuthorizationHeader = up[0];
//            String passwordStr = up[1];
//            password = passwordStr != null ? passwordStr.getBytes() : null;
//        }
//
//        //if the session is already authenticated
//        if (userIdFromSession != null) {
//            if (userIdFromAuthorizationHeader == null || userIdFromSession.equals(userIdFromAuthorizationHeader)) {
//                return userIdFromSession;
//            }
//            //special-case when the userId from the session doesn't match the one in the authorization header
//            //LoginManager.instance().logout(req, resp, false);
//            userIdFromSession = null;
//        }
//
//        //if we are here, check the authorization header ...
//        try {
//            boolean authenticated = UserAccountManager.instance().authenticateUser(userIdFromAuthorizationHeader, password);
//            if (authenticated) {
//                //LoginManager.instance().addUserIdToSession(req, userIdFromAuthorizationHeader);
//                return userIdFromAuthorizationHeader;
//            }
//        }
//        catch (AuthenticationException e) {
//        }
//
//        //if we are here, it means we are not authenticated, return null
//        return null;
//    }
    
//    /**
//     * Parse out the username:password pair from the authorization header.
//     * 
//     * @param auth
//     * @return <pre>new String[] {<username>, <password>};</pre>
//     */
//    private String[] getUsernamePassword(String auth) {
//        String[] up = new String[2];
//        up[0] = null;
//        up[1] = null;
//        if (auth == null) {
//            return up;
//        }
//
//        if (!auth.toUpperCase().startsWith("BASIC "))  {
//            return up;
//        }
//
//        // Get encoded user and password, comes after "BASIC "
//        String userpassEncoded = auth.substring(6);
//
//        // Decode it, using any base 64 decoder
//        sun.misc.BASE64Decoder dec = new sun.misc.BASE64Decoder();
//        String userpassDecoded = null;
//        try {
//            userpassDecoded = new String(dec.decodeBuffer(userpassEncoded));
//        }
//        catch (IOException e) {
//            log.error("Error decoding username and password from HTTP request header", e);
//            return up;
//        }
//        String username = "";
//        String passwordStr = null;
//        int idx = userpassDecoded.indexOf(":");
//        if (idx >= 0) {
//            username = userpassDecoded.substring(0, idx);
//            passwordStr = userpassDecoded.substring(idx+1);
//        }
//        up[0] = username;
//        up[1] = passwordStr;
//        return up;
//    }

//    /**
//     * Call this method when the current request is not authenticated. 
//     * Fail with a 401 status code (UNAUTHORIZED) 
//     * and respond with the WWW-Authenticate header for this servlet.
//     * 
//     * Note that this is the normal situation the first time you request a protected resource.
//     * The client web browser will prompt for userID and password and cache them
//     * so that it doesn't have to prompt you again.
//     * @param response
//     * @throws IOException
//     */
//    private void requestAuthentication(HttpServletResponse response) throws IOException {
//        final String realm = "GenePattern";
//        response.setHeader("WWW-Authenticate", "BASIC realm=\""+realm+"\"");
//        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
//    }

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
    private boolean gpUserCanRead(String userid, File fileObj) {
        ServerConfiguration.Context userContext = ServerConfiguration.Context.getContextForUser(userid);
        boolean allowInputFilePaths = ServerConfiguration.instance().getAllowInputFilePaths(userContext);
        if (!allowInputFilePaths) {
            return false;
        }
        
        CommandProperties.Value value = ServerConfiguration.instance().getValue(userContext, "server.browse.file.system.root");
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
            try {
                if (isDescendant(rootFile, fileObj)) {
                    return true;
                }
            }
            catch (IOException e) {
                log.error("Error in isDescendant(rootFile="+ rootFile.getAbsolutePath()
                        +", fileObj="+fileObj.getAbsolutePath()
                        +"): "+e.getLocalizedMessage(), e);
            }
        }
        return false;
    }

    /**
     * Is the given child file a descendant of the parent file,
     * based on comparing canonical path names.
     * 
     * @param parent
     * @param child
     * @return
     * @throws IOException, which can occur when calling getCanonicalPath.
     * 
     * Notes:
     *     http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5002170
     */
    public static boolean isDescendant(File parent, File child) throws IOException {
        if (child.equals(parent)) {
            return true;
        } 
        
        //assume canonical paths are sufficient
        String canonicalParent = parent.getAbsoluteFile().getCanonicalPath();
        String canonicalChild = child.getAbsoluteFile().getCanonicalPath();
        if (canonicalChild.startsWith(canonicalParent)) {
            return true;
        }

        //Note: doesn't follow sym links
        return false;
    }

}
