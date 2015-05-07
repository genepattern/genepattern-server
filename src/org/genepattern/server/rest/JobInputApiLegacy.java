/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.eula.GetTaskStrategy;
import org.genepattern.server.eula.GetTaskStrategyDefault;
import org.genepattern.server.executor.JobSubmissionException;
import org.genepattern.server.handler.AddNewJobHandler;
import org.genepattern.server.job.input.JobInput;
import org.genepattern.server.job.input.Param;
import org.genepattern.server.job.input.ParamListHelper;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;

public class JobInputApiLegacy {
    final static private Logger log = Logger.getLogger(JobInputApiLegacy.class);

    private GpContext userContext;
    private JobInput jobInput;
    private TaskInfo taskInfo;
    private final boolean initDefault;
    

    public JobInputApiLegacy(final GpContext userContext, final JobInput jobInput) {
        this(userContext, jobInput, null);
    }
    public JobInputApiLegacy(final GpContext userContext, final JobInput jobInput, final GetTaskStrategy getTaskStrategyIn) {
        this(userContext, jobInput, getTaskStrategyIn, false);
    }
    public JobInputApiLegacy(final GpContext userContext, final JobInput jobInput, final GetTaskStrategy getTaskStrategyIn, final boolean initDefault) {
        this.userContext=userContext;
        this.jobInput=jobInput;
        this.initDefault=initDefault;
        
        final GetTaskStrategy getTaskStrategy;
        if (getTaskStrategyIn == null) {
            getTaskStrategy=new GetTaskStrategyDefault();
        }
        else {
            getTaskStrategy=getTaskStrategyIn;
        }
        this.taskInfo=getTaskStrategy.getTaskInfo(jobInput.getLsid());
        this.taskInfo.getParameterInfoArray();
        userContext.setTaskInfo(taskInfo);
    }

    public TaskInfo getTaskInfo() {
        return taskInfo;
    }

    private ParameterInfo[] initParameterValues() throws Exception 
    {
        if (jobInput.getParams()==null) {
            log.debug("jobInput.params==null");
            return new ParameterInfo[0];
        }

        //initialize a map of paramName to ParameterInfo 
        final Map<String,ParameterInfoRecord> paramInfoMap=ParameterInfoRecord.initParamInfoMap(taskInfo);

        //for each formal input parameter ... set the actual value to be used on the command line
        for(Entry<String,ParameterInfoRecord> entry : paramInfoMap.entrySet()) {
            // validate num values
            // and initialize input file (or parameter) lists as needed
            Param inputParam=jobInput.getParam( entry.getKey() );
            ParamListHelper plh=new ParamListHelper(userContext, entry.getValue(), inputParam, initDefault);
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
        AddNewJobHandler req = new AddNewJobHandler(taskID, userContext.getUserId(), parameters);
        JobInfo jobInfo = req.executeRequest();
        return jobInfo;
    }
}

