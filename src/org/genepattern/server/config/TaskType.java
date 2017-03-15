package org.genepattern.server.config;

import org.genepattern.util.GPConstants;
import org.genepattern.webservice.TaskInfo;

/**
 * The taskType for a module. 
 * Use this as the canonical representation for the GpContext class.
 * 
 * @author pcarr
 */
public enum TaskType {
    JOB,
    VISUALIZER,
    JAVASCRIPT,
    PIPELINE,
    NOT_SET;

    public static TaskType initTaskType(final TaskInfo taskInfo) {
        if (taskInfo == null || taskInfo.getTaskInfoAttributes() == null) {
            return TaskType.NOT_SET;
        };
        final String taskType=taskInfo.getTaskInfoAttributes().get(GPConstants.TASK_TYPE);
        
        if (GPConstants.TASK_TYPE_VISUALIZER.equalsIgnoreCase(taskType)) {
            return VISUALIZER;
        }
        else if (GPConstants.TASK_TYPE_JAVASCRIPT.equalsIgnoreCase(taskType)) {
            return JAVASCRIPT;
        }
        // note: 'endsWith' idiom is from a very early implementation in the TaskInfo class
        //     it's not clear to me if we need to match the pattern '*pipeline' or if an
        //     exact match is better
        else if (taskType.endsWith("pipeline")) {
            return PIPELINE;
        }
        else {
            return JOB;
        }
    }
}