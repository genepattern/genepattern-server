/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/


package org.genepattern.webservice;

import javax.activation.DataHandler;

/**
 * A utility class to help maintain filenames when files are passed as
 * attachements to web services.
 */
public class FileWrapper {
	private String filename;

	private DataHandler dataHandler;

	private long length;

	private long lastModified;

	public FileWrapper() {
	}

	public FileWrapper(String filename, DataHandler dh, long length,
			long lastModified) {
		this.filename = filename;
		this.dataHandler = dh;
		this.length = length;
		this.lastModified = lastModified;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public String getFilename() {
		return this.filename;
	}

	public void setDataHandler(DataHandler dh) {
		this.dataHandler = dh;
	}

	public DataHandler getDataHandler() {
		return this.dataHandler;
	}

	public long getLength() {
		return this.length;
	}

	public void setLength(long length) {
		this.length = length;
	}

	public long getLastModified() {
		return this.lastModified;
	}

	public void setLastModified(long lastModified) {
		this.lastModified = lastModified;
	}
}
