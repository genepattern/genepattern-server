
package edu.mit.broad.gp.gpge.views.data.nodes;

import java.io.File;
import java.io.FileFilter;


class HiddenFileFilter implements FileFilter {
	static HiddenFileFilter FILE_FILTER = new HiddenFileFilter();

	public boolean accept(File f) {
		return !f.isHidden() && !f.getName().startsWith(".") && !f.getName().endsWith("~");
	}

}