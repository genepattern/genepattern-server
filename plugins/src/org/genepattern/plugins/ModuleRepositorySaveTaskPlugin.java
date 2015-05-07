/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.plugins;

import edu.mit.genome.gp.util.ZipCatalogUpload;
import org.genepattern.server.webservice.server.SaveTaskPlugin;

import java.io.File;
import java.io.IOException;

/**
 * @author Joshua Gould
 */
public class ModuleRepositorySaveTaskPlugin implements SaveTaskPlugin {
    private static final String THIRD_PARTY_URL =
            "http://wwwdev.broadinstitute.org/webservices/3rdpartymodulerepository/ModuleRepositoryServlet";


    public void taskSaved(File zipFile) {
        ZipCatalogUpload zipCatalogUpload = new ZipCatalogUpload();
        try {
            zipCatalogUpload
                    .upload(THIRD_PARTY_URL, ZipCatalogUpload.MODULE, ZipCatalogUpload.PROD,
                            zipFile.getCanonicalPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        ModuleRepositorySaveTaskPlugin r = new ModuleRepositorySaveTaskPlugin();
        r.taskSaved(new File(args[0]));
    }
}
