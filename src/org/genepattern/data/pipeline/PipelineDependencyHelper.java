package org.genepattern.data.pipeline;

import java.util.HashSet;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoCache;

public class PipelineDependencyHelper {
    public static Logger log = Logger.getLogger(PipelineDependencyHelper.class);
    final static PipelineDependencyHelper helper = new PipelineDependencyHelper();
    
    // If the singleton has been initialized yet or not
    static boolean init = false;
    
    // This is a map where given a pipeline, it will return all tasks on which it is directly dependent
    ConcurrentMap<TaskInfo, Set<TaskInfo>> pipelineToDependencies = new ConcurrentHashMap<TaskInfo, Set<TaskInfo>>();
    
    // This is a map where given a task, it will return all pipelines it is directly embedded in
    ConcurrentMap<TaskInfo, Set<TaskInfo>> taskToPipelines = new ConcurrentHashMap<TaskInfo, Set<TaskInfo>>();
    
 // The map of pipelines to missing task LSIDs in the pipeline
    ConcurrentMap<TaskInfo, Set<String>> pipelineToMissing = new ConcurrentHashMap<TaskInfo, Set<String>>();
    
    public static PipelineDependencyHelper instance() {
        if (!init) { helper.build(); }
        return helper;
    }
    
    public synchronized void build() {
        TaskInfo[] tasks = TaskInfoCache.instance().getAllTasks();
        pipelineToDependencies = new ConcurrentHashMap<TaskInfo, Set<TaskInfo>>();
        
        // Build up the pipelineToDependencies map
        for (TaskInfo task : tasks) {
            if (task.isPipeline()) {
                addPipelineToDependencies(task);
            }
        }
        
        // Build up the taskToPipelines map
        for (TaskInfo pipeline : pipelineToDependencies.keySet()) {
            this.addTasksFromPipeline(pipeline);
        }
        for (TaskInfo task : tasks) {
            if (taskToPipelines.get(task) == null) {
                taskToPipelines.put(task, new HashSet<TaskInfo>());
            }
        }
        
        init = true;
    }
    
    public boolean isInitialized(TaskInfo task) {
        return taskToPipelines.get(task) != null;
    }
    
    private void addPipelineToDependencies(TaskInfo task) {
        Set<TaskInfo> dependencies = extractDependencies(task);
        pipelineToDependencies.put(task, dependencies);
    }
    
    private void addTasksFromPipeline(TaskInfo task) {
        Set<TaskInfo> dependencies = pipelineToDependencies.get(task);
        for (TaskInfo dependency : dependencies) {
            Set<TaskInfo> existing = taskToPipelines.get(dependency);
            if (existing == null) { existing = new HashSet<TaskInfo>(); }
            existing.add(task);
            taskToPipelines.put(dependency, existing);
        }
    }
    
    public synchronized void remove(Integer taskId) {
        remove((TaskInfo) null);
    }
    
    public synchronized void remove(TaskInfo task) {
        helper.build();
    }
    
    public Set<String> getMissingDependencies(TaskInfo task) {
        Set<String> missing = pipelineToMissing.get(task);
        if (missing == null) return new HashSet<String>();
        else return missing;
    }
    
    /**
     * Returns a set of all recursive dependencies of the given pipeline
     * Note: This method may need some testing when put into actual use
     * @param task
     * @return
     */
    public Set<String> getMissingDependenciesRecursive(TaskInfo task) {
        Set<String> missing = new HashSet<String>();
        missing.addAll(getMissingDependencies(task));
        
        for (TaskInfo info : pipelineToDependencies.get(task)) {
            if (info.isPipeline()) {
                missing.addAll(getMissingDependenciesRecursive(info));
            }
        }
        
        return missing;
        
    }
    
    public synchronized void add(TaskInfo task) {
        if (!PipelineDependencyHelper.instance().isInitialized(task)) {
            if (task.isPipeline()) {
                addPipelineToDependencies(task);
                addTasksFromPipeline(task);
            }
        }
    }
    
    private Set<TaskInfo> extractDependencies(TaskInfo task) {
        Set<TaskInfo> dependencies = new HashSet<TaskInfo>();
        try {
            PipelineModel model = PipelineUtil.getPipelineModel(task);
            Vector<JobSubmission> jobs = model.getTasks();
            
            for (JobSubmission job : jobs) {
                TaskInfo dependant = getTaskInfoFromJobSubmission(job);
                if (dependant != null) {
                    dependencies.add(dependant);
                }
                else {
                    // The dependency was not found, add it to the map of missing dependencies
                    Set<String> missing = pipelineToMissing.get(task);
                    if (missing == null) { missing = new HashSet<String>(); }
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
    
    private TaskInfo getTaskInfoFromJobSubmission(JobSubmission job) {
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
    
    public Set<TaskInfo> getDependentPipelines(PipelineModel pipeline) {
        TaskInfo task = TaskInfoCache.instance().getTask(pipeline.getLsid());
        return getDependentPipelines(task);
    }
    
    public Set<TaskInfo> getDependentPipelines(TaskInfo task) {
        return taskToPipelines.get(task);
    }
    
    public Set<TaskInfo> getDependentPipelinesRecursive(PipelineModel pipeline) {
        TaskInfo task = TaskInfoCache.instance().getTask(pipeline.getLsid());
        return getDependentPipelinesRecursive(task);
    }
    
    public Set<TaskInfo> getDependentPipelinesRecursive(TaskInfo task) {
        Set<TaskInfo> pipelines = new HashSet<TaskInfo>();
        return getDependentPipelinesRecursive(task, pipelines);
    }
    
    private Set<TaskInfo> getDependentPipelinesRecursive(TaskInfo task, Set<TaskInfo> pipelines) {
        Set<TaskInfo> latest = getDependentPipelines(task);
        pipelines.addAll(latest);
        
        for (TaskInfo pipeline : latest) {
            pipelines = getDependenciesRecursive(pipeline, pipelines);
        }
        
        return pipelines;
    }
    
    public Set<TaskInfo> getDependencies(TaskInfo pipeline) {
        return pipelineToDependencies.get(pipeline);
    }
    
    public Set<TaskInfo> getDependencies(PipelineModel pipeline) {
        TaskInfo task = TaskInfoCache.instance().getTask(pipeline.getLsid());
        return getDependencies(task);
    }
    
    public Set<TaskInfo> getDependenciesRecursive(PipelineModel pipeline) {
        TaskInfo task = TaskInfoCache.instance().getTask(pipeline.getLsid());
        return getDependenciesRecursive(task);
    }
    
    public Set<TaskInfo> getDependenciesRecursive(TaskInfo pipeline) {
        Set<TaskInfo> dependencies = new HashSet<TaskInfo>();
        return getDependenciesRecursive(pipeline, dependencies);
    }
    
    private Set<TaskInfo> getDependenciesRecursive(TaskInfo pipeline, Set<TaskInfo> dependencies) {
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