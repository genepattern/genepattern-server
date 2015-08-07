/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webapp;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URLConnection;
import java.util.Hashtable;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.webservice.server.DirectoryManager;
import org.genepattern.server.webservice.server.local.LocalTaskIntegratorClient;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;
import org.genepattern.webservice.WebServiceException;

/**
 * Helper servlet for getting module documentation files.
 * <pre>
 * Requires:
 *     userID, from the Session
 *     module name, from the URL by naming convention
 * Optional:
 *     file, from the URL by naming convention
 * 
 * E.g. http://localhost:8080/gp/module/doc/<name>/[<file>]
 * </pre>
 * 
 * <p>
 * The <name> can be an LSID or a name for a module installed on the server. It gets the latest version if a version is not included in the LSID.
 * The <file> can be a name of a documentation file in the taskLib directory for the module. If <file> is not specified, get the default documentation file for the module.
 * <p>
 * Here are some example URLs which work for ComparativeMarkerSelection, which does have two doc files.
 * <ul>
 * <li>
 * http://127.0.0.1:8080/gp/module/doc/urn:lsid:broadinstitute.org:cancer.software.genepattern.module.analysis:00044:4/ComparativeMarkerSelection.pdf
 * <li>
 * http://127.0.0.1:8080/gp/module/doc/urn:lsid:broadinstitute.org:cancer.software.genepattern.module.analysis:00044:4
 * <li>
 * http://127.0.0.1:8080/gp/module/doc/urn:lsid:broadinstitute.org:cancer.software.genepattern.module.analysis:00044
 * <li>
 * http://127.0.0.1:8080/gp/module/doc/ComparativeMarkerSelection
 * <li>
 * http://127.0.0.1:8080/gp/module/doc/urn:lsid:broadinstitute.org:cancer.software.genepattern.module.analysis:00044:4/CompMarkSelAppNote.pdf
 * <li>
 * http://127.0.0.1:8080/gp/module/doc/ComparativeMarkerSelection/CompMarkSelAppNote.pdf
 * </ul>
 * 
 * @author pcarr
 *
 */
public class ModuleDocServlet extends HttpServlet implements Servlet {
    private static final Logger log = Logger.getLogger(ModuleDocServlet.class);
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        processRequest(request, response);
    }
    
    /**
     * Parse the taskNameOrLsid and the filePath from the PathInfo of the HTTP request.
     * 
     * @param request
     * @return a String[2], where String[0] is the module name or lsid, 
     *     and String[1] is the relative path.
     */
    protected static String[] splitPathInfo(HttpServletRequest request) {
        
        final String[] NOT_SET=new String[]{"", null};
        
        String pathInfo=request.getPathInfo();
        if (pathInfo==null) {
            log.error("HttpServletRequest returned null pathInfo");
            return NOT_SET;
        }
        if (pathInfo.startsWith("/")) {
            pathInfo=pathInfo.substring(1);
        }
        else {
            log.error("Expecting '/' as first character in pathInfo");
            pathInfo="/"+pathInfo;
            return NOT_SET;
        }
        final String DELIM="\\Q/\\E";  // <=== equivalent of Pattern.quote("/");
        String[] rval=pathInfo.split(DELIM, 2);
        if (rval==null || rval.length==0) {
            return NOT_SET;
        }
        else if (rval.length==1) {
            return new String[]{rval[0], null};
        }
        return rval;
    }
    
    /**
     * Sanitize the filepath, to avoid getting access to files by absolute path,
     * or in a parent directory.
     * @param filename
     * @return
     */
    protected static String sanitizePath(String filename) {
        if (filename.startsWith("/") || filename.contains("../")) {
            log.error("invalid filepath="+filename);
            //filename must be in the taskLibDir
            // e.g. ignore something like ../../genepattern.properties
            int i = filename.lastIndexOf('/');
            if (i >= 0) {
                filename = filename.substring(i+1,filename.length());
            }
        }
        return filename;
    }
    
    private void processRequest(HttpServletRequest request, HttpServletResponse response) throws IOException { 
        String userID = (String) request.getSession().getAttribute(GPConstants.USERID);
        LocalTaskIntegratorClient taskIntegratorClient = new LocalTaskIntegratorClient(userID, null);
        
        String[] split=splitPathInfo(request);
        String moduleId=split[0]; //a module LSID or name
        String filename=split[1]; //a file path, relative to the libdir of the module

        TaskInfo ti;
        TaskInfoAttributes tia;

        ti = GenePatternAnalysisTask.getTaskInfo(moduleId, userID);
        if (ti == null) {
            showError("Can't find module: "+moduleId, request, response);
            return;
        }
        tia = ti.giveTaskInfoAttributes();

        if (filename != null && filename.length() == 0) filename = null;
        if (filename == null) {
            try {
                File[] docFiles = taskIntegratorClient.getDocFiles(ti);
                if (docFiles.length > 0) {
                    filename = docFiles[0].getName();
                }
            }
            catch (WebServiceException e) {
                showError("Server error: Can't find module '"+moduleId+"'. "+e.getLocalizedMessage(), request, response);
                return;
            }
        }
        if (filename == null) {
            showError("No documentation for module: "+moduleId, request, response);
            return;
        }
        filename=sanitizePath(filename);
        File in = null;
        File taskLibDir = null;
        try {
            String lsid = tia.get(GPConstants.LSID);
            String taskLibDirName = DirectoryManager.getTaskLibDir(ti.getName(), lsid, userID);
            taskLibDir = new File(taskLibDirName);
            if (!taskLibDir.exists() || !taskLibDir.isDirectory() || !taskLibDir.canRead()) {
                //
            }
            else {
                in = new File(taskLibDir, filename);
            }

            if (in == null || !in.exists() || !in.canRead()) {
                showError("Server error: Can't read module documentation file: "+filename, request, response);
                return;
            }
        }
        catch (MalformedURLException e) {
            showError("Server error: "+e.getLocalizedMessage(), request, response);
            return;
        }

        String contentType = URLConnection.getFileNameMap().getContentTypeFor(filename);
        if (contentType == null) {
            final Hashtable<String,String> htTypes = new Hashtable<String, String>();
            htTypes.put(".jar", "application/java-archive");
            htTypes.put(".zip", "application/zip");
            htTypes.put("." + GPConstants.TASK_TYPE_PIPELINE, "text/plain");
            htTypes.put(".class", "application/octet-stream");
            htTypes.put(".pdf", "application/pdf");
            htTypes.put(".doc", "application/msword");

            int i = filename.lastIndexOf(".");
            String extension = (i > -1 ? filename.substring(i) : "");
            contentType = htTypes.get(extension.toLowerCase());
        }
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        response.setHeader("Content-Type", contentType);
        response.setHeader("Content-disposition", "inline; filename=\"" + filename + "\"");

        InputStream is = null;
        OutputStream os = null;
        try {
            os = response.getOutputStream();
            is = new BufferedInputStream(new FileInputStream(in));
            byte[] b = new byte[10000];
            int bytesRead;
            while ((bytesRead = is.read(b)) != -1) {
                response.getOutputStream().write(b, 0, bytesRead);
            }
            os.flush();
            os.close();
            os = null;
        } 
        catch (FileNotFoundException e) {
            throw (e);
        }
        catch (IOException e) {
            throw (e);
        }
        finally {
            if (is != null) {
                try {
                    is.close();
                }
                catch (IOException e) {
                    //TODO: log error
                }
            }
            if (os != null) {
                try {
                    os.close();
                }
                catch (IOException e) {
                    //TODO: log error
                }
            }
        }
    }

    private boolean isNull(String str) {
        return str == null || str.length() == 0;
    }

    private void showError(String message, HttpServletRequest request, HttpServletResponse response) 
    throws IOException
    {
        String redirectURL = request.getContextPath() + "/getTaskDocError.jsp";
        if (!isNull(message)) {
            redirectURL += "?e=" + response.encodeRedirectURL(message);
        }
        response.sendRedirect(redirectURL);
    }
}
