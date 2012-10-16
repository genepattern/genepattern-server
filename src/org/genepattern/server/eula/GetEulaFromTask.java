package org.genepattern.server.eula;

import java.util.List;

import org.genepattern.webservice.TaskInfo;

/**
 * Instead of adding a new method to the TaskInfo class, e.g. TaskInfo.getEulas(), implement this
 * interface.
 * 
 * @author pcarr
 *
 */
public interface GetEulaFromTask {
    
    /**
     * Get the list of End-user license agreements (EULA) for the given module or pipeline.
     * Can be an empty list if the task has none. 
     * When the taskInfo is a pipeline, this method does not include the EULAs required for any of the
     * steps included in the pipeline. 
     * 
     * This interface exists so that we can define different rules or strategies for declaring the EULA for a task.
     * 
     * Examples include, 
     *     a) a 'license=' property in the manifest, or,
     *     b) a support file which matches the naming convention, '*license*'
     *     c) a custom JSON or YAML format for license information (not implemented). 
     *     d) context specific licenses, for example based on the server configuration.
     * 
     * @param taskInfo
     * @return
     */
    List<EulaInfo> getEulasFromTask(TaskInfo taskInfo);
}
