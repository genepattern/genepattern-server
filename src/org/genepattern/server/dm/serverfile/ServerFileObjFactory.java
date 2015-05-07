/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.dm.serverfile;

import java.io.File;

import org.genepattern.server.dm.GpFilePath;

public class ServerFileObjFactory {

    /**
     * Get a new instance of a ServerFilePath, based on the given serverFile.
     * 
     * @param userContext
     * @param serverFile, if relative path, it is relative to the working directory of the GP server. Not recommended to do that.
     * @return
     */
    static public GpFilePath getServerFile(File serverFile) {
        ServerFilePath serverFilePath = new ServerFilePath(serverFile);
        return serverFilePath;
    }
   
}
