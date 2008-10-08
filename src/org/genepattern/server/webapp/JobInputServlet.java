/*******************************************************************************
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright (2003-2008) by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are
 * reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 *
 *******************************************************************************/

package org.genepattern.server.webapp;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.genepattern.server.webapp.jsf.AuthorizationHelper;
import org.genepattern.util.GPConstants;

/**
 * Access input files that were uploaded to GenePattern via the web interface.
 *
 * @author Peter Carr
 */
public class JobInputServlet extends HttpServlet implements Servlet {
    //private static Logger log = Logger.getLogger(JobInputFilter.class);
    
    public JobInputServlet() {
        super();
    }
    
    public void init() throws ServletException {
        super.init();
    }
    
    public void doGet(HttpServletRequest request, HttpServletResponse response) 
    throws IOException, ServletException
    {
        String useridFromSession = null;
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object obj = session.getAttribute(GPConstants.USERID);
            if (obj instanceof String) {
                useridFromSession = (String) obj;
            }
        }

        //note: this only works because this is a servlet configured in web.xml with one and only one url-pattern:
        //      '/fileupload/*'
        String servletPath = request.getServletPath();
        if (!"/fileupload".equals(servletPath)) {
            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String path = request.getPathInfo(); 
        int idx = path.indexOf("/");
        ++idx;
        int idx2 = path.indexOf("/", idx);
        String useridFromPath = idx < 0 || idx2 < 0 ? null : path.substring(idx, idx2);
        
        boolean allowed = false;
        if (useridFromSession != null && AuthorizationHelper.adminJobs(useridFromSession)) {
            allowed = true;
        }
        else if (useridFromSession != null && useridFromSession.equals(useridFromPath)) {
            allowed = true;
        }
        if (!allowed) {
            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN);
            return;            
        }

        //replace url path separators with file system path separators
        path = path.replace('/', File.separatorChar);
        File fileuploadDir = new File("fileupload");
        File file = new File(fileuploadDir, path);
        if (!file.canRead()) {
            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN);
        }
        serveFile(file, response);        
    }
    
    private void serveFile(File fileObj, HttpServletResponse httpServletResponse) 
    throws IOException
    {
        String lcFileName = fileObj.getName().toLowerCase();

        httpServletResponse.setHeader("Content-disposition", "inline; filename=\"" + fileObj.getName() + "\"");
        httpServletResponse.setHeader("Cache-Control", "no-store");
        httpServletResponse.setHeader("Pragma", "no-cache");
        httpServletResponse.setDateHeader("Expires", 0);
        httpServletResponse.setDateHeader("Last-Modified", fileObj.lastModified());
        httpServletResponse.setHeader("Content-Length", "" + fileObj.length());

        if (lcFileName.endsWith(".html") || lcFileName.endsWith(".htm")){
            httpServletResponse.setHeader("Content-Type", "text/html"); 
        }
    
        BufferedInputStream is = null;
        try {
            OutputStream os = httpServletResponse.getOutputStream();
            is = new BufferedInputStream(new FileInputStream(fileObj));
            byte[] b = new byte[10000];
            int bytesRead;
            while ((bytesRead = is.read(b)) != -1) {
                os.write(b, 0, bytesRead);
            }
        }
        catch (FileNotFoundException e) {
            httpServletResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
        } 
        finally {
            if (is != null) {
                try {
                    is.close();
                } 
                catch (IOException x) {
                }
            }
        }
    }
}
