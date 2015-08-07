/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.executor.pipeline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.domain.JobStatus;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;
import org.jgrapht.EdgeFactory;
import org.jgrapht.Graph;
import org.jgrapht.graph.SimpleDirectedGraph;

/**
 * Runtime model of a pipeline, before adding jobs to the queue.
 * 
 * @author pcarr
 */
public class PipelineGraph {
    private static Logger log = Logger.getLogger(PipelineGraph.class);
    
    public static class MyVertex {
        final private String stepId;
        private JobInfo jobInfo=null;

        /**
         * @param stepId
         * @param jobInfo
         */
        public MyVertex(final String stepId, final JobInfo jobInfo) {
            this.stepId=stepId;
            this.jobInfo=jobInfo;
        }
        public String getStepId() {
            return stepId;
        }

        public JobInfo getJobInfo() {
            return jobInfo;
        }
        
        public boolean equals(Object obj) {
            MyVertex v=null;
            if (obj instanceof MyVertex) {
                v=(MyVertex) obj;
            }
            else {
                return false;
            }
            return stepId.equals(v.getStepId());
        }
        
        public int hashCode() {
            return stepId.hashCode();
        }
    }

    public static class MyEdge {
        private MyVertex from;
        private MyVertex to;
        
        public MyEdge(final MyVertex from, final MyVertex to) {
            this.from=from;
            this.to=to;
        }
        
        public MyVertex getFrom() {
            return from;
        }
        
        public MyVertex getTo() {
            return to;
        }
    }
    

    static class MyEdgeFactory implements EdgeFactory<MyVertex, MyEdge> {
        //@Override
        public MyEdge createEdge(MyVertex from, MyVertex to) {
            return new MyEdge(from,to);
        }
    }
    

    //factory methods
    static public PipelineGraph getDependencyGraph(final JobInfo pipelineJobInfo) {
        final boolean isInTransaction=HibernateUtil.isInTransaction();
        //rebuild the graph for the pipeline
        HibernateUtil.beginTransaction();
        try {
            final List<JobInfo> childJobInfos = getChildJobInfos(pipelineJobInfo);
            return getDependencyGraph(pipelineJobInfo, childJobInfos);
        }
        catch (Throwable t) {
            log.error("Error getting dependency graph for pipeline #"+pipelineJobInfo.getJobNumber(), t);
            return new PipelineGraph();
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }

    /**
     * Initialize the job dependency map for a given parent pipeline.
     * It is a map of jobs
     */
    static public PipelineGraph getDependencyGraph(final JobInfo pipelineJobInfo, final List<JobInfo> children) {
        //step 0 is the first child step
        PipelineGraph graph = new PipelineGraph();
        //graph.addStep("-1", ""+pipelineJobInfo.getJobNumber());
        int stepId=-1;
        for(JobInfo toJob : children) {
            ++stepId;
            final String toStep=""+stepId;
            graph.addStep(toStep, toJob);
            Set<String> fromSteps = getDependentSteps(toJob);
            for(String fromStep : fromSteps) {
                graph.addDependency(fromStep, toStep);
            }
        }
        return graph;
    }
    
    private static List<JobInfo> getChildJobInfos(final JobInfo pipelineJobInfo) {
        final int parentJobId = pipelineJobInfo.getJobNumber();
        boolean inTransaction = HibernateUtil.isInTransaction();
        try {
            AnalysisDAO dao = new AnalysisDAO();
            JobInfo[] all = dao.getChildren(parentJobId);
            List<JobInfo> childJobs = new ArrayList<JobInfo>();
            for(JobInfo jobInfo : all) {
                childJobs.add(jobInfo);
            }
            return childJobs;
        }
        catch (Throwable t) {
            log.error("Error getting child jobInfos for pipeline #"+pipelineJobInfo.getJobNumber(), t);
            HibernateUtil.closeCurrentSession();
            return Collections.emptyList();
        }
        finally {
            if (!inTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }

    /**
     * For the given child job in a pipeline, get the list of upstream steps
     * that the job depends on.
     * 
     * A child job 'depends' on another step if it uses the output of that step
     * as input to at least one of its input parameters.
     * 
     * @param child, the jobInfo for a step in a pipeline.
     * @return a set (must be unique) of stepIds, the convention is to use a String
     *      representation of an integer, based on the sequential order in which the child steps are listed
     *      in the manifest for the pipeline. The first step has stepId=0.
     */
    static private Set<String> getDependentSteps(final JobInfo child) {
        Set<String> fromSteps=new HashSet<String>();
        for(ParameterInfo jobParam : child.getParameterInfoArray()) {
            String stepId=getDependentStepId(jobParam);
            if (stepId != null) {
                fromSteps.add(stepId);
            }
            //String inheritFilename = (String) jobParam.getAttributes().get(PipelineModel.INHERIT_FILENAME);
            //String inheritTaskname = (String) jobParam.getAttributes().get(PipelineModel.INHERIT_TASKNAME);
            //if (inheritTaskname != null && inheritTaskname.length()>0) {
            //    fromSteps.add(inheritTaskname);
            //}
        }
        return fromSteps;
    }
    
    /**
     * Get the stepId, if any, for the job which is the source of the input file for the given jobParam.
     * 
     * @param jobParam, an input parameter for a job in the pipeline.
     * @return null if the param does not use an input from another sibling job in the parent pipeline.
     */
    private static String getDependentStepId(final ParameterInfo jobParam) {
        //String inheritFilename = (String) jobParam.getAttributes().get(PipelineModel.INHERIT_FILENAME);
        String inheritTaskname = (String) jobParam.getAttributes().get(PipelineModel.INHERIT_TASKNAME);
        if (inheritTaskname != null && inheritTaskname.length()>0) {
            return inheritTaskname;
        }
        return null;
    }
    
    /** 
     * Map of step_id to job_id for all child steps in the pipeline,
     * when the job_id is null, it means the jobs have not yet been added to the GP internal queue.
     */
    private Map<String,String> jobLookup;
    private Map<String,JobInfo> jobInfoLookup;
    private SimpleDirectedGraph<MyVertex,MyEdge> jobGraph;

    public PipelineGraph() {
        jobGraph = new SimpleDirectedGraph<MyVertex,MyEdge>(new MyEdgeFactory());
        jobLookup = new HashMap<String,String>();
        jobInfoLookup = new HashMap<String,JobInfo>();
    }

    public void addStep(final String stepId, final JobInfo jobInfo) {
        final String jobId=""+jobInfo.getJobNumber();
        log.debug("adding stepId="+stepId+", jobId="+jobId); 
        jobGraph.addVertex(new MyVertex(stepId, jobInfo));
        jobLookup.put("stepId="+stepId, jobId);
        jobLookup.put("jobId="+jobId, stepId);
        jobInfoLookup.put(jobId, jobInfo);
    }
    
    private JobInfo getJobInfoFromStep(final String stepId) {
        JobInfo jobInfo=null;
        String jobId=jobLookup.get("stepId="+stepId);
        if (jobId != null) {
            jobInfo = jobInfoLookup.get(jobId);
        }
        if (jobInfo==null) {
            throw new IllegalArgumentException("no jobInfo for stepId="+stepId+", jobId="+jobId);
        }
        return jobInfo;
    }

    //tail waits for head to complete
    public void addDependency(final String fromStep, final String toStep) {
        log.debug("adding dependency, fromStep="+fromStep+", toStep="+toStep);
        JobInfo fromJob=getJobInfoFromStep(fromStep);
        JobInfo toJob=getJobInfoFromStep(toStep);
        MyVertex from = new MyVertex(fromStep, fromJob);
        MyVertex to = new MyVertex(toStep, toJob);

        jobGraph.addEdge(from, to);
    }

    public Graph<MyVertex,MyEdge> getGraph() {
        return jobGraph;
    }
    
    /**
     * Get the set of zero or more jobs that are ready to be started.
     * A job is ready to be started if all of the upstream jobs it depends on have completed.
     * 
     * TODO: not sure what to do with jobs which have completed, but with an ERROR status.
     * 
     * @return
     */
    public Set<JobInfo> getJobsToRun() {
        Set<JobInfo> jobsToRun=new HashSet<JobInfo>();
        for(MyVertex v : jobGraph.vertexSet()) {
            JobInfo toJobInfo=v.getJobInfo();
            if (!isPending(toJobInfo)) {
                //the target job has already been started, don't start it again
            }
            else {
                boolean isReady=true;
                for(MyEdge e : jobGraph.incomingEdgesOf(v)) {
                    JobInfo fromJobInfo=e.getFrom().getJobInfo();
                    boolean isFinished=isFinished(fromJobInfo);
                    if (!isFinished) {
                        isReady=false;
                        break;
                    }
                }
                if (isReady) {
                    jobsToRun.add(toJobInfo);
                }
            }
        }
        return jobsToRun;
    }

    /**
     * Check to see if we are waiting for any jobs to finish, 
     * if not, return 'true', which means, all of the steps in the pipeline are finished.
     * @return
     */
    public boolean allStepsComplete() {
        for(MyVertex v : jobGraph.vertexSet()) {
            JobInfo childJob=v.getJobInfo();
            boolean isFinished=isFinished(childJob);
            if (!isFinished) {
                //at least one job has not finished
                return false;
            }
        }
        //all jobs are finished
        return true;
    }
    
    /**
     * Yet another implementation of the rule for determining if a job
     * is waiting to be added to the queue.
     * 
     * @param jobInfo
     * @return true, if the job has not yet been added to the queue,
     *     presumably because it is waiting for an upstream step.
     */
    private static boolean isPending(final JobInfo jobInfo) {
        if (jobInfo==null) {
            throw new IllegalArgumentException("jobInfo==null");
        }
        if (jobInfo.getStatus()==null || jobInfo.getStatus().length()==0) {
            throw new IllegalArgumentException("jobInfo.status not set");
        }
        return JobStatus.PENDING.equals(jobInfo.getStatus());
    }

    /**
     * Yet another implementation of the rule for determining if a job is complete.
     * 
     * @param jobInfo
     * @return
     */
    private static boolean isFinished(final JobInfo jobInfo) {
        if (jobInfo==null) {
            throw new IllegalArgumentException("jobInfo==null");
        }
        if (jobInfo.getStatus()==null || jobInfo.getStatus().length()==0) {
            throw new IllegalArgumentException("jobInfo.status not set");
        }
        return JobStatus.FINISHED.equals(jobInfo.getStatus()) ||
                JobStatus.ERROR.equals(jobInfo.getStatus());
    }
    
    /**
     * Get the set of zero or more jobs that this given job depends on.
     * 
     * @param toJobInfo
     * @return
     */
    public Set<JobInfo> getDependentJobs(JobInfo toJobInfo) {
        final String toJobId=""+toJobInfo.getJobNumber();
        log.debug("job #"+toJobId+" is waiting for jobs: ");
        String toStepId = jobLookup.get("jobId="+toJobId);
        MyVertex toStepVertex=new MyVertex(toStepId, toJobInfo);
        Set<MyEdge> edges = jobGraph.incomingEdgesOf(toStepVertex);
        if (edges==null || edges.size()==0) {
            return Collections.emptySet();
        }
        Set<String> fromJobIds = new HashSet<String>();
        for(MyEdge edge : edges) {
            MyVertex from = edge.getFrom();
            String fromStepId=from.getStepId();
            String fromJobId=jobLookup.get("stepId="+fromStepId);
            log.debug("    fromStepId="+fromStepId+", fromJobId="+fromJobId);
            if (fromJobId==null) {
                log.error("fromJobId==null, for stepId="+fromStepId);
            }
            else {
                fromJobIds.add(fromJobId);
            }
        }
        if (fromJobIds.size()==0) {
            return Collections.emptySet();
        }
        Set<JobInfo> fromJobInfos=new HashSet<JobInfo>();
        for(String jobId : fromJobIds) {
            JobInfo fromJobInfo=jobInfoLookup.get(jobId);
            if (fromJobInfo!=null) {
                fromJobInfos.add(fromJobInfo);
            }
        }
        return fromJobInfos;
    }

}
