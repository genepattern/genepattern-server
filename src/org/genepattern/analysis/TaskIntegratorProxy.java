package org.genepattern.analysis;


import java.io.File;
import javax.activation.*;
import java.net.URL;

import java.rmi.RemoteException;

import java.util.Map;

import org.apache.axis.client.Service;

/**
 *@author    Joshua Gould
 */
public class TaskIntegratorProxy {
	String endpoint = null;
	org.apache.axis.client.Service service = null;
	TaskIntegratorSoapBindingStub stub;


	public TaskIntegratorProxy(String url, String userName)
			 throws java.net.MalformedURLException, org.apache.axis.AxisFault {
		this(url, userName, true);
	}


	public TaskIntegratorProxy(String url, String userName, boolean maintainSession)
			 throws java.net.MalformedURLException, org.apache.axis.AxisFault {
		this.endpoint = url;
		this.endpoint = this.endpoint + "/gp/services/TaskIntegrator";
		if(!(endpoint.startsWith("http://"))) {
			this.endpoint = "http://" + this.endpoint;
		}
		this.service = new Service();
		stub = new TaskIntegratorSoapBindingStub(new URL(endpoint), service);
		stub.setUsername(userName);
		stub.setMaintainSession(maintainSession);
	}


	public Map getServiceInfo()
			 throws WebServiceException, RemoteException {
		return stub.getServiceInfo();
	}


	public String importZipFromURL(String url, int privacy) throws WebServiceException, RemoteException {
		return stub.importZipFromURL(url, privacy);
	}


	public String importZip(File zipFile, int privacy)
			 throws WebServiceException, RemoteException {
		return stub.importZip(new DataHandler(new FileDataSource(zipFile)), privacy);
	}


	public void exportToZip(String taskName, File destinationFile)
			 throws WebServiceException, RemoteException {
		DataHandler dh = stub.exportToZip(taskName);
		copy(dh, destinationFile);
	}


	public String modifyTask(int accessId, String taskName, String description, ParameterInfo[] parameterInfoArray, java.util.HashMap taskAttributes, File[] files) throws WebServiceException, RemoteException {
		String[] fileNames = null;
		DataHandler[] dataHandlers = null;
		if(files != null) {
			dataHandlers = new DataHandler[files.length];
			fileNames = new String[files.length];
			for(int i = 0; i < files.length; i++) {
				dataHandlers[i] = new DataHandler(new FileDataSource(files[i]));

			}
		}
		return stub.modifyTask(accessId, taskName, description, parameterInfoArray, taskAttributes, dataHandlers, fileNames);
	}


	public String deleteFiles(String lsid, String[] fileNames) throws WebServiceException, RemoteException {
		return stub.deleteFiles(lsid, fileNames);
	}


	public void deleteTask(String lsid)
			 throws WebServiceException, RemoteException {
		stub.deleteTask(lsid);
	}




	public void getSupportFiles(String lsid, String[] fileNames, File destinationDirectory) throws WebServiceException, RemoteException {
		DataHandler[] dataHandlers = stub.getSupportFiles(lsid, fileNames);
		if(!destinationDirectory.exists()) {
			destinationDirectory.mkdirs();
		}
		for(int i = 0, length = dataHandlers.length; i < length; i++) {
			File destinationFile = new File(destinationDirectory, fileNames[i]);
			copy(dataHandlers[i], destinationFile);
		}
	}


	private void copy(DataHandler dh, File destinationFile) throws WebServiceException {
		File axisFile = new File(dh.getName());
		axisFile.renameTo(destinationFile);
		/*
		    java.io.FileOutputStream fos = null;
		    try {
		    fos = new java.io.FileOutputStream(destinationFile);
		    dh.writeTo(fos);
		    } catch(java.io.IOException e) {
		    throw new WebServiceException(e);
		    } finally {
		    try {
		    if(fos != null) {
		    fos.close();
		    }
		    } catch(java.io.IOException x) {
		    }
		    }
		 */
	}


   public String cloneTask(String lsid, String clonedTaskName) throws WebServiceException, RemoteException {
		return stub.cloneTask(lsid, clonedTaskName);
	}

   
	public long[] getLastModificationTimes(String lsid, String[] fileNames) throws WebServiceException, RemoteException {
		return stub.getLastModificationTimes(lsid, fileNames);
	}


	public String[] getSupportFileNames(String lsid) throws WebServiceException, RemoteException {
		return stub.getSupportFileNames(lsid);
	}
}
