package org.genepattern.server.webservice.server.local;

import java.io.File;
import java.io.IOException;
import javax.servlet.jsp.JspWriter;
import java.util.Vector;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;

import org.genepattern.server.webservice.server.ITaskIntegrator;
import org.genepattern.server.webservice.server.TaskIntegrator;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.WebServiceException;

/**
 * @author Joshua Gould
 */
public class LocalTaskIntegratorClient implements ITaskIntegrator {
	ITaskIntegrator service;
	String progressMessage = "";
	JspWriter out = null;

	public LocalTaskIntegratorClient(final String userName, final JspWriter out) {
		this.out = out;
		service = new TaskIntegrator() {
			
			protected String getUserName() {
				return userName;
			}
		};
	}

	public String modifyTask(int accessId, String taskName, String description,
			ParameterInfo[] parameterInfoArray, java.util.Map taskAttributes,
			javax.activation.DataHandler[] dataHandlers, String[] fileNames)
			throws WebServiceException {
		return service.modifyTask(accessId, taskName, description,
				parameterInfoArray, taskAttributes, dataHandlers, fileNames);
	}

	public String cloneTask(String lsid, String cloneName)
			throws WebServiceException {
		return service.cloneTask(lsid, cloneName);
	}

	public DataHandler[] getSupportFiles(String lsid)
			throws WebServiceException {
		return service.getSupportFiles(lsid);
	}

	public String[] getSupportFileNames(String lsid) throws WebServiceException {
		return service.getSupportFileNames(lsid);
	}

	public File[] getAllFiles(TaskInfo task) throws WebServiceException {
		String taskId = (String) task.getTaskInfoAttributes().get(
				GPConstants.LSID);
		if (taskId == null) {
			taskId = task.getName();
		}
		DataHandler[] dh = service.getSupportFiles(taskId);

		Vector vFiles = new Vector();
		for (int i = 0, length = dh.length; i < length; i++) {
			FileDataSource ds = (FileDataSource) dh[i].getDataSource();
			File file =  ds.getFile();
			if (!file.isDirectory()) vFiles.add(file);
		}
		File[] files = (File[])vFiles.toArray(new File[vFiles.size()]);
		
		return files;
	}

	public File[] getDocFiles(TaskInfo task) throws WebServiceException {
		String taskId = (String) task.getTaskInfoAttributes().get(
				GPConstants.LSID);
		if (taskId == null) {
			taskId = task.getName();
		}
		DataHandler[] dh = service.getDocFiles(taskId);
		File[] files = new File[dh.length];
		for (int i = 0, length = dh.length; i < length; i++) {
			FileDataSource ds = (FileDataSource) dh[i].getDataSource();
			files[i] = ds.getFile();
		}
		return files;
	}

	public void deleteTask(String lsid) throws WebServiceException {
		service.deleteTask(lsid);
	}

	public String deleteFiles(String lsid, String[] fileNames)
			throws WebServiceException {
		return service.deleteFiles(lsid, fileNames);
	}

	public String importZipFromURL(String url, int privacy)
			throws WebServiceException {
		return service.importZipFromURL(url, privacy, true, this);
	}

	public String importZipFromURL(String url, int privacy, boolean recursive)
			throws WebServiceException {
		return service.importZipFromURL(url, privacy, recursive, this);
	}
	
	public String importZipFromURL(String url, int privacy, boolean recursive, ITaskIntegrator taskIntegrator)
			throws WebServiceException {
		return service.importZipFromURL(url, privacy, recursive, taskIntegrator);
	}
	
	public String importZip(DataHandler handler, int privacy) throws WebServiceException {
		return service.importZip(handler, privacy);
	}
	
	public DataHandler exportToZip(String taskName) throws WebServiceException {
		return service.exportToZip(taskName, false);
	}

	public DataHandler exportToZip(String taskName, boolean recursive) throws WebServiceException {
		return service.exportToZip(taskName, recursive);
	}

	public long[] getLastModificationTimes(String lsid, String[] fileNames) throws WebServiceException {
		return service.getLastModificationTimes(lsid, fileNames);
	}

	public DataHandler[] getSupportFiles(String lsid, String[] fileNames)
			throws WebServiceException {
		return service.getSupportFiles(lsid, fileNames);
	}
	
	public DataHandler[] getDocFiles(String lsid) throws WebServiceException {
		return service.getDocFiles(lsid);
	}
	
	public boolean isZipOfZips(String url) throws WebServiceException {
		File file = null;
		java.io.OutputStream os = null;
		java.io.InputStream is = null;
		boolean deleteFile = false;
		try {

			if (url.startsWith("file://")) {
				String fileStr = url.substring(7, url.length());
				file = new File(fileStr);
			} else {
				deleteFile = true;
				file = File.createTempFile("gpz", ".zip");
				os = new java.io.FileOutputStream(file);
				is = new java.net.URL(url).openStream();
				byte[] buf = new byte[100000];
				int i;
				while ((i = is.read(buf, 0, buf.length)) > 0) {
					os.write(buf, 0, i);
				}
			}
		} catch (java.io.IOException ioe) {
			throw new WebServiceException(ioe);
		} finally {
			if (os != null) {
				try {
					os.close();
				} catch (java.io.IOException x) {
				}
			}
			if (is != null) {
				try {
					is.close();
				} catch (java.io.IOException x) {
				}
			}

		}
		try {
			return org.genepattern.server.TaskUtil.isZipOfZips(file);
		} catch (java.io.IOException ioe) {
			throw new WebServiceException(ioe);
		} finally {
			if (deleteFile && file != null) {
				file.delete();
			}
		}
	}

	public void statusMessage(String message) {
		System.out.println(message);
		try { 
			out.println(message + "<br>");
			flush();
		} catch (IOException ioe) {
			// ignore
		}
	}

	public void errorMessage(String message) {
		System.out.println(message);
		try { 
			out.println(message + "<br>");
			flush();
		} catch (IOException ioe) {
			// ignore
		}
	}

	public void beginProgress(String message) {
		progressMessage = message;
		try {
			out.print(message + " <span id=\""  + message.hashCode() + "\">0</span>% complete<br>");
			flush();
		} catch (IOException ioe) {
			// ignore
		}
	}

	public void continueProgress(int percentComplete) {
		System.out.print("\rcontinueProgress: " + progressMessage + " " + percentComplete + "% complete");
		try { 
			out.println("<script language=\"Javascript\">writeToLayer('" + progressMessage.hashCode() + "', " + percentComplete + ");</script>");
			//out.println(progressMessage + " " + percentComplete + "% complete<br>");
			flush();
		} catch (IOException ioe) {
			// ignore
		}
	}

	public void endProgress() {
		System.out.println("\r" + progressMessage + " complete     ");
	}

	protected void flush() throws IOException {
		for (int i = 0; i < 8*1024; i++) out.print(" ");
		out.println();
		out.flush();
	}
}