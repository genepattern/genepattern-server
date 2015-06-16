/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.data.pipeline;

import java.util.Set;

import org.genepattern.webservice.TaskInfo;

/**
 * interface extracted from original PipelineDependencyHelper class.
 * 
 * @author pcarr
 *
 */
public interface PipelineDependency {
    boolean isInitialized(TaskInfo task);
    void remove(Integer taskId);
    void remove(TaskInfo task);
    Set<String> getMissingDependencies(TaskInfo task);
    
    /**
     * Returns a set of all recursive dependencies of the given pipeline
     * Note: This method may need some testing when put into actual use
     * @param task
     * @return
     */
    Set<String> getMissingDependenciesRecursive(TaskInfo task);
    void add(TaskInfo task);
    Set<TaskInfo> getDependentPipelines(PipelineModel pipeline);    
    Set<TaskInfo> getDependentPipelines(TaskInfo task);
    Set<TaskInfo> getDependentPipelinesRecursive(PipelineModel pipeline);
    Set<TaskInfo> getDependentPipelinesRecursive(TaskInfo task);
    Set<TaskInfo> getDependencies(TaskInfo pipeline);
    Set<TaskInfo> getDependencies(PipelineModel pipeline);
    Set<TaskInfo> getDependenciesRecursive(PipelineModel pipeline);
    Set<TaskInfo> getDependenciesRecursive(TaskInfo pipeline);
}