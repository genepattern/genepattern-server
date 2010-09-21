package org.genepattern.server.executor.pipeline;

import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.server.JobInfoManager;
import org.genepattern.server.TaskIDNotFoundException;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;

/** 
 * Utility class for managing pipelines.
 * 
 * @author pcarr
 */
public class PipelineUtil {
    public static class PipelineModelException extends Exception {
        public PipelineModelException(Throwable t) {
            super(t);
        }
    }
    
    static public PipelineModel getPipelineModel(JobInfo pipelineJobInfo) 
    throws TaskIDNotFoundException, PipelineModelException
    {
        PipelineModel model = null;
        TaskInfo taskInfo = JobInfoManager.getTaskInfo(pipelineJobInfo.getTaskID());
        TaskInfoAttributes tia = taskInfo.giveTaskInfoAttributes();
        if (tia != null) {
            String serializedModel = (String) tia.get(GPConstants.SERIALIZED_MODEL);
            if (serializedModel != null && serializedModel.length() > 0) {
                try {
                    model = PipelineModel.toPipelineModel(serializedModel);
                } 
                catch (Throwable x) {
                    throw new PipelineModelException(x);
                }
            }
        }
        model.setLsid(pipelineJobInfo.getTaskLSID());
        return model;
    }


//    static public PipelineModel getPipelineModel(int pipelineTaskId) 
//    throws TaskIDNotFoundException, PipelineModelException
//    {
//        PipelineModel model = null;
//        TaskInfo taskInfo = JobInfoManager.getTaskInfo(pipelineTaskId);
//        TaskInfoAttributes tia = taskInfo.giveTaskInfoAttributes();
//        if (tia != null) {
//            String serializedModel = (String) tia.get(GPConstants.SERIALIZED_MODEL);
//            if (serializedModel != null && serializedModel.length() > 0) {
//                try {
//                    model = PipelineModel.toPipelineModel(serializedModel);
//                } 
//                catch (Throwable x) {
//                    throw new PipelineModelException(x);
//                }
//            }
//        }
//        return model;
//    }
//    
}
