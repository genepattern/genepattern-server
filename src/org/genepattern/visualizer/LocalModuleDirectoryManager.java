/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.visualizer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * 
 * @author Joshua Gould
 * 
 */
public class LocalModuleDirectoryManager {
    /** name of file for storing LSID-directory key-value pairs */
    private final static String databaseFilename = ".gp-module-database.properties";
    /** lock file for synchronization across multiple JVMs */
    private final static String lock = ".#gp.lock";
    /** name where module support files are stored */
    private String directoryName;
    private String moduleLsid;
    private String moduleName;
    private String directory = System.getProperty("java.io.tmpdir");

    /**
     * Creates a new instance of <tt>LocalModuleDirectoryManager</tt>/
     * 
     * @param lsid
     *                The module LSID.
     * @param moduleName
     *                The module name.
     */
    public LocalModuleDirectoryManager(String lsid, String moduleName) {
	this.moduleLsid = lsid;
	this.moduleName = moduleName;
    }

    /**
     * Initializes this instance.
     * 
     * @throws IOException
     *                 If an error occurs.
     */
    public void init() throws IOException {
	FileInputStream fis = null;
	FileLock fileLock = null;
	Properties props = new Properties();
	RandomAccessFile randomAccessFile = null;
	File databaseFile = null;
	try {
	    try {
		randomAccessFile = new RandomAccessFile(new File(directory, lock), "rws");
		fileLock = randomAccessFile.getChannel().lock();
		databaseFile = new File(directory, databaseFilename);
		databaseFile.createNewFile();
		fis = new FileInputStream(databaseFile);
		props.load(fis);
	    } catch (IOException e) {
		e.printStackTrace();
		throw e;
	    } finally {
		try {
		    if (fis != null) {
			fis.close();
		    }
		} catch (IOException e) {

		}
	    }

	    directoryName = props.getProperty(moduleLsid);
	    Set directories = new HashSet(props.values());
	    if (directoryName == null) {
		if (moduleName.length() > 253) {
		    moduleName = moduleName.substring(0, 252);
		}
		directoryName = moduleName;
		int counter = 1;
		while (directories.contains(directoryName)) {
		    directoryName = moduleName + "-" + counter;
		    counter++;
		}
		props.setProperty(moduleLsid, directoryName);
		FileOutputStream fos = null;
		try {
		    fos = new FileOutputStream(databaseFile);
		    props.store(fos, "");
		} catch (IOException e) {
		    e.printStackTrace();
		    throw e;
		} finally {
		    try {
			if (fos != null) {
			    fos.close();
			}
		    } catch (IOException e) {

		    }
		}

	    }
	} finally {
	    try {
		if (fileLock != null) {
		    fileLock.release();
		}
	    } catch (IOException e) {
		e.printStackTrace();
	    }

	    if (randomAccessFile != null) {
		randomAccessFile.close();
	    }
	}

    }

    /**
     * Gets the module libdir directory.
     * 
     * @return The module libdir.
     */
    public File getDirectory() {
	return new File(directory, directoryName);
    }

}
