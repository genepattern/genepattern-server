/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2006) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.

 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.client;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.genepattern.util.GPConstants;
import org.genepattern.webservice.AdminProxy;
import org.genepattern.webservice.AnalysisJob;
import org.genepattern.webservice.AnalysisWebServiceProxy;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.JobResult;
import org.genepattern.webservice.LocalTaskExecutor;
import org.genepattern.webservice.Parameter;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskExecutor;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.WebServiceException;

/**
 * This class is used to communicate with a GenePattern server.
 * 
 * @author Joshua Gould
 */
public class GPServer {
    protected final String server; // e.g. http://localhost:8080

    protected final String userName;

    /** LRU cache of tasks */
    protected Map cachedTasks;

    /** number of tasks to cache */
    protected final static int MAX_ENTRIES = 20;

    protected AdminProxy adminProxy;

    /**
     * Creates a new GPServer instance.
     * 
     * @param server
     *            The server, for example http://127.0.0.1:8080
     * @param userName
     *            The user name.
     * @exception WebServiceException
     *                If an error occurs while connecting to the server
     */
    public GPServer(String server, String userName) throws WebServiceException {
        this.server = server;
        this.userName = userName;
        this.cachedTasks = new LinkedHashMap(MAX_ENTRIES + 1, .75F, true) {
            public boolean removeEldestEntry(Map.Entry eldest) {
                return size() > MAX_ENTRIES;
            }
        };
        try {
            adminProxy = new AdminProxy(server, userName);
        } catch (Exception e) {
            throw new WebServiceException(e);
        }
    }

    private TaskInfo getTask(String lsid) throws WebServiceException {
        TaskInfo taskInfo = (TaskInfo) cachedTasks.get(lsid);
        if (taskInfo == null) {
            try {
                taskInfo = adminProxy.getTask(lsid);
            } catch (Exception e) {
                throw new WebServiceException(e);
            }
            if (taskInfo == null) {
                throw new WebServiceException(lsid + " not found on server.");
            }
            cachedTasks.put(lsid, taskInfo);
        }
        return taskInfo;
    }

    /**
     * submit a job based on a service and its parameters
     * 
     * @param parmInfos
     *            Description of the Parameter
     * @param handler
     *            Description of the Parameter
     * @param tinfo
     *            Description of the Parameter
     * @return Description of the Return Value
     * @exception org.genepattern.webservice.WebServiceException
     *                Description of the Exception
     */
    private AnalysisJob submitJob(AnalysisWebServiceProxy handler,
            TaskInfo tinfo, ParameterInfo[] parmInfos)
            throws org.genepattern.webservice.WebServiceException {
        final JobInfo job = handler.submitJob(tinfo.getID(), parmInfos);
        final AnalysisJob aJob = new AnalysisJob(server, job);
        return aJob;
    }

    /**
     * Wait for a job to end or error. This loop will wait for a max of 36
     * seconds for 10 tries doubling the wait time each time after 6 seconds to
     * a max of a 16 seconds wait
     * 
     * @param job
     *            Description of the Parameter
     * @param handler
     *            Description of the Parameter
     * @exception org.genepattern.webservice.WebServiceException
     *                Description of the Exception
     */
    private static void waitForErrorOrCompletion(
            AnalysisWebServiceProxy handler, AnalysisJob job)
            throws org.genepattern.webservice.WebServiceException {
        int maxtries = 20;
        int sleep = 1000;
        waitForErrorOrCompletion(handler, job, maxtries, sleep);
    }

    private static void waitForErrorOrCompletion(
            AnalysisWebServiceProxy handler, AnalysisJob job, int maxTries,
            int initialSleep)
            throws org.genepattern.webservice.WebServiceException {
        String status = "";
        JobInfo info = null;
        int count = 0;
        int sleep = initialSleep;
        while (!(status.equalsIgnoreCase("ERROR") || (status
                .equalsIgnoreCase("Finished")))) {
            count++;
            try {
                Thread.currentThread().sleep(sleep);
            } catch (InterruptedException ie) {
            }
            info = handler.checkStatus(job.getJobInfo().getJobNumber());
            job.setJobInfo(info);
            status = info.getStatus();
            sleep = incrementSleep(initialSleep, maxTries, count);
        }
    }

    /**
     * Returns the url to retrieve the given file as part of the given task.
     * 
     * @param taskNameOrLSID
     *            The task name or LSID of the task that contains the file. When
     *            an LSID is provided that does not include a version, the
     *            latest available version of the task identified by the LSID
     *            will be used. If a task name is supplied, the latest version
     *            of the task with the nearest authority is selected. The
     *            nearest authority is the first match in the sequence: local
     *            authority, Broad authority, other authority.
     * @param fileName
     *            The file name.
     * @return The url.
     */
    public URL getTaskFileURL(String taskNameOrLSID, String fileName) {
        try {
            return new URL(server + "/gp/getFile.jsp?task=" + taskNameOrLSID
                    + "&file=" + URLEncoder.encode(fileName, "UTF-8"));
        } catch (java.net.MalformedURLException x) {
            throw new Error(x);
        } catch (UnsupportedEncodingException x) {
            throw new Error(x);
        }
    }

    /**
     * Checks if the given job is complete.
     * 
     * @param jobNumber
     *            the job number
     * @return <tt>true</tt> if the job with the given job number is complete,
     *         <tt>false</tt> otherwise
     * @throws WebServiceException
     *             If an error occurs
     */
    public boolean isComplete(int jobNumber) throws WebServiceException {
        try {
            AnalysisWebServiceProxy analysisProxy = new AnalysisWebServiceProxy(
                    server, userName, false);
            analysisProxy.setTimeout(Integer.MAX_VALUE);
            JobInfo ji = analysisProxy.checkStatus(jobNumber);
            if (ji == null) {
                throw new WebServiceException("The job number " + jobNumber
                        + " was not found.");
            }
            return ji.getStatus().equalsIgnoreCase("finished")
                    || ji.getStatus().equalsIgnoreCase("error");
        } catch (Exception x) {
            throw new WebServiceException(x);
        }
    }

    /**
     * Creates a new <tt>JobResult</tt> instance for the given job number.
     * Invoke this method after the job is complete.
     * 
     * @param jobNumber
     *            the job number
     * @return <tt>JobResult</tt> instance
     * @throws WebServiceException
     *             If an error occurs
     * @see isComplete
     */
    public JobResult createJobResult(int jobNumber) throws WebServiceException {
        try {
            AnalysisWebServiceProxy analysisProxy = new AnalysisWebServiceProxy(
                    server, userName, false);
            analysisProxy.setTimeout(Integer.MAX_VALUE);
            JobInfo info = analysisProxy.checkStatus(jobNumber);

            String status = info.getStatus();
            if (!(status.equalsIgnoreCase("ERROR") || (status
                    .equalsIgnoreCase("Finished"))))
                return null;

            TaskInfo taskInfo = getTask(info.getTaskLSID());

            ArrayList resultFiles = new ArrayList();
            ParameterInfo[] jobParameterInfo = info.getParameterInfoArray();
            boolean stderr = false;
            boolean stdout = false;
            ArrayList jobParameters = new ArrayList();
            for (int j = 0; j < jobParameterInfo.length; j++) {
                if (jobParameterInfo[j].isOutputFile()) {
                    String fileName = jobParameterInfo[j].getValue();
                    int index1 = fileName.lastIndexOf('/');
                    int index2 = fileName.lastIndexOf('\\');
                    int index = (index1 > index2 ? index1 : index2);
                    if (index != -1) {
                        fileName = fileName.substring(index + 1, fileName
                                .length());
                    }
                    if (fileName.equals(GPConstants.STDOUT)) {
                        stdout = true;
                    } else if (fileName.equals(GPConstants.STDERR)) {
                        stderr = true;
                    } else {
                        resultFiles.add(fileName);
                    }
                } else {
                    jobParameters.add(new Parameter(jobParameterInfo[j]
                            .getName(), jobParameterInfo[j].getValue()));
                }
            }
            try {
                return new JobResult(new URL(server), info.getJobNumber(),
                        (String[]) resultFiles.toArray(new String[0]), stdout,
                        stderr, (Parameter[]) jobParameters
                                .toArray(new Parameter[0]), (String) taskInfo
                                .getTaskInfoAttributes().get(GPConstants.LSID));
            } catch (java.net.MalformedURLException mfe) {
                throw new Error(mfe);
            }

        } catch (Exception x) {
            throw new WebServiceException(x);
        }

    }

    /**
     * Submits the given task with the given parameters and does not wait for
     * the job to complete.
     * 
     * @param taskNameOrLSID
     *            The task name or LSID. When an LSID is provided that does not
     *            include a version, the latest available version of the task
     *            identified by the LSID will be used. If a task name is
     *            supplied, the latest version of the task with the nearest
     *            authority is selected. The nearest authority is the first
     *            match in the sequence: local authority, Broad authority, other
     *            authority.
     * @param parameters
     *            The parameters to run the task with.
     * @return The job number.
     * @see isComplete
     * @see createJobResult
     * @throws WebServiceException
     *             If an error occurs during the job submission process.
     */
    public int runAnalysisNoWait(String taskNameOrLSID, Parameter[] parameters)
            throws WebServiceException {
        try {
            TaskInfo taskInfo = getTask(taskNameOrLSID);
            ParameterInfo[] actualParameters = Util.createParameterInfoArray(
                    taskInfo, parameters);
            AnalysisWebServiceProxy analysisProxy = null;
            try {
                analysisProxy = new AnalysisWebServiceProxy(server, userName);
                analysisProxy.setTimeout(Integer.MAX_VALUE);
            } catch (Exception x) {
                throw new WebServiceException(x);
            }
            AnalysisJob job = submitJob(analysisProxy, taskInfo,
                    actualParameters);
            return job.getJobInfo().getJobNumber();
        } catch (org.genepattern.webservice.WebServiceException wse) {
            throw new WebServiceException(wse.getMessage(), wse.getRootCause());
        }
    }

    /**
     * Submits the given task with the given parameters and waits for the job to
     * complete.
     * 
     * @param taskNameOrLSID
     *            The task name or LSID. When an LSID is provided that does not
     *            include a version, the latest available version of the task
     *            identified by the LSID will be used. If a task name is
     *            supplied, the latest version of the task with the nearest
     *            authority is selected. The nearest authority is the first
     *            match in the sequence: local authority, Broad authority, other
     *            authority.
     * @param parameters
     *            The parameters to run the task with.
     * @return The job result.
     * @throws WebServiceException
     *             If an error occurs during the job submission or job result
     *             retrieval process.
     */
    public JobResult runAnalysis(String taskNameOrLSID, Parameter[] parameters)
            throws WebServiceException {
        try {
            TaskInfo taskInfo = getTask(taskNameOrLSID);
            ParameterInfo[] actualParameters = Util.createParameterInfoArray(
                    taskInfo, parameters);
            AnalysisWebServiceProxy analysisProxy = null;
            try {
                analysisProxy = new AnalysisWebServiceProxy(server, userName);
                analysisProxy.setTimeout(Integer.MAX_VALUE);
            } catch (Exception x) {
                throw new WebServiceException(x);
            }
            AnalysisJob job = submitJob(analysisProxy, taskInfo,
                    actualParameters);
            waitForErrorOrCompletion(analysisProxy, job);
            ArrayList resultFiles = new ArrayList();
            ParameterInfo[] jobParameterInfo = job.getJobInfo()
                    .getParameterInfoArray();
            boolean stderr = false;
            boolean stdout = false;
            ArrayList jobParameters = new ArrayList();
            for (int j = 0; j < jobParameterInfo.length; j++) {
                if (jobParameterInfo[j].isOutputFile()) {
                    String fileName = jobParameterInfo[j].getValue();
                    int index1 = fileName.lastIndexOf('/');
                    int index2 = fileName.lastIndexOf('\\');
                    int index = (index1 > index2 ? index1 : index2);
                    if (index != -1) {
                        fileName = fileName.substring(index + 1, fileName
                                .length());
                    }
                    if (fileName.equals(GPConstants.STDOUT)) {
                        stdout = true;
                    } else if (fileName.equals(GPConstants.STDERR)) {
                        stderr = true;
                    } else {
                        resultFiles.add(fileName);
                    }
                } else {
                    jobParameters.add(new Parameter(jobParameterInfo[j]
                            .getName(), jobParameterInfo[j].getValue()));
                }
            }
            try {
                return new JobResult(new URL(job.getServer()), job.getJobInfo()
                        .getJobNumber(), (String[]) resultFiles
                        .toArray(new String[0]), stdout, stderr,
                        (Parameter[]) jobParameters.toArray(new Parameter[0]),
                        (String) taskInfo.getTaskInfoAttributes().get(
                                GPConstants.LSID));
            } catch (java.net.MalformedURLException mfe) {
                throw new Error(mfe);
            }
        } catch (org.genepattern.webservice.WebServiceException wse) {
            throw new WebServiceException(wse.getMessage(), wse.getRootCause());
        }
    }

    /**
     * Downloads the support files for the given task from the server and
     * executes the given task locally.
     * 
     * @param taskNameOrLSID
     *            The task name or LSID. When an LSID is provided that does not
     *            include a version, the latest available version of the task
     *            identified by the LSID will be used. If a task name is
     *            supplied, the latest version of the task with the nearest
     *            authority is selected. The nearest authority is the first
     *            match in the sequence: local authority, Broad authority, other
     *            authority.
     * @param parameters
     *            The parameters to run the task with.
     * @throws WebServiceException
     *             If an error occurs while launching the visualizer.
     */
    public void runVisualizer(String taskNameOrLSID, Parameter[] parameters)
            throws WebServiceException {
        TaskInfo taskInfo = getTask(taskNameOrLSID);
        ParameterInfo[] actualParameters = Util.createParameterInfoArray(
                taskInfo, parameters);
        Map paramName2ValueMap = new HashMap();
        if (actualParameters != null) {
            for (int i = 0; i < actualParameters.length; i++) {
                paramName2ValueMap.put(actualParameters[i].getName(),
                        actualParameters[i].getValue());
            }
        }
        try {
            final TaskExecutor executor = new LocalTaskExecutor(taskInfo,
                    paramName2ValueMap, userName, server);
            new Thread() {
                public void run() {
                    try {
                        executor.exec();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }.start();
        } catch (Exception x) {
            throw new WebServiceException(x);
        }
    }

    /**
     * make the sleep time go up as it takes longer to exec. eg for 100 tries of
     * 1000ms (1 sec) first 20 are 1 sec each next 20 are 2 sec each next 20 are
     * 4 sec each next 20 are 8 sec each any beyond this are 16 sec each
     * 
     * @param init
     *            Description of the Parameter
     * @param maxTries
     *            Description of the Parameter
     * @param count
     *            Description of the Parameter
     * @return Description of the Return Value
     */
    private static int incrementSleep(int init, int maxTries, int count) {
        if (count < (maxTries * 0.2)) {
            return init;
        }
        if (count < (maxTries * 0.4)) {
            return init * 2;
        }
        if (count < (maxTries * 0.6)) {
            return init * 4;
        }
        if (count < (maxTries * 0.8)) {
            return init * 8;
        }
        return init * 16;
    }
}