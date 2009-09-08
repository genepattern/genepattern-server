package org.genepattern.server.webapp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
//import org.genepattern.server.util.AuthorizationManagerFactory;
//import org.genepattern.server.util.IAuthorizationManager;
import org.genepattern.util.GPConstants;

/**
 * Helper servlet for GETting files stored via a server file path. This is used to serve input files from the job status page. 
 * For example, when the server is configured to 'allow.input.file.paths'. 
 * The link from the job status page for a job which uses a server file path as input,
 *     <GenePatternUrl>/<urlprefix>/<server.file.path>
 * @author pcarr
 *
 */
public class FileServlet extends HttpServlet {
    private static Logger log = Logger.getLogger(FileServlet.class);
    private static final int DEFAULT_BUFFER_SIZE = 10240; // 10KB.

    private boolean allowInputFilePaths = false; 
    public void init() throws ServletException {
        //TODO: make this a reloadable property
        String inputFilePathProp = System.getProperty("allow.input.file.paths", "false");
        if (inputFilePathProp.equalsIgnoreCase("true") || inputFilePathProp.equals("1")) {
            allowInputFilePaths = true;
        }
    }
    
    public void doGet(HttpServletRequest request, HttpServletResponse response) 
    throws ServletException, IOException {
        if (!allowInputFilePaths) {
            // no server files unless allow.input.file.paths=true
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Server not configured to allow input file paths");
            return;
        }
        //boolean isAdmin = false;
        ////currently, only admin users are allowed to use the 'allow.input.file.paths' feature
        //String userId = getUserIdFromSession(request);
        //IAuthorizationManager authManager = AuthorizationManagerFactory.getAuthorizationManager();
        //isAdmin = authManager.checkPermission("adminServer", userId);
        //if (!isAdmin) {
        //    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Must be admin user to download server file paths");
        //    return;
        //}

        File serverFile = null;
        String serverFilePath = request.getPathInfo();
        if (serverFilePath != null) {
            serverFile = new File(serverFilePath);
        }
        if (serverFile == null || !serverFile.canRead()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid server file path: "+serverFilePath);
        }
        
        serveFile(response, serverFile);
    }

    //private String getUserIdFromSession(HttpServletRequest request) {
    //    HttpSession session = request.getSession(false);
    //    if (session != null) {
    //        Object obj = session.getAttribute(GPConstants.USERID);
    //        if (obj instanceof String) {
    //            return (String) obj;
    //        }
    //    }
    //    return null;        
    //}

    private void serveFile(HttpServletResponse response, File file) throws IOException {
        String contentType = getServletContext().getMimeType(file.getName());
        String contentLength = String.valueOf(file.length());
        
        response.reset();
        response.setBufferSize(DEFAULT_BUFFER_SIZE);
        response.setHeader("Content-Type", contentType);
        response.setHeader("Content-Length", contentLength);
        response.setHeader("Content-Disposition", "inline; filename\"" + file.getName() + "\"");
        
        BufferedInputStream input = null;
        BufferedOutputStream output = null;
        try {
            input = new BufferedInputStream(new FileInputStream(file), DEFAULT_BUFFER_SIZE);
            output = new BufferedOutputStream(response.getOutputStream(), DEFAULT_BUFFER_SIZE);
            
            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            int length;
            while ((length=input.read(buffer)) >0) {
                output.write(buffer, 0, length);
            }
            output.flush();
        }
        finally {
            close(output);
            close(input);
        }
    }
    
    private static void close(Closeable resource) {
        if (resource != null) {
            try {
                resource.close();
            } 
            catch (IOException e) {
                log.error("Error closing resource: "+resource.toString(), e);
            }
        }
    }

}
