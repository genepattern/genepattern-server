package org.genepattern.webservice;

import java.io.File;
import javax.activation.*;
import java.net.URL;

import java.rmi.RemoteException;

import java.util.Map;

import org.apache.axis.client.Service;

/**
 * @author Joshua Gould
 */
public class TaskIntegratorProxy {
	String endpoint = null;

	org.apache.axis.client.Service service = null;

	TaskIntegratorSoapBindingStub stub;

	public TaskIntegratorProxy(String url, String userName)
			throws WebServiceException {
		this(url, userName, true);
	}

	public TaskIntegratorProxy(String url, String userName,
			boolean maintainSession) throws WebServiceException {
      try {
         this.endpoint = url;
         this.endpoint = this.endpoint + "/gp/services/TaskIntegrator";
         if (!(endpoint.startsWith("http://"))) {
            this.endpoint = "http://" + this.endpoint;
         }
         this.service = new Service();
         stub = new TaskIntegratorSoapBindingStub(new URL(endpoint), service);
         stub.setUsername(userName);
         stub.setMaintainSession(maintainSession);
      } catch(java.net.MalformedURLException mue) {
         throw new WebServiceException(mue);
      } catch(org.apache.axis.AxisFault af) {
         throw new WebServiceException(af);
      }
	}

	public Map getServiceInfo() throws WebServiceException {
      try {
         return stub.getServiceInfo();
      } catch(RemoteException re) {
         throw new WebServiceException(re);  
      }
	}

	public String importZipFromURL(String url, int privacy)
			throws WebServiceException {
      try {
         return stub.importZipFromURL(url, privacy);
      } catch(RemoteException re) {
         throw new WebServiceException(re);  
      }
	}

	public String importZip(File zipFile, int privacy)
			throws WebServiceException {
      try {
         return stub.importZip(new DataHandler(new FileDataSource(zipFile)),
				privacy);
      } catch(RemoteException re) {
         throw new WebServiceException(re);  
      }
	}

	public void exportToZip(String taskName, File destinationFile)
			throws WebServiceException {
      try {
         DataHandler dh = stub.exportToZip(taskName);
         copy(dh, destinationFile);
      } catch(RemoteException re) {
         throw new WebServiceException(re);  
      }
	}

	public String modifyTask(int accessId, String taskName, String description,
			ParameterInfo[] parameterInfoArray,
			java.util.HashMap taskAttributes, File[] files)
			throws WebServiceException {
      try {
         String[] fileNames = null;
         DataHandler[] dataHandlers = null;
         if (files != null) {
            dataHandlers = new DataHandler[files.length];
            fileNames = new String[files.length];
            for (int i = 0; i < files.length; i++) {
               dataHandlers[i] = new DataHandler(new FileDataSource(files[i]));
   
            }
         }
         return stub.modifyTask(accessId, taskName, description,
				parameterInfoArray, taskAttributes, dataHandlers, fileNames);
      } catch(RemoteException re) {
         throw new WebServiceException(re);  
      }
	}

	public String deleteFiles(String lsid, String[] fileNames)
			throws WebServiceException {
      try {
         return stub.deleteFiles(lsid, fileNames);
      } catch(RemoteException re) {
         throw new WebServiceException(re);  
      }
	}

	public void deleteTask(String lsid) throws WebServiceException {
		try {
         stub.deleteTask(lsid);
      } catch(RemoteException re) {
         throw new WebServiceException(re);  
      }
	}

	public void getSupportFiles(String lsid, String[] fileNames,
			File destinationDirectory) throws WebServiceException {
      try {
         DataHandler[] dataHandlers = stub.getSupportFiles(lsid, fileNames);
         if (!destinationDirectory.exists()) {
            destinationDirectory.mkdirs();
         }
         for (int i = 0, length = dataHandlers.length; i < length; i++) {
            File destinationFile = new File(destinationDirectory, fileNames[i]);
            copy(dataHandlers[i], destinationFile);
         }
      } catch(RemoteException re) {
         throw new WebServiceException(re);  
      }
	}

	private void copy(DataHandler dh, File destinationFile)
			throws WebServiceException {
		File axisFile = new File(dh.getName());
		axisFile.renameTo(destinationFile);
		/*
		 * java.io.FileOutputStream fos = null; try { fos = new
		 * java.io.FileOutputStream(destinationFile); dh.writeTo(fos); }
		 * catch(java.io.IOException e) { throw new WebServiceException(e); }
		 * finally { try { if(fos != null) { fos.close(); } }
		 * catch(java.io.IOException x) { } }
		 */
	}

	public String cloneTask(String lsid, String clonedTaskName)
			throws WebServiceException {
      try {
         return stub.cloneTask(lsid, clonedTaskName);
      } catch(RemoteException re) {
         throw new WebServiceException(re);  
      }
	}

	public long[] getLastModificationTimes(String lsid, String[] fileNames)
			throws WebServiceException {
      try{
         return stub.getLastModificationTimes(lsid, fileNames);
      } catch(RemoteException re) {
         throw new WebServiceException(re);  
      }
	}

	public String[] getSupportFileNames(String lsid)
			throws WebServiceException {
      try {
         return stub.getSupportFileNames(lsid);
      } catch(RemoteException re) {
         throw new WebServiceException(re);  
      }
	}
}