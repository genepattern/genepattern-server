package org.genepattern.server.eula;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
public class GetEulaFromTaskImpl03 implements GetEulaFromTask {
    private static Logger log = Logger.getLogger(GetEulaFromTaskImpl03.class);

    public List<EulaInfo> getEulasFromTask(TaskInfo taskInfo) {
        List<EulaInfo> eulas = appendEulaInfo(null, null, taskInfo);
        return eulas;
    }
    
    //recursive implementation
    private List<EulaInfo> appendEulaInfo(List<EulaInfo> eulas, GetEulaFromTask getEulaFromTask, TaskInfo taskInfo) {
        if (eulas==null) {
            eulas=new ArrayList<EulaInfo>();
        }
        if (getEulaFromTask==null) {
            //TODO: customize the rule for checking if a single task (module or pipeline) requires an EULA
            getEulaFromTask = new GetEulaFromTaskImpl02();
        }
        List<EulaInfo> eulaObjs = getEulaFromTask.getEulasFromTask(taskInfo);
        eulas.addAll(eulaObjs);
        
        if (taskInfo.isPipeline()) {
            List<TaskInfo> children = getChildren(taskInfo);
            for(TaskInfo child : children) {
                appendEulaInfo(eulas, getEulaFromTask, child);
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
