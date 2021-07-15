/*******************************************************************************
 * Copyright (c) 2003-2021 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.junitutil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

    public static final File patchesDir=new File("test/data/patches").getAbsoluteFile();
    /**
     * Get the patchesDir for unit testing, as a fully qualified File
     * to the top-level './test/data/patches' directory for the project.
     */
    public static File getPatchesDir() {
        return patchesDir;
    }

    private static File dataDir=initDataDir();

    /** 
     * Initialize the dataDir, optionally from the GP_DATA_DIR
     * system property.
     */
    protected static File initDataDir() { 
        return new File(System.getProperty("GP_DATA_DIR", "test/data")).getAbsoluteFile();
    }

    /**
     * Get the top-level directory for data files used by the unit tests. 
     * It's hard-coded to 'test/data'.
     * Set GP_DATA_DIR as a system property if you need to use a different path.
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
    
    /**
     * Read all lines from the given input file.
     * 
     * @param file
     * @return a list of lines
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static List<String> readLines(final File file) throws FileNotFoundException, IOException {
        final List<String> lines=new ArrayList<String>();
        BufferedReader b=null;
        try {
            b = new BufferedReader(new FileReader(file));
            String line="";
            while ((line = b.readLine()) != null) {
                lines.add(line);
            }
        }
        finally {
            if (b!=null) {
                b.close();
            }
        }
        return lines;
    }



}
