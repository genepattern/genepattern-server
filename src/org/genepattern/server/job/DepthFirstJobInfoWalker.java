/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.dm.jobresult.JobResultFile;
import org.genepattern.server.executor.pipeline.PipelineHandler;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;

/**
 * Depth-first traversal of all jobs for a given root pipeline job.
 * This loads the information from the ANALYSIS_JOB table in the GP DB,
 * which requires recursive DB calls to build the full tree.
 * 
 * Output files are based on the records in the DB (not in the file system).
 * Hint: they are not saved to the DB until the job is complete, so this method
 * does not give you a partial listing of result files until after a step is complete.
 * 
 * @author pcarr
 *
 */
public final class DepthFirstJobInfoWalker implements JobInfoWalker {
    private static final Logger log = Logger.getLogger(DepthFirstJobInfoWalker.class);

    /**
     * Rule for extracting a GpFilePath from a ParameterInfo.
     * 
     * @param outputParam
     * @return
     */
    private static GpFilePath getFilePath(ParameterInfo outputParam) {
        //circa gp-3.3.3 and earlier, value is of the form, <jobid>/<filepath>, e.g. "1531/Hind_0001.snp"
        GpFilePath gpFilePath = null;
        String pathInfo = "/" + outputParam.getValue();
        try {
            gpFilePath = new JobResultFile(pathInfo);
        }
        catch (Exception e) {
            log.error(e);
        }
        return gpFilePath;
    }

    /**
     * Get the list of child jobs by making a DB query.
     * @param pipelineJobInfo
     * @return
     */
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
    
    private final JobInfo root;
    private boolean cancelled=false;

    public DepthFirstJobInfoWalker(final JobInfo jobInfo) {
        if (jobInfo==null) {
            throw new IllegalArgumentException("jobInfo==null");
        }
        this.root=jobInfo;
    }

    public void cancel() {
        cancelled=true;
    }

    /**
     * Walk through all of the jobs with the given jobId. If the jobId is a pipeline,
     * recursively walk through all of the child jobs in a breadth first manner.
     */
    @Override
    public void walk(JobInfoVisitor v) {
        visitJob(v, null, root);
    }

    private void visitJob(final JobInfoVisitor v, final JobInfo parentJobInfo, final JobInfo jobInfo) {
        if (cancelled) {
            v.cancelled();
            return;
        }
        boolean visitChildren = v.preVisitChildren(jobInfo);
        v.visitJobInfo(root, parentJobInfo, jobInfo);
        boolean visitOutputFiles = v.preVisitOutputFiles(jobInfo);
        if (visitOutputFiles) {
            visitOutputFiles(v, jobInfo);
            v.postVisitOutputFiles(jobInfo);
        }
        if (visitChildren) {
            visitChildren(v, jobInfo);
            v.postVisitChildren(jobInfo);
        }
    }

    private void visitChildren(final JobInfoVisitor v, final JobInfo parentJobInfo) {
        if (cancelled) {
            v.cancelled();
        }
        if (parentJobInfo==null) {
            return;
        }
        //depth-first traversal
        final List<JobInfo> children=getChildJobInfos(parentJobInfo);
        while (children.size()>0) {
            final JobInfo child = children.remove(0);
            visitJob(v, parentJobInfo, child);
        }
    }

    private void visitOutputFiles(final JobInfoVisitor v, final JobInfo jobInfo) {
        final List<ParameterInfo> pinfos=PipelineHandler.getOutputParameterInfos(jobInfo);
        for(final ParameterInfo pinfo : pinfos) {
            GpFilePath outputFilePath=getFilePath(pinfo);
            v.visitOutputFile(jobInfo, outputFilePath);
        }
    }

}