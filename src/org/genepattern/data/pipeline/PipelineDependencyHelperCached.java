package org.genepattern.data.pipeline;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoCache;

/**
 * Cached implementation of the PipelineDependency interface.
 * 
 * @author pcarr
 * 
 * @deprecated - Use the PipelineDependencyCache instead.
 *
 */
public class PipelineDependencyHelperCached implements PipelineDependency {
    public static Logger log = Logger.getLogger(PipelineDependency.class);
    
    // This is a map where given a pipeline, it will return all tasks on which it is directly dependent
    final ConcurrentMap<TaskInfo, Set<TaskInfo>> pipelineToDependencies = new ConcurrentHashMap<TaskInfo, Set<TaskInfo>>();
    
    // This is a map where given a task, it will return all pipelines it is directly embedded in
    final ConcurrentMap<TaskInfo, Set<TaskInfo>> taskToPipelines = new ConcurrentHashMap<TaskInfo, Set<TaskInfo>>();
    
    // The map of pipelines to missing task LSIDs in the pipeline
    final ConcurrentMap<TaskInfo, Set<String>> pipelineToMissing = new ConcurrentHashMap<TaskInfo, Set<String>>();
    
    
    public PipelineDependencyHelperCached() {
        log.debug("new PipelineDependencyHelperCached");
        build();
    }
    
    private synchronized void build() {
        log.debug("getting all tasks from cache");
        final TaskInfo[] tasks = TaskInfoCache.instance().getAllTasks();
        build(tasks);
    }
    
    private synchronized void build(final TaskInfo[] tasks) {
        log.debug("initializing from tasks");
        pipelineToDependencies.clear();
        taskToPipelines.clear();
        pipelineToMissing.clear();
        
        if (tasks==null) {
            log.error("tasks == null");
            return;
        }
        
        // Build up the pipelineToDependencies map
        for (final TaskInfo task : tasks) {
            if (task.isPipeline()) {
                addPipelineToDependencies(task);
            }
        }
        
        // Build up the taskToPipelines map
        for (final TaskInfo pipeline : pipelineToDependencies.keySet()) {
            this.addTasksFromPipeline(pipeline);
        }
        for (final TaskInfo task : tasks) {
            if (taskToPipelines.get(task) == null) {
                taskToPipelines.put(task, new HashSet<TaskInfo>());
            }
        }
    }
    
    public boolean isInitialized(final TaskInfo task) {
        return taskToPipelines.get(task) != null;
    }
    
    private void addPipelineToDependencies(final TaskInfo task) {
        final Set<TaskInfo> dependencies = extractDependencies(task);
        pipelineToDependencies.put(task, dependencies);
    }
    
    private void addTasksFromPipeline(final TaskInfo task) {
        final Set<TaskInfo> dependencies = pipelineToDependencies.get(task);
        for (final TaskInfo dependency : dependencies) {
            Set<TaskInfo> existing = taskToPipelines.get(dependency);
            if (existing == null) { 
                existing = new HashSet<TaskInfo>(); 
            }
            existing.add(task);
            taskToPipelines.put(dependency, existing);
        }
    }
    
    public synchronized void remove(final Integer taskId) {
        log.debug("remove taskId="+taskId);
        remove((TaskInfo) null);
    }
    
    public synchronized void remove(final TaskInfo task) {
        if (task==null) {
            log.debug("removing null task");
        }
        else {
            log.debug("removing task: "+task.getName()+", "+task.getLsid());
        }
        build();
    }
    
    public Set<String> getMissingDependencies(final TaskInfo task) {
        final Set<String> missing = pipelineToMissing.get(task);
        if (missing == null) {
            return new HashSet<String>();
        }
        else {
            return missing;
        }
    }
    
    /**
     * Returns a set of all recursive dependencies of the given pipeline
     * Note: This method may need some testing when put into actual use
     * @param task
     * @return
     */
    public Set<String> getMissingDependenciesRecursive(final TaskInfo task) {
        final Set<String> missing = new HashSet<String>();
        final Set<String> missingDependenciesForTask = getMissingDependencies(task);
        missing.addAll(missingDependenciesForTask);
        
        final Set<TaskInfo> pipelineToDependenciesSet=pipelineToDependencies.get(task);
        if (pipelineToDependenciesSet == null) {
            log.error("Error in PipelineDependency code, not pipelineToDependencies for task: "+task.getLsid());
        }
        else {
            for (final TaskInfo info : pipelineToDependenciesSet) {
                if (info.isPipeline()) {
                    final Set<String> missingDependenciesStep=getMissingDependenciesRecursive(info);
                    missing.addAll(missingDependenciesStep);
                }
            }
        } 
        return missing; 
    }
    
    public synchronized void add(final TaskInfo task) {
        if (task==null) {
            log.error("add task==null");
        }
        else {
            log.debug("add task: "+task.getName()+", "+task.getLsid());
        }
        if (isInitialized(task)) {
            if (task.isPipeline()) {
                addPipelineToDependencies(task);
                addTasksFromPipeline(task);
            }
        }
    }
    
    private Set<TaskInfo> extractDependencies(final TaskInfo task) {
        final Set<TaskInfo> dependencies = new HashSet<TaskInfo>();
        try {
            final PipelineModel model = PipelineUtil.getPipelineModel(task);
            final Vector<JobSubmission> jobs = model.getTasks();
            
            for (final JobSubmission job : jobs) {
                final TaskInfo dependant = getTaskInfoFromJobSubmission(job);
                if (dependant != null) {
                    dependencies.add(dependant);
                }
                else {
                    // The dependency was not found, add it to the map of missing dependencies
                    Set<String> missing = pipelineToMissing.get(task);
                    if (missing == null) { 
                        missing = new HashSet<String>(); 
                    }
                    missing.add(job.getLSID());
                    pipelineToMissing.put(task, missing);
                }
            }
        }
        catch (Exception e) {
            log.error("Exception building pipeline model from task: " + task.getLsid());
        }
        return dependencies;
    }
    
    private TaskInfo getTaskInfoFromJobSubmission(final JobSubmission job) {
        TaskInfo result = job.getTaskInfo();
        try {
            if (result == null) {
                String lsid = job.getLSID();
                if (lsid != null) {
                    result = TaskInfoCache.instance().getTask(lsid);
                }
            }
        }
        catch (Exception e) {
            result = null;
        }
        return result;
    }
    
    public Set<TaskInfo> getDependentPipelines(final PipelineModel pipeline) {
        final TaskInfo task = TaskInfoCache.instance().getTask(pipeline.getLsid());
        return getDependentPipelines(task);
    }
    
    public Set<TaskInfo> getDependentPipelines(final TaskInfo task) {
        return taskToPipelines.get(task);
    }
    
    public Set<TaskInfo> getDependentPipelinesRecursive(final PipelineModel pipeline) {
        final TaskInfo task = TaskInfoCache.instance().getTask(pipeline.getLsid());
        return getDependentPipelinesRecursive(task);
    }
    
    public Set<TaskInfo> getDependentPipelinesRecursive(final TaskInfo task) {
        final Set<TaskInfo> pipelines = new HashSet<TaskInfo>();
        return getDependentPipelinesRecursive(task, pipelines);
    }
    
    //TODO: document or fix this code, it's replacing the set of pipelines at each iteration in the loop
    private Set<TaskInfo> getDependentPipelinesRecursive(TaskInfo task, Set<TaskInfo> pipelines) {
        Set<TaskInfo> latest = getDependentPipelines(task);
        pipelines.addAll(latest);
        
        for (TaskInfo pipeline : latest) {
            pipelines = getDependentPipelinesRecursive(pipeline, pipelines);
        }
        
        return pipelines;
    }
    
    public Set<TaskInfo> getDependencies(final TaskInfo pipeline) {
        Set<TaskInfo> depends = pipelineToDependencies.get(pipeline);
        if (depends == null) {
            return Collections.emptySet();
        }
        else {
            return depends;
        }
    }
    
    public Set<TaskInfo> getDependencies(final PipelineModel pipeline) {
        TaskInfo task = TaskInfoCache.instance().getTask(pipeline.getLsid());
        return getDependencies(task);
    }
    
    public Set<TaskInfo> getDependenciesRecursive(final PipelineModel pipeline) {
        final TaskInfo task = TaskInfoCache.instance().getTask(pipeline.getLsid());
        return getDependenciesRecursive(task);
    }
    
    public Set<TaskInfo> getDependenciesRecursive(final TaskInfo pipeline) {
        final Set<TaskInfo> dependencies = new HashSet<TaskInfo>();
        return getDependenciesRecursive(pipeline, dependencies);
    }
    
    
    //TODO: double check this method, it's also replacing the return value at each iteration in the loop
    private Set<TaskInfo> getDependenciesRecursive(final TaskInfo pipeline, Set<TaskInfo> dependencies) {
        Set<TaskInfo> latest = getDependencies(pipeline);
        dependencies.addAll(latest);
        
        for (TaskInfo task : latest) {
            if (task.isPipeline()) {
                dependencies = getDependenciesRecursive(pipeline, dependencies);
            }
        }
        
        return dependencies;
    }
}