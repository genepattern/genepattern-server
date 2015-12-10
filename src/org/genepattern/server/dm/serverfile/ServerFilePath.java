/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.dm.serverfile;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.config.Value;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.dm.UrlUtil;
import org.genepattern.server.util.ServerFileFilenameFilter;
import org.genepattern.server.webapp.DataServlet;

/**
 * Implement GpFilePath for server files.
 * 
 * @author pcarr
 */
public class ServerFilePath extends GpFilePath {
    private static Logger log = Logger.getLogger(ServerFilePath.class);

    private File serverFile;
    private URI relativeUri;

    /**
     * Create a new ServerFilePath from the given file object.
     * @param userContext
     * @param file, can be relative or absolute. Relative paths are relative to the working directory
     *     for the GP server.
     */
    public ServerFilePath(File file) {
        if (file == null) {
            throw new IllegalArgumentException("invalid null arg, serverFile");
        }
        if (!file.isAbsolute()) {
            log.warn("Relative file arg to ServerFilePath constructor: "+file.getPath(), new Exception("Relative paths are relative to the working directory"));
        }
        this.serverFile = file;
        //init the relativeUri
        String uriPath = "/data/" + UrlUtil.encodeFilePath(serverFile);
        try {
            relativeUri = new URI( uriPath );
        }
        catch (URISyntaxException e) {
            log.error(e);
            throw new IllegalArgumentException(e);
        }
        
        setName(file.getName());
    }

    public URI getRelativeUri() {
        return relativeUri;
    }

    public File getServerFile() {
        return serverFile;
    }

    public File getRelativeFile() {
        return serverFile;
    }

    public String getFormFieldValue() {
        // TODO Auto-generated method stub
        throw new IllegalArgumentException("Not implemented!");
    }

    public String getParamInfoValue() {
        // TODO Auto-generated method stub
        throw new IllegalArgumentException("Not implemented!");
    }

    /**
     * Implement user permissions for server files.
     */
    public boolean canRead(boolean isAdmin, GpContext userContext) {
        if (isAdmin) {
            return true;
        }
        
        if (userContext == null) {
            return false;
        }

        final boolean allowInputFilePaths = ServerConfigurationFactory.instance().getAllowInputFilePaths(userContext);
        if (!allowInputFilePaths) {
            return false;
        }

        final Value value = ServerConfigurationFactory.instance().getValue(userContext, "server.browse.file.system.root");
        if (value == null) {
            //Note: by default, all files on the server's file system are readable
            //final String DEFAULT_ROOT = "/";            
            //value = new CommandProperties.Value(DEFAULT_ROOT);
            return true;
        }
        
        final File theFile = getServerFile();
        final List<String> filepaths = value.getValues();
        for(final String filepath : filepaths) {
            final File rootFile = new File(filepath);
            //if the fileObj is a descendant of the root file, return true
            if (DataServlet.isDescendant(rootFile, theFile)) {
                return true;
            }
        }
        return false; 
    }
    
    public void initChildren() {
        final GpContext context = GpContext.getServerContext();
        initChildren(context);
    }
    
    public void initChildren(final GpContext context) {
        FilenameFilter filter = ServerFileFilenameFilter.getServerFilenameFilter(context);
        
        File file = getServerFile();
        if (!file.isDirectory()) return;
        if (!this.getChildren().isEmpty()) return;
        for (File child : file.listFiles(filter)) {
            ServerFilePath childWrapper = new ServerFilePath(child);
            childWrapper.initMetadata();
            this.addChild(childWrapper);
        }
    }
}
