package org.genepattern.gpge.ui.tasks;

/**
 * Description of the Interface
 * 
 * @author Joshua Gould
 */
public interface JobListener extends java.util.EventListener {

	public void jobStatusChanged(JobEvent e);

	public void jobCompleted(JobEvent e);

	public void jobAdded(JobEvent e);
}