/*
 * Created on Jun 19, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package edu.mit.broad.gp.gpge.views.data.nodes;

import java.io.File;
import java.io.FileFilter;


class FilesOnlyFileFilter implements FileFilter {
	static FilesOnlyFileFilter FILE_FILTER = new FilesOnlyFileFilter();

	
	public boolean accept(File f) {
		return !f.isDirectory() && !f.isHidden() && !f.getName().startsWith(".") && !f.getName().endsWith("~");
	}

}