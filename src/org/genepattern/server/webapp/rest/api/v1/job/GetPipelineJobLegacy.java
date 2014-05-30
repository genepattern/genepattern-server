package org.genepattern.server.webapp.rest.api.v1.job;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.PermissionsHelper;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.dm.jobresult.JobResultFile;
import org.genepattern.server.webapp.rest.api.v1.DateUtil;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.JobInfoUtil;
import org.genepattern.webservice.ParameterInfo;
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

    private final String gpUrl;
    private final String jobsResourcePath;
    private final boolean includePermissions;
    public GetPipelineJobLegacy(final String gpUrl, final String jobsResourcePath) {
        this(gpUrl, jobsResourcePath, false); 
    }
    public GetPipelineJobLegacy(final String gpUrl, final String jobsResourcePath, final boolean includePermissions) {
        this.gpUrl=gpUrl;
        this.jobsResourcePath=jobsResourcePath;
        this.includePermissions=includePermissions;
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
        final boolean includeOutputFiles=true;
        return getJob(userContext, jobId, includeChildren, includeOutputFiles);
    }

    public JSONObject getJob(final GpContext userContext, final String jobId, final boolean includeChildren, final boolean includeOutputFiles) throws GetJobException {
        final JobInfo jobInfo=initJobInfo(userContext, jobId);
        return getJob(userContext, jobInfo, includeChildren, includeOutputFiles, includePermissions);
    }

    public JSONObject getJob(final GpContext userContext, final JobInfo jobInfo, final boolean includeChildren, final boolean includeOutputFiles, final boolean includePermissions) throws GetJobException {
        //manually create a JSONObject representing the job
        final JSONObject job;
        if (!includeChildren) {
            job = initJsonObject(gpUrl, jobInfo, includeOutputFiles);
        }
        else {
            try {
                InitPipelineJson walker=new InitPipelineJson(userContext, gpUrl, jobsResourcePath, jobInfo, includeOutputFiles);
                walker.prepareJsonObject();
                job=walker.getJsonObject();
            }
            catch (Throwable t) {
                final String errorMessage="Error getting JSON representation for children of jobId="+jobInfo.getJobNumber();
                log.error(errorMessage, t);
                throw new GetJobException(errorMessage + ": "+t.getLocalizedMessage());
            }
        }
        if (includePermissions && job!=null) {
            //only include permissions for the top-level job
            try {
            JSONObject permissions=initPermissionsFromJob(userContext, jobInfo);
            if (permissions!=null) {
                job.put("permissions", permissions);
            }
            }
            catch (Throwable t) {
                final String errorMessage="Error initializing permissions for jobId="+jobInfo.getJobNumber();
                log.error(errorMessage, t);
                throw new GetJobException(errorMessage + ": "+t.getLocalizedMessage());
            }
        }
        return job;
    }
    
    private JSONObject initPermissionsFromJob(final GpContext userContext, final JobInfo jobInfo) throws JSONException {
        final boolean isInTransaction=HibernateUtil.isInTransaction();
        try {
            // constructor starts a new DB transaction
            final PermissionsHelper ph=new PermissionsHelper(
                    userContext.isAdmin(), //final boolean _isAdmin, 
                    userContext.getUserId(), // final String _userId, 
                    jobInfo.getJobNumber(), // final int _jobNo, 
                    jobInfo.getUserId(), //final String _rootJobOwner, 
                    jobInfo.getJobNumber()//, //final int _rootJobNo, 
                    );
            JSONObject jsonObj=permissionsToJson(userContext, ph);
            return jsonObj;
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }

    public JSONObject getChildren(final GpContext userContext, final String jobId, final boolean includeChildren, final boolean includeOutputFiles) throws GetJobException {
        JSONObject parentJobObj=getJob(userContext, jobId, includeChildren, includeOutputFiles);
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
     * @param includeOutputFiles, if true include a representation of the output files
     * @return
     */
    public static JSONObject initJsonObject(final String gpUrl, final JobInfo jobInfo, final boolean includeOutputFiles) throws GetJobException {
        final JSONObject job = new JSONObject();
        try {
            job.put("jobId", ""+jobInfo.getJobNumber());
            job.put("taskName", jobInfo.getTaskName());
            job.put("taskLsid", jobInfo.getTaskLSID());
            job.put("datetime", jobInfo.getDateSubmitted().toString());
            
            job.put("dateSubmitted", DateUtil.toIso8601(jobInfo.getDateSubmitted()));
            if (jobInfo.getDateCompleted() != null) {
                job.put("dateCompleted", DateUtil.toIso8601(jobInfo.getDateCompleted()));
            }

            //job owner
            job.put("userId", jobInfo.getUserId());
            
            //access permissions
            //TODO: improve group permissions query
            /*
             select a.job_no, a.date_submitted, a.date_completed, a.user_id, a.task_lsid, a.task_name, j.group_id, j.permission_flag
             from analysis_job a left outer join job_group j on a.job_no = j.job_no  
             where a.user_id='{userId}'
            */
            
            //init jobStatus
            final JSONObject jobStatus = new JSONObject();
            final boolean isFinished=JobInfoUtil.isFinished(jobInfo);
            jobStatus.put("isFinished", isFinished);
            final boolean hasError=JobInfoUtil.hasError(jobInfo);
            jobStatus.put("hasError", hasError);
            final boolean isPending=JobInfoUtil.isPending(jobInfo);
            jobStatus.put("isPending", isPending);
            job.put("status", jobStatus);
            
            //init resultFiles
            //TODO: sort output files
            if (includeOutputFiles) {
                final JSONArray outputFiles=new JSONArray();
                final JSONArray logFiles=new JSONArray();
                int numFiles=0;
                for(final ParameterInfo pinfo : getOutputFiles(jobInfo)) {
                    final GpFilePath outputFile = getOutputFile(jobInfo, pinfo);
                    boolean isExecutionLog=GpOutputFile.isExecutionLog(pinfo);
                    if (isExecutionLog) {
                        try {
                            JSONObject logFileJson=GpOutputFile.fromGpfilePath(gpUrl, outputFile, pinfo).toJson();
                            String executionLogLocation=logFileJson.getJSONObject("link")
                                .getString("href");
                            jobStatus.put("executionLogLocation", executionLogLocation);
                            logFiles.put(logFileJson);
                        }
                        catch (Exception e) {
                            log.error("Error initializing executionLogLocation", e);
                            throw new GetJobException("Error initializing executionLogLocation: "+e.getLocalizedMessage());
                        }
                    }
                    else {
                        ++numFiles;
                        try {
                            JSONObject outputFileJson=GpOutputFile.fromGpfilePath(gpUrl, outputFile, pinfo).toJson();
                            outputFiles.put(outputFileJson);
                            if (pinfo._isStderrFile()) {
                                jobStatus.put("stderrLocation", outputFileJson.getJSONObject("link").getString("href"));
                            }
                        }
                        catch (Exception e) {
                            final String message="Error serializing JSON object for jobId="+jobInfo.getJobNumber();
                            log.error(message, e);
                            throw new GetJobException(message);
                        }
                    }
                }
                job.put("numOutputFiles", numFiles);
                job.put("outputFiles", outputFiles);
                job.put("logFiles", logFiles);
            }
        }
        catch (JSONException e) {
            log.error("Error initializing JSON representation for jobId="+jobInfo.getJobNumber(), e);
            throw new GetJobException("Error initializing JSON representation for jobId="+jobInfo.getJobNumber()+
                    ": "+e.getLocalizedMessage());
        }
        return job;
    }
    
    /**
     * Create JSON representation from the given PermissionsHelper class.
     * This class was part of the JSF implementation.
     * For example,
     * <pre>
       permissions: {
           currentUser: <currentUser>, <-- the user for whom these permissions apply, not necessarily the owner of the job
           canSetPermissions: <true | false>, <-- can the current user change the permissions
           canWrite: <true | false>, <-- can the current user modify the job (delete or delete files)
           canRead: <true | false>, <-- can the current user read the job (download files)
           isShared: <true | false>, <-- is this job shared with other users?
           isPublic: <true | false> <-- is this job public  
       }
     * </pre>
     * 
     * @param ph
     * @return
     */
    public static JSONObject permissionsToJson(final GpContext userContext, final PermissionsHelper ph) throws JSONException {
        if (ph==null) {
            log.error("PermissionsHelper is null");
            return null;
        }
        JSONObject jsonObj=new JSONObject();
        jsonObj.put("currentUser", userContext.getUserId());
        jsonObj.put("canRead", ph.canReadJob());
        jsonObj.put("canWrite", ph.canWriteJob());
        jsonObj.put("canSetPermissions", ph.canSetJobPermissions());
        jsonObj.put("isPublic", ph.isPublic());
        jsonObj.put("isShared", ph.isShared());
        return jsonObj;
    }

}