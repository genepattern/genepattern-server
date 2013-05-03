package org.genepattern.server.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.genepattern.server.eula.GetTaskStrategyDefault;
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
        this(jobContext, jobInput, null);
    }
    public JobInputApiLegacy(final Context jobContext, final JobInput jobInput, final GetTaskStrategy getTaskStrategyIn) {
        this.jobContext=jobContext;
        this.jobInput=jobInput;
        
        final GetTaskStrategy getTaskStrategy;
        if (getTaskStrategyIn == null) {
            getTaskStrategy=new GetTaskStrategyDefault();
        }
        else {
            getTaskStrategy=getTaskStrategyIn;
        }
        this.taskInfo=getTaskStrategy.getTaskInfo(jobInput.getLsid());
        this.taskInfo.getParameterInfoArray();
    }

    public TaskInfo getTaskInfo() {
        return taskInfo;
    }

    public ParameterInfo[] initParameterValues() throws Exception 
    {
        if (jobInput.getParams()==null) {
            log.debug("jobInput.params==null");
            return new ParameterInfo[0];
        }

        //initialize a map of paramName to ParameterInfo 
        final Map<String,ParameterInfoRecord> paramInfoMap=ParameterInfoRecord.initParamInfoMap(taskInfo);
        //set default values for any parameters which were not set by the user
        for(Entry<String,ParameterInfoRecord> entry : paramInfoMap.entrySet()) {
            final String pname=entry.getKey();
            final ParameterInfoRecord record=entry.getValue();
            Param inputValue=jobInput.getParam(entry.getKey());
            if (inputValue==null) {
                //param not set by end user, check for default values
                List<String> defaultValues=ParamListHelper.getDefaultValues(record.getFormal());
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
            final ParameterInfoRecord record=paramInfoMap.get(id.getFqName());
            if (record==null) {
                log.error("Can't get record for id="+id.getFqName());
                break;
            }
            
            ParamListHelper plh=new ParamListHelper(jobContext, record, param);
            plh.validateNumValues();
            plh.updatePinfoValue();
        }
        
        List<ParameterInfo> actualParameters = new ArrayList<ParameterInfo>();
        for(ParameterInfoRecord pinfoRecord : paramInfoMap.values()) {
            actualParameters.add( pinfoRecord.getActual() );
        }
        ParameterInfo[] actualParams = actualParameters.toArray(new ParameterInfo[0]);
        return actualParams;
    }

    public String submitJob() throws Exception {
        final ParameterInfo[] actualValues=initParameterValues();
        final int taskId=getTaskInfo().getID();
        final JobInfo jobInfo=submitJob(taskId, actualValues);
        final String jobId = "" + jobInfo.getJobNumber();
        return jobId;
    }

    public JobInfo submitJob(final int taskID, final ParameterInfo[] parameters) throws JobSubmissionException {
        AddNewJobHandler req = new AddNewJobHandler(taskID, jobContext.getUserId(), parameters);
        JobInfo jobInfo = req.executeRequest();
        return jobInfo;
    }
}

