/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.junitutil;

import java.io.File;

import org.junit.Ignore;

@Ignore
public class FileUtil {
    /**
     * Helper class which returns the parent File of this source file.
     * @return
     */
    public static File getSourceDir(final Class<?> clazz) {
        String cname = clazz.getCanonicalName();
        int idx = cname.lastIndexOf('.');
        String dname = cname.substring(0, idx);
        dname = dname.replace('.', '/');
        File sourceDir = new File("test/src/" + dname);
        return sourceDir;
    }
    
    /**
     * Get a File object, from the name of a file which is in the same directory as this source file.
     * @param filename
     * @return
     */
    public static File getSourceFile(final Class<?> clazz, String filename) {
        File p = getSourceDir(clazz);
        return new File(p, filename);
    }

    public static final File resourcesDir=new File("resources").getAbsoluteFile();
    /**
     * Get the <resources> directory for unit testing, as a fully qualified File
     * to the top-level './resources' directory for the project.
     */
    public static File getResourcesDir() {
        return resourcesDir;
    }

    public static final File webappDir=new File("website").getAbsoluteFile();
    /**
     * Get the webappDir for unit testing, as a fully qualified File
     * to the top-level './website' directory for the project.
     * @return
     */
    public static File getWebappDir() {
        return webappDir;
    }

    private static File dataDir=new File("test/data");
    /**
     * Get the top-level directory for data files used by the unit tests. 
     * It's hard-coded to 'test/data'.
     * 
     * @return
     */
    public static File getDataDir() {
        return dataDir;
    }

    /**
     * Get a data file from the given relative path.
     * @param relativePath
     * @return
     */
    public static File getDataFile(final String relativePath) {
        return new File(dataDir, relativePath);
    }

}
