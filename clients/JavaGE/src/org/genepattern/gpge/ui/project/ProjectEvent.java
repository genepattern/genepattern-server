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