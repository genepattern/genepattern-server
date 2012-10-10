package org.genepattern.server.eula;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.data.pipeline.PipelineModelException;
import org.genepattern.data.pipeline.PipelineUtil;
import org.genepattern.webservice.TaskInfo;

/**
 * Recursively get all EulaInfo for the given task.
 * This method checks for licensed modules in pipelines.
 * 
 * @author pcarr
 */
public class GetEulaFromTaskRecursive {
    private static Logger log = Logger.getLogger(GetEulaFromTaskRecursive.class);

//    //TODO: use Strategy pattern for this method
//    private GetEulaFromTask getGetEulaFromTask() {
//        //allow for dependency injection, via setGetEulaFromTask
//        if (getEulaFromTask != null) {
//            return getEulaFromTask;
//        }
//        
//        //otherwise, hard-coded rule        
//        //option 1: license= in manifest
//        //return new GetEulaFromTaskImpl01();
//        //option 2: support file named '*license*' in tasklib
//        return new GetEulaAsSupportFile();
//    }
    
    private GetEulaFromTask getEulaFromTask = null;
    public void setGetEulaFromTask(GetEulaFromTask impl) {
        this.getEulaFromTask=impl;
    }
  
    public SortedSet<EulaInfo> getEulasFromTask(TaskInfo taskInfo) {
        SortedSet<EulaInfo> eulas = appendEulaInfo(null, taskInfo);
        return eulas;
    }
    
    //recursive implementation
    private SortedSet<EulaInfo> appendEulaInfo(SortedSet<EulaInfo> eulas, TaskInfo taskInfo) {
        if (eulas==null) {
            eulas=new TreeSet<EulaInfo>(EulaInfo.defaultComparator(taskInfo));
        }
        if (getEulaFromTask==null) {
            //TODO: should throw exception, instead return empty set
            log.error("Initialization error, getEulaFromTask==null");
            return new TreeSet<EulaInfo>();
        }
        List<EulaInfo> eulaObjs = getEulaFromTask.getEulasFromTask(taskInfo);
        //TODO: this is the part of the code where we add the duplicates
        eulas.addAll(eulaObjs);
        
        if (taskInfo.isPipeline()) {
            List<TaskInfo> children = getChildren(taskInfo);
            for(TaskInfo child : children) {
                appendEulaInfo(eulas, child);
            }
        }
        return eulas; 
    }
    
    private List<TaskInfo> getChildren(TaskInfo parent) {
        if (parent==null) {
            throw new IllegalArgumentException("parent==null");
        }
        PipelineModel pipelineModel=null;
        try {
            pipelineModel=PipelineUtil.getPipelineModel(parent);
        }
        catch (PipelineModelException e) {
            log.error("Error initializing PipelineModel for task, lsid="+parent.getLsid(), e);
            return Collections.emptyList();
        }
        pipelineModel.setLsid(parent.getLsid());

        List<TaskInfo> children = new ArrayList<TaskInfo>();
        for(JobSubmission jobSubmission : pipelineModel.getTasks()) {
            TaskInfo child=jobSubmission.getTaskInfo();
            children.add(child);
        }
        return children;
    }

}
