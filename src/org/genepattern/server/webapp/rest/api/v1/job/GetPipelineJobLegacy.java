/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webapp.rest.api.v1.job;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.JobInfoManager;
import org.genepattern.server.PermissionsHelper;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.dm.jobresult.JobResultFile;
import org.genepattern.server.executor.drm.dao.JobRunnerJob;
import org.genepattern.server.executor.drm.dao.JobRunnerJobDao;
import org.genepattern.server.job.JobInfoLoaderDefault;
import org.genepattern.server.job.comment.JobComment;
import org.genepattern.server.job.comment.JobCommentManager;
import org.genepattern.server.job.status.Status;
import org.genepattern.server.job.tag.JobTag;
import org.genepattern.server.job.tag.JobTagManager;
import org.genepattern.server.webapp.rest.api.v1.DateUtil;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoCache;
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
    
    public static JobInfo initJobInfo(final GpContext userContext, final String jobId) throws GetJobException {
        if (userContext==null) {
            throw new IllegalArgumentException("userContext==null");
        }
        if (userContext.getUserId()==null || userContext.getUserId().length() == 0) {
            throw new IllegalArgumentException("userContext.userId not set");
        }
        
        final boolean isInTransaction=HibernateUtil.isInTransaction();
        try {
            JobInfo jobInfo=new JobInfoLoaderDefault().getJobInfo(userContext, jobId);
            return jobInfo;
        }
        catch (Exception e) {
            throw new GetJobException(e.getLocalizedMessage());
        }
        catch (Throwable t) {
            log.error("Error initializing jobInfo for jobId="+jobId, t);
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
        throw new GetJobException("Server error initializing jobInfo, jobId="+jobId);
    }

    
    @Override
    public JSONObject getJob(final GpContext userContext, final String jobId,
                             boolean includeComments, boolean includeTags)
    throws GetJobException
    {
        final boolean includeChildren=false; //legacy support
        final boolean includeOutputFiles=true;
        return getJob(userContext, jobId, includeChildren, includeOutputFiles, includeComments, includeTags);
    }

    public JSONObject getJob(final GpContext userContext, final String jobId, final boolean includeChildren,
                             final boolean includeOutputFiles, boolean includeComments, boolean includeTags) throws GetJobException {
        final JobInfo jobInfo=initJobInfo(userContext, jobId);
        return getJob(userContext, jobInfo, includeChildren, includeOutputFiles, includePermissions, includeComments, includeTags);
    }

    public JSONObject getJob(final GpContext userContext, final JobInfo jobInfo, final boolean includeChildren,
                             final boolean includeOutputFiles, final boolean includePermissions,
                             final boolean includeComments, final boolean includeTags) throws GetJobException {
        //manually create a JSONObject representing the job
        final JSONObject job;
        if (!includeChildren) {
            job = initJsonObject(gpUrl, jobInfo, includeOutputFiles, includeComments, includeTags);
        }
        else {
            try {
                InitPipelineJson walker=new InitPipelineJson(userContext, gpUrl, jobsResourcePath, jobInfo,
                        includeOutputFiles, includeComments, includeTags);
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
    public static JSONObject initJsonObject(final String gpUrl, final JobInfo jobInfo,
                                            final boolean includeOutputFiles, final boolean includeComments,
                                            final boolean includeTags) throws GetJobException {

        TaskInfo taskInfo = null;
        try {
            taskInfo = TaskInfoCache.instance().getTask(jobInfo.getTaskID());
        }
        catch (Throwable t)
        {
            log.error("Error getting TaskInfo for job, jobId="+jobInfo.getJobNumber()+
                    ", taskName="+jobInfo.getTaskName()+
                    ", taskLsid="+jobInfo.getTaskLSID(), t);
        }
        final boolean includeJobRunnerStatus=true; // default value
        return initJsonObject(gpUrl, jobInfo, taskInfo, includeOutputFiles, includeComments, includeTags, includeJobRunnerStatus);
    }

    public static JSONObject initJsonObject(final String gpUrl, final JobInfo jobInfo, final TaskInfo taskInfo,
                                            final boolean includeOutputFiles, final boolean includeComments,
                                            final boolean includeTags, final boolean includeJobRunnerStatus) throws GetJobException {
        JobRunnerJob jobStatusRecord=null;
        if (includeJobRunnerStatus) {
            try {
                jobStatusRecord=new JobRunnerJobDao().selectJobRunnerJob(jobInfo.getJobNumber());
            }
            catch (Throwable t) {
                log.error("Unexpected error initializing jobStatusRecord from jobId="+jobInfo.getJobNumber(), t);
            }
        }
        return initJsonObject(gpUrl, jobInfo, taskInfo, jobStatusRecord, includeOutputFiles, includeComments, includeTags);
    }

    public static JSONObject initJsonObject(final String gpUrl, final JobInfo jobInfo, final TaskInfo taskInfo, final JobRunnerJob jobStatusRecord,
                                            final boolean includeOutputFiles, final boolean includeComments,
                                            final boolean includeTags) throws GetJobException {
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

            try {
                if(taskInfo != null && taskInfo.getTaskInfoAttributes() != null
                        &&TaskInfo.isJavascript(taskInfo.getTaskInfoAttributes()))
                {
                    job.put("launchUrl", JobInfoManager.generateLaunchURL(taskInfo, jobInfo));
                }
            }
            catch (Exception e)
            {
                log.error("Error getting launch Url", e);
            }

            //access permissions
            //TODO: improve group permissions query
            /*
             select a.job_no, a.date_submitted, a.date_completed, a.user_id, a.task_lsid, a.task_name, j.group_id, j.permission_flag
             from analysis_job a left outer join job_group j on a.job_no = j.job_no  
             where a.user_id='{userId}'
            */
            
            //init resultFiles
            //TODO: sort output files
            String executionLogLocation=null;
            String stderrLocation=null;
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
                            logFiles.put(logFileJson);
                            executionLogLocation=logFileJson.getJSONObject("link").getString("href");
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
                                stderrLocation=outputFileJson.getJSONObject("link").getString("href");
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

                final JSONObject jobStatus = initJobStatusJson(jobInfo, jobStatusRecord, executionLogLocation, stderrLocation); 
                job.put("status", jobStatus);
            }

            if(includeComments)
            {
                List<JobComment> jobComments = JobCommentManager.selectAllJobComments(jobInfo.getJobNumber());
                job.put("comments", JobCommentManager.createJobCommentBundle(jobInfo.getUserId(), jobComments));
            }

            if(includeTags)
            {
                List<JobTag> jobTags = JobTagManager.selectAllJobTags(jobInfo.getJobNumber());
                job.put("tags", JobTagManager.createJobTagBundle(jobTags));
            }
        }
        catch (JSONException e) {
            log.error("Error initializing JSON representation for jobId="+jobInfo.getJobNumber(), e);
            throw new GetJobException("Error initializing JSON representation for jobId="+jobInfo.getJobNumber()+
                    ": "+e.getLocalizedMessage());
        }
        return job;
    }

    private static JSONObject initJobStatusJson(final JobInfo jobInfo, final JobRunnerJob jobStatusRecord, String executionLogLocation, String stderrLocation) throws JSONException  {
        Status status=new Status.Builder()
            .jobInfo(jobInfo)
            .jobStatusRecord(jobStatusRecord)
            .stderrLocation(stderrLocation)
            .executionLogLocation(executionLogLocation)
        .build();
        
        final JSONObject statusObj = status.toJsonObj();
        return statusObj;
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