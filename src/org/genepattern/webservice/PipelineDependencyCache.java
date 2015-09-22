/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.webservice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.data.pipeline.PipelineModelException;
import org.genepattern.data.pipeline.PipelineUtil;
import org.genepattern.server.TaskLSIDNotFoundException;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;


/**
 * Experimental class developed to be integrated into the TaskInfoCache so that we can quickly
 * lookup all installed pipelines which use (aka depend on) a given module.
 * @author pcarr
 *
 */
public class PipelineDependencyCache {
    final static private Logger log = Logger.getLogger(PipelineDependencyCache.class);
    
    final static public String PROP_ENABLE="pipelineDependencyCache.enable";
    /** @deprecated pass in a GpConfig */
    public static boolean isEnabled(final GpContext userContext) {
        return isEnabled(ServerConfigurationFactory.instance(), userContext);
    }
    public static boolean isEnabled(final GpConfig gpConfig, final GpContext userContext) {
        boolean enabled = gpConfig.getGPBooleanProperty(userContext, PROP_ENABLE, false);
        return enabled;
    }
    
    public static PipelineDependencyCache instance() {
        return CachedImpl.INSTANCE;
    }
    
    private static class CachedImpl {
        private static final PipelineDependencyCache INSTANCE = new PipelineDependencyCache();
    }

    private static class MyMultiMap {
        final private Map<String,List<String>> lookup;
        public MyMultiMap() {
            lookup=new HashMap<String,List<String>>();
        }
        
        public void add(final String key, final String value) {
            List<String> values=lookup.get(key);
            if (values==null) {
                values=new ArrayList<String>();
                lookup.put(key, values);
            }
            values.add(value);
        }
        
        public List<String> remove(final String key) {
            return lookup.remove(key);
        }
        
        public List<String> get(final String key) {
            List<String> rval=lookup.get(key);
            if (rval==null) {
                return Collections.emptyList();
            }
            return rval;
        }
        
        public void clear() {
            lookup.clear();
        }
    }
    
    //a map of parentLsid -> List if childLsid
    final private MyMultiMap childLsidLookup;    
    //a map of childLsid -> List of parentLsid
    final private MyMultiMap parentLsidLookup;

    //force singleton
    private PipelineDependencyCache() {
        childLsidLookup=new MyMultiMap();
        parentLsidLookup=new MyMultiMap();
    }
    
    public void clear() {
        childLsidLookup.clear();
        parentLsidLookup.clear();
    }
    
    /**
     * Get the set of installed pipelines which directly include the given task.
     * 
     * @param taskInfo
     * @return an empty set if the given task is not included in any installed pipelines.
     */
    public Set<TaskInfo> getParentPipelines(final TaskInfo childTaskInfo) {
        if (childTaskInfo==null) {
            log.debug("ignoring null childTaskInfo");
            return Collections.emptySet();
        }
        if (childTaskInfo.getLsid()==null || childTaskInfo.getLsid().length()==0) {
            log.debug("ignoring, childTaskInfo.lsid not set");
            return Collections.emptySet();
        }
        final List<String> parentLsids=parentLsidLookup.get(childTaskInfo.getLsid());
        if (parentLsids == null) {
            log.error("parentLsids is null, for childTaskInfo="+childTaskInfo.getName()+", "+childTaskInfo.getLsid());
            return Collections.emptySet();
        }
        if (parentLsids.size()==0) {
            //can happen often, no need to create a new HashSet
            return Collections.emptySet();
        }
        
        final Set<TaskInfo> parentPipelines=new HashSet<TaskInfo>();
        for(final String parentLsid : parentLsids) {
            final TaskInfo parentTaskInfo; 
            try {
                parentTaskInfo=TaskInfoCache.instance().getTask(parentLsid);
                if (parentTaskInfo != null) {
                    log.debug("found parent task: "+parentTaskInfo.getName()+", "+parentTaskInfo.getLsid());
                    parentPipelines.add(parentTaskInfo);
                }
            }
            catch (TaskLSIDNotFoundException e) {
                log.debug("missing task, lsid="+parentLsid);
            }
            catch (Throwable t) {
                log.error("unexpected exception in TaskInfoCache.getTask("+parentLsid+")", t);
            }
        }
        return parentPipelines;
    }

    //
    // the following code is a preliminary implementation which supports getting 
    // all calling pipelines, accounting for nested steps
    //
    
    /**
     * Get the list of all pipelines which include the given task,
     * search through all pipeline dependencies.
     * 
     * @param taskInfo
     * @return
     */
    public Set<TaskInfo> getAllReferringPipelines(final TaskInfo taskInfo) {
        Set<TaskInfo> callers=appendCallers(null, taskInfo, 0);
        return callers;
    }
    
    private Set<TaskInfo> appendCallers(Set<TaskInfo> toSet, final TaskInfo callee, final int cycleCheck) {
        if (cycleCheck > 1000) {
            log.error("Too many iterations, "+cycleCheck+", while checking for pipeline dependencies.");
            return Collections.emptySet();
        }
        
        if (toSet==null) {
            toSet=new HashSet<TaskInfo>();
        }
        if (callee==null) {
            //TODO: log error
            return toSet;
        }
        
        Set<TaskInfo> parents=getParentPipelines(callee);
        Set<TaskInfo> addedInThisIteration=new HashSet<TaskInfo>();
        for(TaskInfo parent : parents) {
            //self-reference
            if (callee.getLsid().equals(parent.getLsid())) {
                log.error("ignoring self in self-referential pipeline, lsid="+callee.getLsid());
            }
            else if (toSet.contains(parent)) {
            }
            else {
                toSet.add(parent);
                addedInThisIteration.add(parent);
            }
        }
        
        //recursive check
        for(final TaskInfo parent : addedInThisIteration) {
            appendCallers(toSet, parent, cycleCheck+1);
        }
        return toSet;
    }
    
    public void addTask(final TaskInfo taskToAdd) {
        if (taskToAdd==null) {
            throw new IllegalArgumentException("taskToAdd==null");
        }
        if (taskToAdd.isPipeline()) {
            final String parentLsid=taskToAdd.getLsid();
            final PipelineModel pipelineModel;
            try {
                pipelineModel=PipelineUtil.getPipelineModel(taskToAdd);
            }
            catch (PipelineModelException e) {
                log.error("error initializing PipelineModel for "+taskToAdd.getName()+", "+taskToAdd.getLsid(), e);
                return;
            }
            for(final JobSubmission step : pipelineModel.getTasks()) {
                final String childLsid=step.getLSID();
                addTuple(parentLsid, childLsid);
            }
        }
    }
    
    public void removeTask(final String taskLsid) {
        if (taskLsid==null) {
            return;
        }
        removeRecord(taskLsid);
    }

    
    /**
     * Add parent-child relationship to the lookup table(s).
     * 
     * @param parentLsid, the lsid for a parent pipeline
     * @param childLsid, the lsid for a child step in the parent pipeline
     */
    private void addTuple(final String parentLsid, final String childLsid) {
        parentLsidLookup.add(childLsid, parentLsid);
        childLsidLookup.add(parentLsid, childLsid);
    }
    
    private void removeRecord(final String parentLsid) {
        if (parentLsid==null) {
            return;
        }
        childLsidLookup.remove(parentLsid);
    }
    

}
