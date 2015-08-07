/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.eula;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.data.pipeline.PipelineModelException;
import org.genepattern.server.webservice.server.local.IAdminClient;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.webservice.SuiteInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;
import org.genepattern.webservice.WebServiceException;
import org.xml.sax.InputSource;

/**
 * Recursively get all EulaInfo for the given task.
 * This method checks for licensed modules in pipelines.
 * 
 * @author pcarr
 */
public class GetEulaFromTaskRecursive {
    final static private Logger log = Logger.getLogger(GetEulaFromTaskRecursive.class);

    static class MyAdminClient implements IAdminClient {
        private GetTaskStrategy myGetTaskStrategy=null;
        public MyAdminClient(final GetTaskStrategy getTaskStrategy) {
            this.myGetTaskStrategy=getTaskStrategy;
        }
        ///CLOVER:OFF  
        //    --> disable clover report for most of this method, we only need to implement one method in the IAdminClient interface
        //@Override
        public List<String> getVersions(LSID lsid) {
            log.error("Not implemented!");
            return null;
        }

        //@Override
        public TaskInfo[] getAllTasksForModuleAdmin() {
            log.error("Not implemented!");
            return null;
        }

        //@Override
        public TaskInfo[] getTasksOwnedBy() {
            log.error("Not implemented!");
            return null;
        }

        //@Override
        public TreeMap<String, TaskInfo> getTaskCatalogByLSID(Collection<TaskInfo> tasks) {
            log.error("Not implemented!");
            return null;
        }

        //@Override
        public TreeMap<String, TaskInfo> getTaskCatalogByLSID() throws WebServiceException {
            log.error("Not implemented!");
            return null;
        }

        //@Override
        public Collection<TaskInfo> getTaskCatalog() throws WebServiceException {
            log.error("Not implemented!");
            return null;
        }

        //@Override
        public Collection<TaskInfo> getLatestTasks() throws WebServiceException {
            log.error("Not implemented!");
            return null;
        }

        //@Override
        public Map<String, Collection<TaskInfo>> getLatestTasksByType() throws WebServiceException {
            log.error("Not implemented!");
            return null;
        }
        ///CLOVER:ON
        //@Override
        public TaskInfo getTask(final String lsid) throws WebServiceException {
            if (myGetTaskStrategy != null) {
                return myGetTaskStrategy.getTaskInfo(lsid);
            }
            //otherwise error
            throw new WebServiceException("myGetTaskStrategy is not initialized");
        }
        ///CLOVER:OFF
        //@Override
        public SuiteInfo getSuite(String lsid) throws WebServiceException {
            log.error("Not implemented!");
            return null;
        }

        //@Override
        public SuiteInfo[] getAllSuites() throws WebServiceException {
            log.error("Not implemented!");
            return null;
        }

        //@Override
        public String getUserProperty(String key) {
            log.error("Not implemented!");
            return null;
        }
        ///CLOVER:ON
    }
    
    private GetEulaFromTask getEulaFromTask = null;
    public void setGetEulaFromTask(GetEulaFromTask impl) {
        this.getEulaFromTask=impl;
    }

    private GetTaskStrategy getTaskStrategy = null;
    public void setGetTaskStrategy(GetTaskStrategy impl) {
        this.getTaskStrategy=impl;
    }

    private IAdminClient getAdminClient() {
        if (getTaskStrategy==null) {
            getTaskStrategy=new GetTaskStrategyDefault();
        }
        MyAdminClient adminClient=new MyAdminClient(getTaskStrategy);
        return adminClient;
    }
  
    public SortedSet<EulaInfo> getEulasFromTask(TaskInfo taskInfo) {
        SortedSet<EulaInfo> eulas = appendEulaInfo(null, taskInfo);
        return eulas;
    }
    
    //recursive implementation
    private SortedSet<EulaInfo> appendEulaInfo(SortedSet<EulaInfo> eulas, TaskInfo taskInfo) {
        if (taskInfo==null) {
            log.error("taskInfo==null");
            throw new IllegalArgumentException("taskInfo==null");
        }
        if (getEulaFromTask==null) {
            log.error("Initialization error, getEulaFromTask==null");
            throw new IllegalArgumentException("Initialization error, getEulaFromTask==null");
        }
        List<EulaInfo> eulaObjs = getEulaFromTask.getEulasFromTask(taskInfo);
        if (eulas==null) {
            eulas=new TreeSet<EulaInfo>(EulaInfo.defaultComparator(taskInfo));
        }
        eulas.addAll(eulaObjs);
        
        if (taskInfo.isPipeline()) {
            List<TaskInfo> children = getChildren(taskInfo);
            for(TaskInfo child : children) {
                appendEulaInfo(eulas, child);
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
            pipelineModel=initPipelineModel(parent);
            pipelineModel.setLsid(parent.getLsid());
        }
        catch (Throwable t) {
            log.error("Error initializing PipelineModel for task, lsid="+parent.getLsid(), t);
            return Collections.emptyList();
        }

        List<TaskInfo> children = new ArrayList<TaskInfo>();
        for(JobSubmission jobSubmission : pipelineModel.getTasks()) {
            TaskInfo child=jobSubmission.getTaskInfo();
            //can return null, if the task is not installed on the server
            //    ignore the null children
            if (child == null) {
                log.debug("jobSubmission.taskInfo is null, task is not installed on server, lsid="+jobSubmission.getLSID());
            }
            if (child != null) {
                children.add(child);
            }
        }
        return children;
    }

    // initial implementation, worked fine, but with too many dependencies on the underlying database
    // instead, optionally use a custom IAdminClient for initializing taskInfo instances
    //private PipelineModel initPipelineModelImpl01(TaskInfo parent) throws PipelineModelException {
    //    PipelineModel pipelineModel=null;
    //    pipelineModel=PipelineUtil.getPipelineModel(parent);
    //    return pipelineModel;
    //}

    private PipelineModel initPipelineModel(final TaskInfo taskInfo) throws Exception {
        IAdminClient adminClient=getAdminClient();
        return initPipelineModel(taskInfo,adminClient);
    }
    
    //TODO: consider moving this method into the PipelineUtil class
    private PipelineModel initPipelineModel(final TaskInfo taskInfo, final IAdminClient adminClient) throws Exception {
        InputSource inputXmlSource;
        boolean verify=false;
        if (taskInfo == null) {
            throw new IllegalArgumentException("taskInfo is null");
        }
        TaskInfoAttributes tia = taskInfo.giveTaskInfoAttributes();
        String serializedModel = (String) tia.get(GPConstants.SERIALIZED_MODEL);
        if (serializedModel == null || serializedModel.length() == 0) {
            throw new PipelineModelException("Missing "+GPConstants.SERIALIZED_MODEL+" for taskInfo.ID="+taskInfo.getID()+", taskInfo.name="+taskInfo.getName());
        }
        PipelineModel model = null;
        try {
            StringReader stringReader = new StringReader(serializedModel);
            inputXmlSource=new InputSource(stringReader);
            model = PipelineModel.toPipelineModel(inputXmlSource, verify, adminClient);
            model.setLsid(taskInfo.getLsid());
        } 
        catch (Throwable t) {
            throw new PipelineModelException(t);
        }
        return model;
    }

}
