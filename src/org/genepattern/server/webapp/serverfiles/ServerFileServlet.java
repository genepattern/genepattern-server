/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webapp.serverfiles;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.dm.GpFileObjFactory;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.dm.serverfile.ServerFilePath;
import org.genepattern.server.config.Value;

/**
 * Servlet to back the new server files browser
 * @author tabor
 */
public class ServerFileServlet extends HttpServlet {
    private static final long serialVersionUID = 9107065998021054493L;
    public static Logger log = Logger.getLogger(ServerFileServlet.class);
    
    public static final String TREE = "/tree";
    
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) {
        String action = request.getPathInfo();
        
        // Ensure permission to use a server file path
        if (!ensureServerFilePermissions(request)) return;
        
        // Route to the appropriate action, returning an error if unknown
        if (TREE.equals(action)) {
            loadTreeLevel(request, response);
        }
        else {
            // Default to tree if unknown
            loadTreeLevel(request, response);
        }
    }
    
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) {
        doGet(request, response);
    }
    
    @Override
    public void doPut(HttpServletRequest request, HttpServletResponse response) {
        doGet(request, response);
    }
    
    private GpContext getUserContext(HttpServletRequest request) {
        String userId = (String) request.getSession().getAttribute("userID");
        GpContext userContext = GpContext.getContextForUser(userId);
        return userContext;
    }
    
    private boolean ensureServerFilePermissions(HttpServletRequest request) {
        GpContext userContext = getUserContext(request);
        boolean  allowInputFilePaths = ServerConfigurationFactory.instance().getAllowInputFilePaths(userContext);
        return allowInputFilePaths;
    }
    
    private List<File> getSystemBrowseRoots(HttpServletRequest request) {
        GpContext userContext = getUserContext(request);
        Value  roots = ServerConfigurationFactory.instance().getValue(userContext, "server.browse.file.system.root");
        
        List<File> toReturn = new ArrayList<File>();
        for (String path : roots.getValues()) {
            toReturn.add(new File(path));
        }
        return toReturn;
    }

    private void loadTreeLevel(final HttpServletRequest request, final HttpServletResponse response) {
        final GpConfig gpConfig=ServerConfigurationFactory.instance();
        String url = request.getParameter("dir");
        
        List<GpFilePath> tree = null;
        if (url == null) {
            try {
                tree = new ArrayList<GpFilePath>();
                GpContext context = getUserContext(request);
                
                // Add the default root dirs: "server.browse.file.system.root" property
                List<File> roots = getSystemBrowseRoots(request);
                for (File file : roots) {
                    ServerFilePath rootPath = new ServerFilePath(file);
                    rootPath.initMetadata();
                    rootPath.initChildren(context);
                    tree.add(rootPath);
                }
            }
            catch (Exception e) {
                log.error("Error getting root of upload file tree");
            }
        }
        else {
            GpFilePath dir;
            try {
                GpContext context = getUserContext(request);
                dir = GpFileObjFactory.getRequestedGpFileObj(gpConfig, url);
                dir.initMetadata();
                ((ServerFilePath) dir).initChildren(context);
                tree = dir.getChildren();
            }
            catch (Exception e) {
                log.error("Error getting ServerFilePath object in ServerFileServlet");
                return;
            }
            
        }
        
        ServerFileTreeJSON json = null;
        if (!tree.isEmpty()) {
            json = new ServerFileTreeJSON(request, tree);
        }
        else {
            json = new ServerFileTreeJSON(request, null, ServerFileTreeJSON.EMPTY);
        }
        this.write(response, json);
    }
    
    private void write(HttpServletResponse response, Object content) {
        this.write(response, content.toString());
    }
    
    private void write(HttpServletResponse response, String content) {
        PrintWriter writer = null;
        try {
            writer = response.getWriter();
            writer.println(content);
            writer.flush();
        }
        catch (IOException e) {
            log.error("Error writing to the response in ServerFileServlet: " + content);
            e.printStackTrace();
        }
        finally {
            if (writer != null) writer.close();
        }
    }
}
