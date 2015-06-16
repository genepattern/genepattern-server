/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webapp.jsf;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.config.Value;

/**
 * Backing bean for the server file browser ui, currently implemented with jqueryFileTree.
 * 
 * TODO: enable browsing more than one root path
 * @author pcarr
 */
public class ServerFileBrowserBean {
    private static Logger log = Logger.getLogger(ServerFileBrowserBean.class);
    private static final String DEFAULT_ROOT = "/";

    private String serverBrowseFileSystemRoot = null;
    
    private void init() {
        String userId = UIBeanHelper.getUserId();
        GpContext userContext = GpContext.getContextForUser(userId);
        Value value = ServerConfigurationFactory.instance().getValue(userContext, "server.browse.file.system.root");
        if (value != null) {
            serverBrowseFileSystemRoot = value.getValue();
        }
        if (serverBrowseFileSystemRoot == null || serverBrowseFileSystemRoot.trim().equals("")) {
            serverBrowseFileSystemRoot = DEFAULT_ROOT;
        }
    }

    public String getServerBrowseFileSystemRoot() {
        if (serverBrowseFileSystemRoot == null) {
            try {
                init();
            }
            catch (Throwable t) {
                //prototype code, handle unforeseen exceptions
                log.error("Unexpected exception in ServerFileBrowserBean", t);
                serverBrowseFileSystemRoot = DEFAULT_ROOT;
            }
        }
        return serverBrowseFileSystemRoot;
    }
}
