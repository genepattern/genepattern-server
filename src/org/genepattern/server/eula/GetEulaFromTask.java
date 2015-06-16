/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
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
    
    /**
     * For an individual module or task, set the End-user license agreement (EULA) for the task.
     * Can be null, which means, 'this module (or pipeline) has no EULA'.
     * 
     * This method may make changes to the TaskInfo arg. Make sure to persist those changes to the DB
     * after you call this method.

     * @param eula
     * @param taskInfo
     */
    void setEula(EulaInfo eula, TaskInfo taskInfo);

    /**
     * For an individual module or task, set the list of zero or more End-user license agreements (EULA)
     * attached to the task.
     * 
     * This method may make changes to the TaskInfo arg. Make sure to persist those changes to the DB
     * after you call this method.
     * 
     * @param eulas
     * @param taskInfo
     */
    void setEulas(List<EulaInfo> eulas, TaskInfo taskInfo);

}
