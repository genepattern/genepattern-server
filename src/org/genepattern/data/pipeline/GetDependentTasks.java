package org.genepattern.data.pipeline;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.eula.GetTaskStrategy;
import org.genepattern.server.eula.GetTaskStrategyDefault;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;

/**
 * Helper class for checking for module dependencies before running a task.
 * It recursively initializes a set of all required TaskInfo which are needed
 * to run a given pipeline.
 * It also initilizes a set of missing task LSIDs, for those tasks referenced by the 
 * pipeline and which are not installed.
 * 
 * Recursively get all dependent tasks for the given task.
 * 
 * @author pcarr
 */
public class GetDependentTasks {
    final static private Logger log = Logger.getLogger(GetDependentTasks.class);
    
    final private Context userContext;
    final private GetTaskStrategy getTaskStrategy;
    final private Set<TaskInfo> dependentTasks;
    final private Set<LSID> missingTaskLsids;
    final private Set<TaskInfo> insufficientPermissions;
    
    //so that we don't visit the same pipeline more than once
    final private Set<String> visitedLsids;

    public GetDependentTasks(final Context userContext, final TaskInfo forTask) {
        this(userContext, forTask, null);
    }

    public GetDependentTasks(final Context userContext, final TaskInfo forTask, final GetTaskStrategy getTaskStrategyIn) {
        this.userContext=userContext;
        if (forTask==null) {
            throw new IllegalArgumentException("forTask==null");
        }
        if (getTaskStrategyIn!=null) {
            this.getTaskStrategy=getTaskStrategyIn;
        }
        else {
            this.getTaskStrategy=new GetTaskStrategyDefault();
        }
        
        //do all the work in the constructor
        dependentTasks=new LinkedHashSet<TaskInfo>();
        missingTaskLsids=new LinkedHashSet<LSID>();
        insufficientPermissions=new LinkedHashSet<TaskInfo>();
        visitedLsids=new HashSet<String>();
        
        
        if (forTask.isPipeline()) {
            visitChildren(forTask);
        }
        
        //check for permissions on all dependent tasks
        for(final TaskInfo dependentTask : dependentTasks) {
            if (!canRun(dependentTask)) {
                insufficientPermissions.add(dependentTask);
            }
        }
    }
        
    private void visitChildren(final TaskInfo taskInfo) {
        if (taskInfo==null) {
            log.error("taskInfo==null");
            return;
        }
        if (!taskInfo.isPipeline()) {
            log.error("taskInfo is not a pipeline");
            return;
        }
        
        if (visitedLsids.contains(taskInfo.getLsid())) {
            //skip
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("visiting "+taskInfo.getName()+", "+taskInfo.getLsid());
        }
        visitedLsids.add(taskInfo.getLsid());

        PipelineModel pipelineModel=null;
        try {
            pipelineModel=initPipelineModel(taskInfo);
            pipelineModel.setLsid(taskInfo.getLsid());
        }
        catch (Throwable t) {
            log.error("Error initializing PipelineModel for task, lsid="+taskInfo.getLsid(), t);
            return;
        }
        
        final List<TaskInfo> children = new ArrayList<TaskInfo>();
        
        //breadth first traversal of all child pipelines
        for(final JobSubmission jobSubmission : pipelineModel.getTasks()) {
            final String lsid=jobSubmission.getLSID();
            TaskInfo dependantTask=null;
            try {
                dependantTask=getTaskStrategy.getTaskInfo(lsid);
            }
            catch (Throwable t) {
                //ignore exceptions, if we can't load the task, call it a missing task
            }
            // if the task is not installed on the server, dependantTask will be set to null
            if (dependantTask == null) {
                log.debug("task is not installed on server, lsid="+jobSubmission.getLSID());
                try {
                    final LSID missingTaskLsid = new LSID(lsid);
                    missingTaskLsids.add(missingTaskLsid);
                }
                catch (MalformedURLException e) {
                    log.error(e);
                }
            }
            if (dependantTask != null) {
                boolean added=dependentTasks.add(dependantTask);
                if (added) {
                    //only need to traverse children once per unique lsid
                    //added is only true after the first time we add a dependentTask
                    children.add(dependantTask); 
                    //check for user permissions, can the current user run the task?
                    if (!canRun(dependantTask)) {
                        insufficientPermissions.add(dependantTask);
                    }
                }
            }
        }

        //now visit each of the child pipelines
        for(final TaskInfo child : children) {
            if (child.isPipeline()) {
                visitChildren(child);
            }
        }
    }

    /**
     * Get the set of LSIDs for tasks which are in the pipeline or one of the installed nested pipelines,
     * but which are not installed on this server.
     * 
     * @return
     */
    public Set<LSID> getMissingTaskLsids() {
        return Collections.unmodifiableSet( missingTaskLsids );
    }

    /**
     * Get the set of LSIDs for tasks whcih are in the pipeline or one of the installed nested pipelines,
     * but which the current user does not have sufficient privileges to run.
     * 
     * @return
     */
    public Set<TaskInfo> getPrivateTasks() {
        return Collections.unmodifiableSet( insufficientPermissions );
    }
    
    /**
     * Get the set of installed tasks which depend on the given task.
     * Don't include the given task in the returned set.
     * 
     * Example usage:
     * 1) Before you are about to run the task, get the list of installed tasks so that you can
     *    check for required license agreements (eula).
     * 2) Before you are about to delete the task, alert the end-user that there are installed
     *    tasks which depend on this task.
     * 
     * @param taskInfo
     * @return
     */
    public Set<TaskInfo> getInstalledDependentTasks(final TaskInfo taskInfo) {
        return Collections.unmodifiableSet( dependentTasks );
    }    

    /**
     * Rule for whether the current user has permission to execute the given task.
     * 
     * @param taskInfo
     * @return
     */
    private boolean canRun(final TaskInfo taskInfo) {
        if (userContext.isAdmin()) {
            return true;
        }
        boolean isPublic =  taskInfo.getAccessId() == GPConstants.ACCESS_PUBLIC;
        if (isPublic) {
            return true;
        }
        final String taskOwner = taskInfo.getUserId();
        if (taskOwner == null) {
            log.error("taskOwner==null, for "+taskInfo.getName()+", "+taskInfo.getLsid());
            return false;
        }
        return taskOwner.equals( userContext.getUserId() );
    }

    /**
     * Initialize a PipelineModel object for the given taskInfo.
     * Copied from PipelineHandler.
     * 
     * @param taskInfo
     * @return
     * @throws PipelineModelException
     */
    private static PipelineModel initPipelineModel(final TaskInfo taskInfo) throws PipelineModelException
    {
        if (taskInfo==null) {
            log.error("Unexpected null arg");
            return null;
        }
        if (!taskInfo.isPipeline()) {
            log.debug("task is not a pipeline");
            return null;
        }
        
        final TaskInfoAttributes tia = taskInfo.giveTaskInfoAttributes();
        if (tia == null) {
            throw new PipelineModelException("taskInfo.giveTaskInfoAttributes is null for taskInfo.ID="+taskInfo.getID()+", taskInfo.name="+taskInfo.getName());
        }
        String serializedModel = (String) tia.get(GPConstants.SERIALIZED_MODEL);
        if (serializedModel == null || serializedModel.length() == 0) {
            throw new PipelineModelException("Missing "+GPConstants.SERIALIZED_MODEL+" for taskInfo.ID="+taskInfo.getID()+", taskInfo.name="+taskInfo.getName());
        }
        PipelineModel model = null;
        try {
            model = PipelineModel.toPipelineModel(serializedModel);
        } 
        catch (Throwable t) {
            throw new PipelineModelException(t);
        }
        if (model == null) {
            throw new PipelineModelException("pipeline model is null for taskInfo.ID="+taskInfo.getID()+", taskInfo.name="+taskInfo.getName());
        }
        model.setLsid(taskInfo.getLsid());
        return model;
    }

}
