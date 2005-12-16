/*
  The Broad Institute
  SOFTWARE COPYRIGHT NOTICE AGREEMENT
  This software and its documentation are copyright (2003-2006) by the
  Broad Institute/Massachusetts Institute of Technology. All rights are
  reserved.

  This software is supplied without any warranty or guaranteed support
  whatsoever. Neither the Broad Institute nor MIT can be responsible for its
  use, misuse, or functionality.
*/



package edu.mit.broad.gp.gpge.views.data.nodes;

import java.io.File;
import java.io.FileFilter;


class HiddenFileFilter implements FileFilter {
	static HiddenFileFilter FILE_FILTER = new HiddenFileFilter();

	public boolean accept(File f) {
		return !f.isHidden() && !f.getName().startsWith(".") && !f.getName().endsWith("~");
	}

}