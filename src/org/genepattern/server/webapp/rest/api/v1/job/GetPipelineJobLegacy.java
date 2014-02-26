package org.genepattern.server.webapp.rest.api.v1.job;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.dm.jobresult.JobResultFile;
import org.genepattern.server.webapp.SendToModuleManager;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.JobInfoUtil;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class GetPipelineJobLegacy implements GetJob {
    private static final Logger log = Logger.getLogger(GetPipelineJobLegacy.class);
    
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

    /**
     * Add a list of sendToModule lsids
     * This list will be empty is there is a null user in the userContext
     * @param userContext
     * @param kind
     * @return
     */
    public static JSONArray initSendTo(GpContext userContext, String kind) {
        JSONArray sendTo = new JSONArray();

        if (userContext.getUserId() != null) {
            SortedSet<TaskInfo> tasks = SendToModuleManager.instance().getSendTo(userContext.getUserId(), kind);

            for (TaskInfo task: tasks) {
                sendTo.put(task.getLsid());
            }
        }

        return sendTo;
    }
    
    public static JSONObject initOutputFile(GpContext userContext, final GpFilePath gpFilePath) throws Exception {
        //create a JSON representation of a file
        JSONObject o = new JSONObject();

        JSONObject link = new JSONObject();
        link.put("href", gpFilePath.getUrl().toExternalForm());
        link.put("name", gpFilePath.getName());
        o.put("link", link);

        o.put("sendTo", initSendTo(userContext, gpFilePath.getKind()));

        o.put("fileLength", gpFilePath.getFileLength());
        o.put("lastModified", gpFilePath.getLastModified().getTime());
        o.put("kind", gpFilePath.getKind());
        return o;
    }
    
    private final String jobsResourcePath;
    public GetPipelineJobLegacy(final String jobsResourcePath) {
        this.jobsResourcePath=jobsResourcePath;
    }
    
    private JobInfo initJobInfo(final GpContext userContext, final String jobId) throws GetJobException {
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
        return jobInfo;
    }
    
    @Override
    public JSONObject getJob(final GpContext userContext, final String jobId)
    throws GetJobException
    {
        final boolean includeChildren=false; //legacy support
        return getJob(userContext, jobId, includeChildren);
    }

    public JSONObject getJob(final GpContext userContext, final String jobId, final boolean includeChildren) throws GetJobException {
        JobInfo jobInfo=initJobInfo(userContext, jobId);
        return getJob(userContext, jobInfo, includeChildren);
    }

    public JSONObject getJob(GpContext userContext, JobInfo jobInfo, boolean includeChildren) throws GetJobException {
        //manually create a JSONObject representing the job
        final JSONObject job;
        if (!includeChildren) {
            job = initJsonObject(userContext, jobInfo);
        }
        else {
            try {
                InitPipelineJson walker=new InitPipelineJson(userContext, jobsResourcePath, jobInfo);
                walker.prepareJsonObject();
                job=walker.getJsonObject();
            }
            catch (Throwable t) {
                final String errorMessage="Error getting JSON representation for children of jobId="+jobInfo.getJobNumber();
                log.error(errorMessage, t);
                throw new GetJobException(errorMessage + ": "+t.getLocalizedMessage());
            }
        }
        return job;
    }

    public JSONObject getChildren(GpContext userContext, String jobId) throws GetJobException {
        final boolean includeChildren=true;
        JSONObject parentJobObj=getJob(userContext, jobId, includeChildren);
        try {
            return parentJobObj.getJSONObject("children");
        }
        catch (Throwable t) {
            log.error(t);
        }
        return null;
    }

    /**
     * Create a JSONObject representing the job
     * @param jobInfo
     * @return
     */
    public static JSONObject initJsonObject(GpContext userContext, final JobInfo jobInfo) throws GetJobException {
        final boolean includeOutputFiles=true;
        return initJsonObject(userContext, jobInfo, includeOutputFiles);
    }

    public static JSONObject initJsonObject(GpContext userContext, final JobInfo jobInfo, final boolean includeOutputFiles) throws GetJobException {
        final JSONObject job = new JSONObject();
        try {
            job.put("jobId", ""+jobInfo.getJobNumber());
            job.put("taskName", jobInfo.getTaskName());
            job.put("taskLsid", jobInfo.getTaskLSID());
            job.put("datetime", jobInfo.getDateSubmitted().toString());
            
            //init jobStatus
            final JSONObject jobStatus = new JSONObject();
            final boolean isFinished=JobInfoUtil.isFinished(jobInfo);
            jobStatus.put("isFinished", isFinished);
            final boolean hasError=JobInfoUtil.hasError(jobInfo);
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
            if (includeOutputFiles) {
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
                            JSONObject outputFileJson=initOutputFile(userContext, outputFile);
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
        }
        catch (JSONException e) {
            log.error("Error initializing JSON representation for jobId="+jobInfo.getJobNumber(), e);
            throw new GetJobException("Error initializing JSON representation for jobId="+jobInfo.getJobNumber()+
                    ": "+e.getLocalizedMessage());
        }
        return job;

    }

}