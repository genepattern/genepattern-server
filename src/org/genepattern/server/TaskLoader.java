package org.genepattern.server;

/**
 * Implementation of TaskLoaderMBean and jboss serviceMBean
 * 
 * @author Rajesh Kuttan
 * @version 1.0
 */

public class TaskLoader {

	private String jndiName = "TaskLoader";

	private AnalysisManager analysisManager = AnalysisManager.getInstance();

	/**
	 * MBean client can call this method to reload, omnigene properties to JNDI
	 * 
	 * @throws OmnigeneException
	 *             <CODE>OmnigeneException</CODE>
	 */
	public AnalysisManager getAnalysisManager() {
		return analysisManager;
	}

	/**
	 * This function defined in <CODE>ServiceMBean</CODE> interface, to get
	 * mbean name
	 * 
	 * @return MBean Name
	 */
	public String getName() {
		return jndiName;
	}

	/**
	 * Define in <CODE>ServiceMBean</CODE> and invoked when MBean is started.
	 * 
	 * @throws Exception
	 *             Exception
	 */
	public void startService() throws Exception {
		try {
			analysisManager.startAllAnalysisTask();
		} catch (Exception analysisEx) {
			analysisEx.printStackTrace();
			throw new Exception(analysisEx.getMessage());
		}
	}

	/**
	 * Define in <CODE>ServiceMBean</CODE> and invoked before MBean is stoped.
	 */
	public void stopService() {
		try {
			analysisManager.stopAllAnalysisTask();
		} catch (Exception analysisEx) {
			analysisEx.printStackTrace();
		}
	}

}