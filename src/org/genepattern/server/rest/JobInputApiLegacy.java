package org.genepattern.server.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.eula.GetTaskStrategy;
import org.genepattern.server.executor.JobSubmissionException;
import org.genepattern.server.handler.AddNewJobHandler;
import org.genepattern.server.job.input.JobInput;
import org.genepattern.server.job.input.ParamListHelper;
import org.genepattern.server.job.input.JobInput.Param;
import org.genepattern.server.job.input.JobInput.ParamId;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;

public class JobInputApiLegacy {
    final static private Logger log = Logger.getLogger(JobInputApiLegacy.class);

    private Context jobContext;
    private JobInput jobInput;
    private TaskInfo taskInfo;
    

    public JobInputApiLegacy(final Context jobContext, final JobInput jobInput) {
        this.jobContext=jobContext;
        this.jobInput=jobInput;
    }

    public TaskInfo getTaskInfo() {
        return taskInfo;
    }

    public synchronized void initTaskInfo(final GetTaskStrategy getTaskStrategy) {
        this.taskInfo=getTaskStrategy.getTaskInfo(jobInput.getLsid());
        this.taskInfo.getParameterInfoArray();
    }

    public void initParameterValues() throws Exception {
        if (jobInput.getParams()==null) {
            log.debug("jobInput.params==null");
            return;
        }

        //initialize a map of paramName to ParameterInfo 
        final Map<String,ParameterInfo> paramInfoMap=new HashMap<String,ParameterInfo>();
        for(ParameterInfo pinfo : taskInfo.getParameterInfoArray()) {
            paramInfoMap.put(pinfo.getName(), pinfo);
        }

        //set default values for any parameters which were not set by the user
        for(Entry<String,ParameterInfo> entry : paramInfoMap.entrySet()) {
            final String pname=entry.getKey();
            final ParameterInfo pinfo=entry.getValue();
            Param inputValue=jobInput.getParam(entry.getKey());
            if (inputValue==null) {
                //param not set by end user, check for default values
                List<String> defaultValues=ParamListHelper.getDefaultValues(pinfo);
                if (defaultValues != null) {
                    for(final String value : defaultValues) {
                        log.debug("adding default value: "+pname+"="+value);
                        jobInput.addValue(pname, value);
                    }
                }
            }
        }

        //walk through the list of input values
        for(Entry<ParamId, Param> entry : jobInput.getParams().entrySet()) {
            final Param param=entry.getValue();                
            final ParamId id = param.getParamId();
            final ParameterInfo pinfo=paramInfoMap.get(id.getFqName());
            if (pinfo==null) {
                log.error("Can't get pInfo for id="+id.getFqName());
                break;
            }
            
            ParamListHelper plh=new ParamListHelper(jobContext, pinfo, param);
            plh.validateNumValues();
            plh.updatePinfoValue();
        }
    }

    private List<ParameterInfo> getMissingParams() {
        //initialize a map of paramName to ParameterInfo 
        final List<ParameterInfo> requiredParams=new ArrayList<ParameterInfo>();
        for(ParameterInfo pinfo : taskInfo.getParameterInfoArray()) {
            if (!pinfo.isOptional()) {
                requiredParams.add(pinfo);
            }
        }

        final List<ParameterInfo> missingRequiredParams=new ArrayList<ParameterInfo>();
        for(ParameterInfo pinfo : requiredParams) {
            if (!jobInput.hasValue(pinfo.getName())) {
                missingRequiredParams.add(pinfo);
            }
        }
        return missingRequiredParams;
    }
    
    public String submitJob() throws JobSubmissionException {
        JobInfo job = submitJob(taskInfo.getID(), taskInfo.getParameterInfoArray());
        String jobId = "" + job.getJobNumber();
        return jobId;
    }

    private JobInfo submitJob(final int taskID, final ParameterInfo[] parameters) throws JobSubmissionException {
        AddNewJobHandler req = new AddNewJobHandler(taskID, jobContext.getUserId(), parameters);
        JobInfo jobInfo = req.executeRequest();
        return jobInfo;
    }
}

