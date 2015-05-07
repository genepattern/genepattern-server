/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.server.webservice.server.local;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Vector;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.servlet.jsp.JspWriter;

import org.apache.log4j.Logger;
import org.genepattern.server.webservice.server.DirectoryManager;
import org.genepattern.server.webservice.server.Status;
import org.genepattern.server.webservice.server.TaskIntegrator;
import org.genepattern.server.webservice.server.dao.TaskIntegratorDAO;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.SuiteInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoCache;
import org.genepattern.webservice.WebServiceException;

/**
 * @author Joshua Gould
 * Extensions to TaskIntegraotr for use locally (as opposed to through the web service).
 */
public class LocalTaskIntegratorClient extends TaskIntegrator implements Status {

	private static Logger log = Logger.getLogger(LocalTaskIntegratorClient.class);

	String progressMessage = "";

	JspWriter out = null; // TODO this variable does not belong here

	String userName = null;

	public LocalTaskIntegratorClient(final String userName) {
		this.userName = userName;
	}

	public LocalTaskIntegratorClient(final String userName, final JspWriter out) {
		this.out = out;
		this.userName = userName;
	}

	protected String getUserName() {
		return userName;

	}

	/**
	 * Return all files for a given task. Used by addTask.jsp and viewTask.jsp.
	 * Gets the files from the super class an array of DataHandlers and converts
	 * them to an array of FileDataSources.
	 * 
	 * @param task
	 * @return array of files
	 * @throws WebServiceException
	 */
	public File[] getAllFiles(TaskInfo task) throws WebServiceException {
		String taskId = (String) task.getTaskInfoAttributes().get(GPConstants.LSID);
		if (taskId == null) {
			taskId = task.getName();
		}
		DataHandler[] dh = getSupportFiles(taskId);

		Vector vFiles = new Vector();
		for (int i = 0, length = dh.length; i < length; i++) {
			FileDataSource ds = (FileDataSource) dh[i].getDataSource();
			File file = ds.getFile();
			if (!file.isDirectory())
				vFiles.add(file);
		}
		File[] files = (File[]) vFiles.toArray(new File[vFiles.size()]);

		return files;
	}

    /**
     * Returns the docFiles for a given task. Gets the files from the super
     * class an array of DataHandlers and converts them to an array of
     * FileDataSources.
     * 
     * @param task
     * @return
     * @throws WebServiceException
     */
	public File[] getDocFiles(TaskInfo task) throws WebServiceException {
	    DataHandler[] dh = getDocFiles(task.getID(), task.getLsid());
	    File[] files = new File[dh.length];
	    for (int i = 0, length = dh.length; i < length; i++) {
	        FileDataSource ds = (FileDataSource) dh[i].getDataSource();
	        files[i] = ds.getFile();
	    }
	    return files;
	}
	
    /**
     * Gets the files that belong to the given task or suite that are considered to be documentation files. Returned as
     * an array of DataHandlers.
     * 
     * @param lsid
     *                The LSID
     * @return The docFiles
     * @exception WebServiceException
     *                    If an error occurs
     */
    private DataHandler[] getDocFiles(Integer taskId, String lsid) throws WebServiceException {
        List<String> docFilenames = TaskInfoCache.instance().getDocFilenames(taskId, lsid);
        if (docFilenames==null) {
            log.error("Unexpected null list in getDocFilenames(taskId="+taskId+", lsid="+lsid+")");
            return new DataHandler[0];
        }
        if (docFilenames.size()==0) {
            return new DataHandler[0];
        }

        File taskLibDir = null;
        try {
            String libDir = DirectoryManager.getLibDir(lsid);
            taskLibDir = new File(libDir);
        } 
        catch (Exception e) {
            log.error(e);
            throw new WebServiceException(e);
        }
        
        DataHandler[] dh = new DataHandler[docFilenames.size()];
        int i=0;
        for(String docFilename : docFilenames) {
            File f = new File(taskLibDir, docFilename);
            dh[i] = new DataHandler(new FileDataSource(f));
            ++i;
        }
        return dh;
    }


	/**
	 * Create or update a suite object.  This method does not update files.  Used
	 * by the web interface,  files are handled in the web layer using the 
	 * commons file upload packge.
	 * 
	 * @param suiteInfo
	 * @return
	 * @throws WebServiceException
	 */
	public void saveOrUpdateSuite(SuiteInfo suiteInfo) throws WebServiceException {

		isAuthorized(getUserName(), "TaskIntegrator.installSuite");

		if (suiteInfo.getLSID() != null) {
			if (suiteInfo.getLSID().trim().length() == 0)
				suiteInfo.setLSID(null);
		}

		(new TaskIntegratorDAO()).saveOrUpdate(suiteInfo);
	}

	/**
	 * Print a message to the jsp output stream
	 */
	public void statusMessage(String message) {
		System.out.println(message);
		try {
			if (out != null) {
				out.println(message + "<br>");
				flush();
			}
		} catch (IOException ioe) {
			// ignore
		}
	}

	public void beginProgress(String message) {
		progressMessage = message;
		try {
			if (out != null) {
				out.print(message + " <span id=\"" + message.hashCode() + "\">0</span>% complete<br>");
				flush();
			}
		} catch (IOException ioe) {
			// ignore
		}
	}

	public void continueProgress(int percentComplete) {
		System.out.print("\rcontinueProgress: " + progressMessage + " " + percentComplete + "% complete");
		try {
			if (out != null) {
				out.println("<script language=\"Javascript\">writeToLayer('" + progressMessage.hashCode() + "', "
						+ percentComplete + ");</script>");
				// out.println(progressMessage + " " + percentComplete + "%
				// complete<br>");
				flush();
			}
		} catch (IOException ioe) {
			// ignore
		}
	}

	public void endProgress() {
		System.out.println("\r" + progressMessage + " complete     ");
	}

	protected void flush() throws IOException {
		if (out != null) {
			for (int i = 0; i < 8 * 1024; i++)
				out.print(" ");
			out.println();
			out.flush();
		}
	}

}
