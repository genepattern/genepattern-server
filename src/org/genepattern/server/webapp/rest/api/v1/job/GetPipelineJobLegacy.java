package org.genepattern.server.webapp.rest.api.v1.job;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.PermissionsHelper;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.dm.jobresult.JobResultFile;
import org.genepattern.server.webapp.rest.api.v1.Util;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.util.GPConstants;
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
        o.put("lastModified", Util.toIso8601(gpFilePath.getLastModified()));

        JSONArray kindArr=new JSONArray();
        kindArr.put(gpFilePath.getKind());
        o.put("kind", kindArr);
        return o;
    }
    
    private final String jobsResourcePath;
    private final boolean includePermissions;
    public GetPipelineJobLegacy(final String jobsResourcePath) {
        this(jobsResourcePath, false); 
    }
    public GetPipelineJobLegacy(final String jobsResourcePath, final boolean includePermissions) {
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
            job = initJsonObject(jobInfo, includeOutputFiles);
        }
        else {
            try {
                InitPipelineJson walker=new InitPipelineJson(userContext, jobsResourcePath, jobInfo, includeOutputFiles);
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
    public static JSONObject initJsonObject(final JobInfo jobInfo, final boolean includeOutputFiles) throws GetJobException {
        final JSONObject job = new JSONObject();
        try {
            job.put("jobId", ""+jobInfo.getJobNumber());
            job.put("taskName", jobInfo.getTaskName());
            job.put("taskLsid", jobInfo.getTaskLSID());
            job.put("datetime", jobInfo.getDateSubmitted().toString());
            
            job.put("dateSubmitted", Util.toIso8601(jobInfo.getDateSubmitted()));
            if (jobInfo.getDateCompleted() != null) {
                job.put("dateCompleted", Util.toIso8601(jobInfo.getDateCompleted()));
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
            //TODO: sort output files
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