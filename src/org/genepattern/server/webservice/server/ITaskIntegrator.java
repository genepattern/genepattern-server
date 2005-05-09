package org.genepattern.server.webservice.server;

import javax.activation.DataHandler;

import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.WebServiceException;

/**
 * @author Joshua Gould
 */
public interface ITaskIntegrator {

	/**
	 * Deletes the given task
	 * 
	 * @param lsid
	 *            The LSID
	 * @exception WebServiceException
	 *                If an error occurs
	 */
	public void deleteTask(String lsid) throws WebServiceException;

	/**
	 * Deletes the given files that belong to the given task
	 * 
	 * @param lsid
	 *            The LSID
	 * @param fileNames
	 *            Description of the Parameter
	 * @return The LSID of the new task
	 * @exception WebServiceException
	 *                If an error occurs
	 */
	public String deleteFiles(String lsid, String[] fileNames)
			throws WebServiceException;

	/**
	 * Installs the zip file at the given url overwriting anything already
	 * there.
	 * 
	 * @param url
	 *            The url of the zip file
	 * @param privacy
	 *            One of GPConstants.ACCESS_PUBLIC or GPConstants.ACCESS_PRIVATE
	 * @return The LSID of the task
	 * @throws WebServiceException
	 *             If an error occurs
	 */
	public String importZipFromURL(String url, int privacy)
			throws WebServiceException;

	/**
	 * Installs the zip file at the given url overwriting anything already
	 * there.
	 * 
	 * @param url
	 *            The url of the zip file
	 * @param privacy
	 *            One of GPConstants.ACCESS_PUBLIC or GPConstants.ACCESS_PRIVATE
	 * @param recursive
	 *            Installs dependent tasks from same zip file (zip of zips)
	 * @return The LSID of the task
	 * @throws WebServiceException
	 *             If an error occurs
	 */
	public String importZipFromURL(String url, int privacy, boolean recursive)
			throws WebServiceException;

	/**
	 * Installs the zip file overwriting anything already there.
	 * 
	 * @param dataHandler
	 *            The zip file
	 * @param privacy
	 *            One of GPConstants.ACCESS_PUBLIC or GPConstants.ACCESS_PRIVATE
	 * @return The LSID of the task
	 * @throws WebServiceException
	 *             If an error occurs
	 */
	public String importZip(DataHandler dataHandler, int privacy)
			throws WebServiceException;

	/**
	 * Modifies the task with the given name. If the task does not exist, it
	 * will be created.
	 * 
	 * @param accessId
	 *            One of GPConstants.ACCESS_PUBLIC or GPConstants.ACCESS_PRIVATE
	 * @param taskName
	 *            The name of the task
	 * @param description
	 *            The description
	 * @param parameterInfoArray
	 *            The input parameters
	 * @param taskAttributes
	 *            Attributes that go in the task manifest file
	 * @param dataHandlers
	 *            Holds the uploaded files
	 * @param fileNames
	 *            The names of the files on the client
	 * @return The LSID of the task
	 * @exception WebServiceException
	 *                If an error occurs
	 */
	public String modifyTask(int accessId, String taskName, String description,
			ParameterInfo[] parameterInfoArray, java.util.Map taskAttributes,
			javax.activation.DataHandler[] dataHandlers, String[] fileNames)
			throws WebServiceException;

	/**
	 * Clones the given task.
	 * 
	 * @param lsid
	 *            The lsid of the task to clone
	 * @param cloneName
	 *            The name of the cloned task
	 * @return The LSID of the cloned task
	 * @exception WebServiceException
	 *                If an error occurs
	 */
	public String cloneTask(String lsid, String cloneName)
			throws WebServiceException;

	/**
	 * Gets the files that belong to the given task that are considered to be
	 * documentation files
	 * 
	 * @param lsid
	 *            The LSID
	 * @return The docFiles
	 * @exception WebServiceException
	 *                If an error occurs
	 */
	public javax.activation.DataHandler[] getDocFiles(String lsid)
			throws WebServiceException;

	/**
	 * Gets the an array of file names that belong to the given task
	 * 
	 * @param lsid
	 *            The LSID
	 * @return The supportFileNames
	 * @exception WebServiceException
	 *                If an error occurs
	 */
	public String[] getSupportFileNames(String lsid) throws WebServiceException;

	/**
	 * Gets the an array of files that belong to the given task
	 * 
	 * @param lsid
	 *            The LSID
	 * @return The supportFiles
	 * @exception WebServiceException
	 *                If an error occurs
	 */
	public javax.activation.DataHandler[] getSupportFiles(String lsid)
			throws WebServiceException;

	/**
	 * Gets the an array of the given files that belong to the given task
	 * 
	 * @param lsid
	 *            The LSID
	 * @param fileNames
	 *            The fileNames
	 * @return The files
	 * @exception WebServiceException
	 *                If an error occurs
	 */
	public javax.activation.DataHandler[] getSupportFiles(String lsid,
			String[] fileNames) throws WebServiceException;

	/**
	 * Gets the an array of the last mofification times of the given files that
	 * belong to the given task
	 * 
	 * @param lsid
	 *            The LSID
	 * @param fileNames
	 *            The fileNames
	 * @return The last modification times
	 * @exception WebServiceException
	 *                If an error occurs
	 */
	public long[] getLastModificationTimes(String lsid, String[] fileNames)
			throws WebServiceException;

	/**
	 * Exports the given task to a zip file
	 * 
	 * @param lsid
	 *            The LSID
	 * @return The zip file
	 * @exception WebServiceException
	 *                If an error occurs
	 */
	public javax.activation.DataHandler exportToZip(String lsid)
			throws WebServiceException;
			
	public void statusMessage(String message);
	public void errorMessage(String message);
	public void beginProgress(String message);
	public void continueProgress(int percentComplete);
	public void endProgress(String message);
}