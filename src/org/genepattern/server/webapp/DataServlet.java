package org.genepattern.server.webapp;

import java.io.File;
import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.genepattern.server.UserAccountManager;
import org.genepattern.server.auth.AuthenticationException;

/**
 * Access to GP data files from IGV with support for HTTP Basic Authentication and partial get.
 * 
 * TODO: implement access control based on current user and server file path
 * 
 * Notes:
 *     http://stackoverflow.com/questions/1478401/wrap-the-default-servlet-but-override-the-default-webapp-path
 *     http://tomcat.apache.org/tomcat-6.0-doc/funcspecs/fs-default.html
 *     http://balusc.blogspot.com/2009/02/fileservlet-supporting-resume-and.html
 *     
 * For debugging, use this from the command line:
 *     curl --basic -u "test:test" -H Range:bytes=0-10 --dump-header - http://127.0.0.1:8080/gp/data//Users/pcarr/tmp/test.txt
 *
 * 
 * @author pcarr
 */
public class DataServlet extends HttpServlet implements Servlet {
    private static Logger log = Logger.getLogger(DataServlet.class);

    public DataServlet() {
        super();
    }
    
    public void init() throws ServletException {
        super.init();
    }
    
    public void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String userId = basicAuth(req, resp);
        if (userId == null || userId.trim().length() == 0) {
            //Not authorized, the basicAuth method sends the response back to the client
            return;
        }
        processRequest(req, resp, false);
    }

    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String userId = basicAuth(req, resp);
        if (userId == null || userId.trim().length() == 0) {
            //Not authorized, the basicAuth method sends the response back to the client
            return;
        }
        processRequest(req, resp, true);
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
    private void processRequest(HttpServletRequest request, HttpServletResponse response, boolean serveContent) throws IOException {
        String path = request.getPathInfo();
        if (path == null) {
            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        
        String serverFilepath = path;
        File fileObj = new File(serverFilepath);

        //make sure the file exists and can be read
        if (!fileObj.canRead()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found: "+fileObj.getPath());
            return;
        }
        
        //must sure the current user has permission to read the file
        String userId = LoginManager.instance().getUserIdFromSession(request);
        boolean canRead = canRead(userId, fileObj);

        // Accept ranges header
        response.setHeader("Accept-Ranges", "bytes");
        
        serveFile(request, response, serveContent, fileObj);
    }

    private void serveFile(HttpServletRequest request, HttpServletResponse response, boolean serveContent, File fileObj) 
    throws IOException
    {
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
    private boolean canRead(String userid, File fileObj) {
        //is the current user an admin?
        //is it in the user's web uploads path?
        //is it in the user's soap uploads path?
        //is it a job result file for a job which the user can read?
        return true;
    }
    
    
    /**
     * Authenticate the username:password pair from the request header.
     * 
     * @param request
     * @return the username or null if the client is not (yet) authorized.
     */
    private String basicAuth(HttpServletRequest request, HttpServletResponse response) throws IOException { 
        //for debugging
        String userAgent = request.getHeader("User-Agent");

        //bypass basicauth if the current session already has an authorized user
        String userId = LoginManager.instance().getUserIdFromSession(request);
        if (userId != null) {
            return userId;
        }
        
        boolean allow = false;

        // Get Authorization header
        String auth = request.getHeader("Authorization");
        String[] up = getUsernamePassword(auth);
        userId = up[0];
        String passwordStr = up[1];
        byte[] password = passwordStr != null ? passwordStr.getBytes() : null;
        try {
            allow = UserAccountManager.instance().authenticateUser(userId, password);
        }
        catch (AuthenticationException e) {
        }
        
        // If the user was not validated,
        // fail with a 401 status code (UNAUTHORIZED) and pass back a WWW-Authenticate header for this servlet.
        //
        // Note that this is the normal situation the first time you access the page.  
        // The client web browser will prompt for userID and password and cache them 
        // so that it doesn't have to prompt you again.
        if (!allow) {
            final String realm = "GenePattern";
            response.setHeader("WWW-Authenticate", "BASIC realm=\""+realm+"\"");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        }

        // Otherwise, proceed
        return userId;
    }

    /**
     * Parse out the username:password pair from the authorization header.
     * 
     * @param auth
     * @return <pre>new String[] {<username>, <password>};</pre>
     */
    private String[] getUsernamePassword(String auth) {
        String[] up = new String[2];
        up[0] = null;
        up[1] = null;
        if (auth == null) {
            return up;
        }

        if (!auth.toUpperCase().startsWith("BASIC "))  {
            return up;
        }

        // Get encoded user and password, comes after "BASIC "
        String userpassEncoded = auth.substring(6);

        // Decode it, using any base 64 decoder
        sun.misc.BASE64Decoder dec = new sun.misc.BASE64Decoder();
        String userpassDecoded = null;
        try {
            userpassDecoded = new String(dec.decodeBuffer(userpassEncoded));
        }
        catch (IOException e) {
            log.error("Error decoding username and password from HTTP request header", e);
            return up;
        }
        String username = "";
        String passwordStr = null;
        int idx = userpassDecoded.indexOf(":");
        if (idx >= 0) {
            username = userpassDecoded.substring(0, idx);
            passwordStr = userpassDecoded.substring(idx+1);
        }
        up[0] = username;
        up[1] = passwordStr;
        return up;
    }

}
