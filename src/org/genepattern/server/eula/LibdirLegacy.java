/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.eula;

import java.io.File;

import org.apache.log4j.Logger;
import org.genepattern.server.eula.InitException;
import org.genepattern.server.webservice.server.DirectoryManager;

/**
 * Circa GP 3.4.2 and earlier, this is the default way to get the libdir for a module.
 * It has two problems:
 *     1) it has a side effect of creating the directory, even if it shouldn't
 *     2) it is not easy to set this up properly for unit testing
 *     
 * @author pcarr
 */
public class LibdirLegacy implements LibdirStrategy {
    final static private Logger log = Logger.getLogger(LibdirLegacy.class);

    //@Override
    public File getLibdir(final String moduleLsid) throws InitException {
        File tasklibDir = null;
        //TODO: implement safer method, e.g. File tasklibDir = DirectoryManager.getTaskLibDirFromCache(moduleLsid);
        //    getLibDir automatically creates a directory on the file system; it's possible to cause problems if there is bogus input
        try {
            String path = DirectoryManager.getLibDir(moduleLsid);
            if (path != null) {
                tasklibDir = new File(path);
            }
        }
        catch (Throwable t) {
            log.error("Error getting libdir for moduleLsid="+moduleLsid+": "+t.getLocalizedMessage(), t);
            throw new InitException("Error getting libdir for moduleLsid="+moduleLsid+": "+t.getLocalizedMessage());
        }
        return tasklibDir;
    }
}
