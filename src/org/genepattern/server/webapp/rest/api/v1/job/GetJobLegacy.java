package org.genepattern.server.webapp.rest.api.v1.job;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.dm.jobresult.JobResultFile;
import org.genepattern.server.domain.JobStatus;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class GetJobLegacy implements GetJob {
    final static private Logger log = Logger.getLogger(GetJobLegacy.class);
    /**
     * helper method which indicates if the job has completed processing.
     */
    private static boolean isFinished(final JobInfo jobInfo) {
        if ( JobStatus.FINISHED.equals(jobInfo.getStatus()) ||
                JobStatus.ERROR.equals(jobInfo.getStatus()) ) {
            return true;
        }
        return false;        
    }
    
    private static boolean hasError(final JobInfo jobInfo) {
        return JobStatus.ERROR.equals(jobInfo.getStatus());
    }
    
    private static List<ParameterInfo> getOutputFiles(final JobInfo jobInfo) {
        List<ParameterInfo> outputs=new ArrayList<ParameterInfo>();
        for(final ParameterInfo pinfo : jobInfo.getParameterInfoArray()) {
            if (pinfo.isOutputFile()) {
                outputs.add(pinfo);
            }
        }
        return outputs;
    }
    
    private static boolean isExecutionLog(final ParameterInfo param) {
        boolean isExecutionLog = (
                param.getName().equals(GPConstants.TASKLOG) || 
                param.getName().endsWith(GPConstants.PIPELINE_TASKLOG_ENDING));
        return isExecutionLog;
    }
    
    private static URL getStderrLocation(final JobInfo jobInfo) throws Exception {
        for(ParameterInfo pinfo : jobInfo.getParameterInfoArray()) {
            if (pinfo._isStderrFile()) {
                //construct URI to the file
                //Hint: the name of the parameter is the name of the file (e.g. name=stderr.txt)
                //      the value of the parameter includes the jobId (e.g. 2137/stderr.txt)
                String name=pinfo.getName();
                JobResultFile stderr=new JobResultFile(jobInfo, new File(name));
                return stderr.getUrl();
            }
        }
        return null;
    }
    
    private static GpFilePath getOutputFile(final JobInfo jobInfo, final ParameterInfo pinfo) {
        if (pinfo.isOutputFile()) {
            String name=pinfo.getName();
            try {
                JobResultFile outputFile=new JobResultFile(jobInfo, new File(name));
                return outputFile;
            }
            catch (Throwable t) {
                log.error(t);
            }
        }
        return null;
    }
    
    private static JSONObject initOutputFile(final GpFilePath gpFilePath) throws Exception {
        //create a JSON representation of a file
        JSONObject o = new JSONObject();
        JSONObject link = new JSONObject();
        link.put("href", gpFilePath.getUrl().toExternalForm());
        link.put("name", gpFilePath.getName());        
        o.put("link", link);
        o.put("fileLength", gpFilePath.getFileLength());
        o.put("lastModified", gpFilePath.getLastModified().getTime());
        return o;
    }
    
    public JSONObject getJob(final ServerConfiguration.Context userContext, final String jobId) 
    throws GetJobException
    {
        if (userContext==null) {
            throw new IllegalArgumentException("userContext==null");
        }
        if (userContext.getUserId()==null || userContext.getUserId().length() == 0) {
            throw new IllegalArgumentException("userContext.userId not set");
        }
        
        JobInfo jobInfo=null;
        //expecting jobId to be an integer
        int jobNo;
        try {
            jobNo=Integer.parseInt(jobId);
        }
        catch (NumberFormatException e) {
            throw new GetJobException("Expecting an integer value for jobId="+jobId+" :"
                    +e.getLocalizedMessage());
        }
        if (jobNo<0) {
            throw new GetJobException("Invalid jobNo="+jobNo+" : Can't be less than 0.");
        }
        final boolean isInTransaction=HibernateUtil.isInTransaction();
        try {
            AnalysisDAO analysisDao = new AnalysisDAO();
            jobInfo = analysisDao.getJobInfo(jobNo);
        }
        catch (Throwable t) {
            log.error("Error initializing jobInfo for jobId="+jobId, t);
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
        
        //manually create a JSONObject representing the job
        final JSONObject job = initJsonObject(jobInfo);
        return job;
    }

    /**
     * Create a JSONObject representing the job
     * @param jobInfo
     * @return
     */
    private JSONObject initJsonObject(final JobInfo jobInfo) throws GetJobException {
        final JSONObject job = new JSONObject();
        try {
            job.put("jobId", ""+jobInfo.getJobNumber());
            
            //init jobStatus
            final JSONObject jobStatus = new JSONObject();
            final boolean isFinished=isFinished(jobInfo);
            jobStatus.put("isFinished", isFinished);
            final boolean hasError=hasError(jobInfo);
            jobStatus.put("hasError", hasError);
            URL stderr=null;
            try {
                stderr=getStderrLocation(jobInfo);
            }
            catch (Throwable t) {
                log.error("Error getting stderr file for jobId="+jobInfo.getJobNumber(), t);
            }
            if (stderr != null) {
                //TODO: come up with a standard JSON representation of a gp job result file
                jobStatus.put("stderrLocation", stderr.toExternalForm());
            }
            job.put("status", jobStatus);
            
            //init resultFiles
            final JSONArray outputFiles=new JSONArray();
            int numFiles=0;
            for(final ParameterInfo pinfo : getOutputFiles(jobInfo)) {
                final GpFilePath outputFile = getOutputFile(jobInfo, pinfo);
                boolean isExecutionLog=isExecutionLog(pinfo);
                if (isExecutionLog) {
                    try {
                        final String executionLogLocation=outputFile.getUrl().toExternalForm();
                        jobStatus.put("executionLogLocation", executionLogLocation);
                    }
                    catch (Exception e) {
                        throw new GetJobException("Error initializing executionLogLocation: "+e.getLocalizedMessage());
                    }
                }
                else {
                    ++numFiles;
                    try {
                        JSONObject outputFileJson=initOutputFile(outputFile);
                        outputFiles.put(outputFileJson);
                    }
                    catch (Exception e) {
                        throw new GetJobException("Error serializing JSON object for jobId="+jobInfo.getJobNumber());
                    }
                }
            }
            job.put("numOutputFiles", numFiles);
            job.put("outputFiles", outputFiles);
        }
        catch (JSONException e) {
            log.error("Error initializing JSON representation for jobId="+jobInfo.getJobNumber(), e);
            throw new GetJobException("Error initializing JSON representation for jobId="+jobInfo.getJobNumber()+
                    ": "+e.getLocalizedMessage());
        }
        return job;

    }
}