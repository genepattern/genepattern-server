package org.genepattern.webservice;

import java.io.File;
import java.net.URL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import javax.activation.*;
import java.rmi.RemoteException;
import org.apache.axis.client.Service;

/**
 * @author Joshua Gould
 */
public class AnalysisWebServiceProxy {
	String endpoint = null;

	org.apache.axis.client.Service service = null;

	AnalysisSoapBindingStub stub;

	public AnalysisWebServiceProxy(String url, String userName)
			throws WebServiceException {
		this(url, userName, true);
	}

	public AnalysisWebServiceProxy(String url, String userName,
			boolean maintainSession) throws WebServiceException {
     try {
         this.endpoint = url;
         this.endpoint = this.endpoint + "/gp/services/Analysis";
         if (!(endpoint.startsWith("http://"))) {
            this.endpoint = "http://" + this.endpoint;
         }
         this.service = new Service();
         stub = new AnalysisSoapBindingStub(new URL(endpoint), service);
         stub.setUsername(userName);
         stub.setMaintainSession(maintainSession);
      } catch(org.apache.axis.AxisFault af) {
         throw new WebServiceException(af);  
      } catch(java.net.MalformedURLException me) {
         throw new Error(me);  
      }
	}
   

   /**
	 * Submits a job to be processed. The job is a child job of the supplied parent job.
	 * 
	 * @param taskID
	 *            Description of the Parameter
	 * @param parameters
	 *            Description of the Parameter
	 * @return Description of the Return Value
	 * @exception WebServiceException
	 *                Description of the Exception
	 */
	public JobInfo submitJob(int taskID, ParameterInfo[] parameters, int parentJobNumber)
			throws WebServiceException {
		try {
			HashMap files = null;
			// loop through parameters array looking for any parm of type FILE
			for (int x = 0; x < parameters.length; x++) {
				final ParameterInfo param = parameters[x];
				if (parameters[x].isInputFile()) {
					String filename = parameters[x].getValue();
					DataHandler dataHandler = new DataHandler(
							new FileDataSource(filename));
					if (filename != null) {
						if (files == null) {
							files = new HashMap();
						}
						parameters[x].setValue(dataHandler.getName());// pass
																	  // only
																	  // name &
																	  // not
																	  // path
						files.put(dataHandler.getName(), dataHandler);
					}
				}
			}

			return stub.submitJob(taskID, parameters, files, parentJobNumber);
		} catch (RemoteException re) {
			throw new WebServiceException(re);
		}
	}
   
   
            
	/**
	 * Submits a job to be processed.
	 * 
	 * @param taskID
	 *            Description of the Parameter
	 * @param parameters
	 *            Description of the Parameter
	 * @return Description of the Return Value
	 * @exception WebServiceException
	 *                Description of the Exception
	 */
	public JobInfo submitJob(int taskID, ParameterInfo[] parameters)
			throws WebServiceException {
		try {
			HashMap files = null;
			// loop through parameters array looking for any parm of type FILE
			for (int x = 0; x < parameters.length; x++) {
				final ParameterInfo param = parameters[x];
				if (parameters[x].isInputFile()) {
					String filename = parameters[x].getValue();
					DataHandler dataHandler = new DataHandler(
							new FileDataSource(filename));
					if (filename != null) {
						if (files == null) {
							files = new HashMap();
						}
						parameters[x].setValue(dataHandler.getName());// pass
																	  // only
																	  // name &
																	  // not
																	  // path
						files.put(dataHandler.getName(), dataHandler);
					}
				}
			}

			return stub.submitJob(taskID, parameters, files);
		} catch (RemoteException re) {
			throw new WebServiceException(re);
		}
	}

	/**
	 * Checks the status of a job.
	 * 
	 * @param jobID
	 *            Description of the Parameter
	 * @return Description of the Return Value
	 * @exception WebServiceException
	 *                Description of the Exception
	 */
	public JobInfo checkStatus(int jobID) throws WebServiceException {
		try {
			return stub.checkStatus(jobID);
		} catch (RemoteException re) {
			throw new WebServiceException(re);
		}
	}

	public void deleteJobResultFile(int jobId, String fileName)
			throws WebServiceException {
		try {
			stub.deleteJobResultFile(jobId, fileName);
		} catch (RemoteException re) {
			throw new WebServiceException(re);
		}
	}

	public void deleteJob(int jobId) throws WebServiceException {
		try {
			stub.deleteJob(jobId);
		} catch (RemoteException re) {
			throw new WebServiceException(re);
		}
	}

	/**
	 * Pings the service to see if it's alive.
	 * 
	 * @return Description of the Return Value
	 * @exception WebServiceException
	 *                Description of the Exception
	 */
	public String ping() throws WebServiceException {
		try {
			return stub.ping();
		} catch (RemoteException re) {
			throw new WebServiceException(re);
		}
	}

	/**
	 * Returns all the available tasks that can be used.
	 * 
	 * @return The tasks value
	 * @exception WebServiceException
	 *                Description of the Exception
	 */
	public TaskInfo[] getTasks() throws WebServiceException {
		try {
			return stub.getTasks();
		} catch (RemoteException re) {
			throw new WebServiceException(re);
		}
	}

	/**
	 * Returns the filenames that were generated by a completed job.
	 * 
	 * @param jobID
	 *            Description of the Parameter
	 * @return The resultFiles value
	 * @exception WebServiceException
	 *                Description of the Exception
	 */
	public String[] getResultFiles(int jobID) throws WebServiceException {
		try {
			Object[] results = stub.getResultFiles(jobID);
			String[] filenames = null;
			if (results != null) {
				filenames = new String[results.length];
				for (int i = 0; i < results.length; i++) {
					FileWrapper fh = (FileWrapper) results[i];
					String newFilename = fh.getDataHandler().getName() + "_"
							+ fh.getFilename();
					File f = new File(fh.getDataHandler().getName());
					f.renameTo(new File(newFilename));
					filenames[i] = newFilename;
				}
			}

			return filenames;
		} catch (RemoteException re) {
			throw new WebServiceException(re);
		}
	}
   
   public void terminateJob(int jobId) throws WebServiceException {
      try {
         stub.terminateJob(jobId);
		} catch (RemoteException re) {
			throw new WebServiceException(re);
		}
   }

	public JobInfo[] getJobs() throws WebServiceException {
		try {
			java.util.List results = new java.util.ArrayList();
         int startIndex = 0;
         final int querySize = 100;
         int numRetrieved = querySize;
         while(numRetrieved==querySize) {
            JobInfo[] jobs = stub.getJobs(null, startIndex, querySize, false); 
            numRetrieved = jobs.length;
            java.util.List t = java.util.Arrays.asList(jobs); 
            results.addAll(t);
            startIndex+=querySize;
         }
         return (JobInfo[]) results.toArray(new JobInfo[0]);
		} catch (RemoteException re) {
			throw new WebServiceException(re);
		}
	}
   
   public void setJobStatus(int parentJobId, String status) throws WebServiceException {
		try {
         stub.setJobStatus(parentJobId, status);
		} catch (RemoteException re) {
			throw new WebServiceException(re);
		}
	}
}