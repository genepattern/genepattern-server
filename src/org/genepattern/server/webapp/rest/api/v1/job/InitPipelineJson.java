/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webapp.rest.api.v1.job;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.job.DepthFirstJobInfoWalker;
import org.genepattern.server.job.JobInfoVisitor;
import org.genepattern.server.job.JobInfoWalker;
import org.genepattern.webservice.JobInfo;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Generate a JSON representation of a completed (or running) pipeline job.
 * This method recursively visits all steps in the pipeline with the help of the JobInfoWalker
 * and JobInfoVisitor interfaces.
 * 
 * @author pcarr
 *
 */
public class InitPipelineJson implements JobInfoVisitor {
    private static final Logger log = Logger.getLogger(InitPipelineJson.class);

    private final GpContext userContext;
    private final JobInfo jobInfo;
    private final String gpUrl;
    private final String jobsResourcePath;
    private boolean includeSummary=false;
    private final boolean includeOutputFiles;
    private final boolean includeComments;
    private final boolean includeTags;
    private JSONObject jobJson;
    //create an ordered Map of jobId -> JSONObject
    private final Map<Integer,JSONObject> jobMap=new LinkedHashMap<Integer,JSONObject>();
    private final List<GpFilePath> outputFiles=new ArrayList<GpFilePath>();

    public InitPipelineJson(final GpContext userContext, final String gpUrl, final String jobsResourcePath,
                            final JobInfo jobInfo, final boolean includeOutputFiles, final boolean includeComments,
                            final boolean includeTags) {
        this.gpUrl=gpUrl;
        this.jobsResourcePath=jobsResourcePath;
        this.jobInfo=jobInfo;
        this.userContext = userContext;
        this.includeOutputFiles=includeOutputFiles;
        this.includeComments = includeComments;
        this.includeTags = includeTags;
    }
    public void setIncludeSummary(final boolean includeSummary) {
        this.includeSummary=includeSummary;
    }
    public void prepareJsonObject() {
        final JobInfoWalker walker = new DepthFirstJobInfoWalker(jobInfo);
        walker.walk(this);

        //append summary information
        if (includeSummary) {
            JSONObject summary=new JSONObject();
            JSONArray allOutputFiles=new JSONArray();
            for(final GpFilePath outputFile : outputFiles) {
                try {
                    allOutputFiles.put(outputFile.getUrl().toExternalForm());
                }
                catch (Throwable t) {
                    log.error(t);
                }
            }
            try {
                summary.put("numJobs", jobMap.size());
                summary.put("numOutputFiles", outputFiles.size());
                summary.put("allOutputFiles", allOutputFiles);
                jobJson.put("summary", summary);
            }
            catch (JSONException e) {
                log.error(e);
            }
        }
    }

    public JSONObject getJsonObject() {
        return jobJson;
    }

    private JSONObject getOrCreateRecord(final GpContext userContext, final String gpUrl, final JobInfo jobInfo) throws GetJobException {
        if (jobInfo==null) {
            return null;
        }
        JSONObject jobJson=jobMap.get(jobInfo.getJobNumber());
        if (jobJson==null) {
            jobJson=GetPipelineJobLegacy.initJsonObject(gpUrl, jobInfo, includeOutputFiles, includeComments, includeTags);
            jobMap.put(jobInfo.getJobNumber(), jobJson);                
        }
        return jobJson;
    }

    private void addChildRecord(final JSONObject parentJob, final JSONObject childJob) throws JSONException {
        if (parentJob==null) {
            return;
        }
        if (childJob==null) {
            return;
        }
        final JSONObject children;
        final JSONArray items;
        if (parentJob.isNull("children")) {
            children=new JSONObject();
            items=new JSONArray();
            children.put("href", jobsResourcePath+"/"+parentJob.getString("jobId")+"/children");
            children.put("items", items);
            parentJob.put("children", children);
        }
        else {
            children=parentJob.getJSONObject("children");
            items=children.getJSONArray("items");
        }
        items.put(childJob);
    }


    @Override
    public boolean preVisitChildren(JobInfo jobInfo) {
        return true;
    }

    @Override
    public boolean preVisitOutputFiles(JobInfo jobInfo) {
        return true;
    }

    @Override
    public void visitJobInfo(final JobInfo rootJobInfo, final JobInfo parentJobInfo, final JobInfo jobInfo) {
        try {
            final JSONObject rootJsonObj = getOrCreateRecord(userContext, gpUrl, rootJobInfo);
            this.jobJson=rootJsonObj;
            final JSONObject parentJsonObj = getOrCreateRecord(userContext, gpUrl, parentJobInfo);
            final JSONObject jsonObj = getOrCreateRecord(userContext, gpUrl, jobInfo);
            addChildRecord(parentJsonObj, jsonObj);
        }
        catch (GetJobException e) {
            log.error(e);
        }
        catch (JSONException e) {
            log.error(e);
        }
    }

    @Override
    public void visitOutputFile(final JobInfo jobInfo, final GpFilePath outputFile) {
        outputFiles.add(outputFile);
    }

    @Override
    public void postVisitChildren(JobInfo jobInfo) {
    }

    @Override
    public void postVisitOutputFiles(JobInfo jobInfo) {
    }

    @Override
    public void cancelled() {
    }

}
