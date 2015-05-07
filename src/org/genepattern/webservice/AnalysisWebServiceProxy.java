/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.webservice;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;

import org.apache.axis.AxisFault;
import org.apache.axis.client.Service;
import org.apache.axis.configuration.BasicClientConfig;
import org.apache.log4j.Logger;

/**
 * @author Joshua Gould
 */
public class AnalysisWebServiceProxy {
    private static final Logger log = Logger.getLogger(AnalysisWebServiceProxy.class);
    
    String endpoint = null;
    org.apache.axis.client.Service service = null;
    AnalysisSoapBindingStub stub;

    public AnalysisWebServiceProxy(String url, String userName, String password) throws WebServiceException {
        this(url, userName, password, true);
    }

    public AnalysisWebServiceProxy(String url, String userName, String password, boolean maintainSession)
    throws WebServiceException {
        try {
            this.endpoint = ProxyUtil.createEndpoint(url, "/services/Analysis");
            
            this.service = new Service(new BasicClientConfig());
            stub = new AnalysisSoapBindingStub(new URL(endpoint), service);
            stub.setUsername(userName);
            stub.setPassword(password);
            stub.setMaintainSession(maintainSession);
        } 
        catch (MalformedURLException e) {
            throw new Error(e);
        } 
        catch (AxisFault af) {
            throw new WebServiceException(af);
        }
    }

    public void setTimeout(int timeout) {
        stub.setTimeout(timeout);
    }

    public AnalysisSoapBindingStub getStub() {
        return stub;
    }

    public JobInfo recordClientJob(int taskID, ParameterInfo[] params) throws WebServiceException {
        try {
            return stub.recordClientJob(taskID, params);
        } catch (RemoteException re) {
            throw new WebServiceException(re);
        }
    }

    public JobInfo recordClientJob(int taskID, ParameterInfo[] params, int parentJobNumber) throws WebServiceException {
        try {
            return stub.recordClientJob(taskID, params, parentJobNumber);
        } catch (RemoteException re) {
            throw new WebServiceException(re);
        }
    }

    /**
     * Submits a job to be processed. The job is a child job of the supplied
     * parent job.
     *
     *
     * @param taskID
     *            The task id on the server.
     * @param parameters
     *            The array of parameters.
     * @param parentJobNumber
     *            The parent job number.
     * @return A <tt>JobInfo</tt> object.
     * @exception WebServiceException
     *                If an error occurs.
     */
    public JobInfo submitJob(int taskID, ParameterInfo[] parameters, int parentJobNumber) throws WebServiceException {
        try {
            HashMap<String, DataHandler> files = null;
            Set<String> dataHandlerNames = new HashSet<String>();
            // look for parameters of type file
            if (parameters != null) {
                for (ParameterInfo p : parameters) {
                    if (p.isInputFile()) {
                        String filename = p.getValue();
                        if (filename != null) {
                            String name = new File(filename).getName();
                            int counter = 1;
                            while (dataHandlerNames.contains(name)) {
                                int dotIndex = name.lastIndexOf('.');
                                if (dotIndex > 0) {
                                    String prefix = name.substring(0, dotIndex) + "-" + counter;
                                    String suffix = name.substring(dotIndex);
                                    name = prefix + suffix;
                                }
                            }
                            final String _name = name;
                            FileDataSource fds = new FileDataSource(filename) {
                                @Override
                                public String getName() {
                                    return _name;
                                }
                            };
                            DataHandler dataHandler = new DataHandler(fds);
                            if (files == null) {
                                files = new HashMap<String, DataHandler>();
                            }

                            // set value to name and not path
                            p.setValue(dataHandler.getName());
                            files.put(dataHandler.getName(), dataHandler);
                            dataHandlerNames.add(dataHandler.getName());
                        }
                    }
                }
            }

            JobInfo jobInfo = null;
            log.debug("submitJob, taskId="+taskID+", parentJobNumber="+parentJobNumber);
            if (parentJobNumber == -1) {
                jobInfo = stub.submitJob(taskID, parameters, files);
            }
            else {
                jobInfo = stub.submitJob(taskID, parameters, files, parentJobNumber);
            }
            log.debug("returned jobNumber: "+ (jobInfo == null ? "null" : jobInfo.getJobNumber()));
            return jobInfo;
        } 
        catch (RemoteException re) {
            throw new WebServiceException(re);
        }
    }

    /**
     * Submits a job to be processed.
     *
     * @param taskID
     *            The task id on the server.
     * @param parameters
     *            The array of parameters
     * @return A <tt>JobInfo</tt> object.
     * @exception WebServiceException
     *                If an error occurs.
     */
    public JobInfo submitJob(int taskID, ParameterInfo[] parameters) throws WebServiceException {
        return submitJob(taskID, parameters, -1);
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

    public void deleteJobResultFile(int jobId, String fileName) throws WebServiceException {
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

    public void purgeJob(int jobId) throws WebServiceException {
        try {
            stub.purgeJob(jobId);
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
     * Downloads the files that were generated by a completed job.
     *
     * @param jobID
     *            The job id.
     * @return The array of file names.
     * @exception WebServiceException
     *                If an error occurs.
     */
    public String[] getResultFiles(int jobID) throws WebServiceException {
        try {
            FileWrapper[] results = stub.getResultFiles(jobID);
            String[] filenames = null;
            if (results != null) {
                filenames = new String[results.length];
                for (int i = 0; i < results.length; i++) {
                    FileWrapper fh = results[i];
                    String newFilename = fh.getDataHandler().getName() + "_" + fh.getFilename();
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

    public File[] getResultFiles(int jobID, String[] resultFilenames, File downloadDirectory, boolean overwrite)
            throws WebServiceException {
        try {
            FileWrapper[] results = stub.getResultFiles(jobID, resultFilenames);
            File[] files = null;
            if (results != null) {
                files = new File[results.length];
                for (int i = 0; i < results.length; i++) {
                    FileWrapper fh = results[i];
                    if (fh == null) { // file doesn't exist on server
                        continue;
                    }

                    File axisFile = new File(fh.getDataHandler().getName());
                    String name = fh.getFilename();
                    File file = new File(downloadDirectory, name);

                    if (!overwrite && file.exists()) {
                        name = "job_" + jobID + "_" + name;
                        file = new File(downloadDirectory, name);
                        if (file.exists()) {
                            String suffix = null;
                            String prefix = name;
                            int dotIndex = name.lastIndexOf(".");
                            if (dotIndex != -1) {
                                prefix = name.substring(0, dotIndex);
                                suffix = name.substring(dotIndex, name.length());
                            }
                            try {
                                file = File.createTempFile(prefix, suffix, downloadDirectory);
                            } catch (IOException e) {
                                System.err.println("Unable to create temp file for " + name);
                            }
                        }
                    } else if (overwrite && file.exists()) {
                        file.delete();
                    }

                    axisFile.renameTo(file);
                    files[i] = file;
                }
            }

            return files;
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

    public JobInfo[] getJobs(String username, boolean getAll) throws WebServiceException {
        try {
            List<JobInfo> results = new ArrayList<JobInfo>();
            int maxJobNumber = -1;
            final int querySize = 100;
            int numRetrieved = querySize;
            while (numRetrieved == querySize) {
                JobInfo[] jobs = stub.getJobs(username, maxJobNumber, querySize, getAll);
                numRetrieved = jobs.length;
                results.addAll(Arrays.asList(jobs));
                if (jobs.length > 1) {
                    maxJobNumber = jobs[jobs.length - 1].getJobNumber() - 1;
                }
            }
            return results.toArray(new JobInfo[0]);
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

    public int[] getChildren(int jobNumber) throws WebServiceException {
        try {
            return stub.getChildren(jobNumber);
        } catch (RemoteException re) {
            throw new WebServiceException(re);
        }
    }

    public String createProvenancePipeline(JobInfo[] jobs, String pipelineName) throws WebServiceException {
        try {
            return stub.createProvenancePipeline(jobs, pipelineName);
        } catch (RemoteException re) {
            throw new WebServiceException(re);
        }

    }

    public String createProvenancePipeline(String fileUrlOrJobNumber, String pipelineName) throws WebServiceException {
        try {
            String lsid = stub.createProvenancePipeline(fileUrlOrJobNumber, pipelineName);
            return lsid;
        } catch (RemoteException re) {
            throw new WebServiceException(re);
        }

    }

    public JobInfo[] findJobsThatCreatedFile(String fileURLOrJobNumber) throws WebServiceException {
        try {
            return stub.findJobsThatCreatedFile(fileURLOrJobNumber);
        } catch (RemoteException re) {
            throw new WebServiceException(re);
        }

    }
}
