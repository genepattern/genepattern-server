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


package org.genepattern.gpge.ui.project;

import org.genepattern.webservice.AnalysisJob;
import java.io.File;

public class ProjectEvent extends java.util.EventObject {
	private File directory;

	public ProjectEvent(Object source, File directory) {
		super(source);
		this.directory = directory;
	}

	public File getDirectory() {
		return directory;
	}
}