package org.genepattern.gpge.ui.project;

/**
 * Description of the Interface
 * 
 * @author Joshua Gould
 */
public interface ProjectDirectoryListener extends java.util.EventListener {

	public void projectAdded(ProjectEvent e);

	public void projectRemoved(ProjectEvent e);

}