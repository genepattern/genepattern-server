/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.server.webservice.server.local;

import org.apache.log4j.Logger;
import org.genepattern.server.webservice.server.Analysis;
import org.genepattern.server.webservice.server.ProvenanceFinder;
import org.genepattern.server.webservice.server.ProvenanceFinder.ProvenancePipelineResult;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.WebServiceException;

/**
 * local Analysis client
 * 
 * @author Joshua Gould
 */
public class LocalAnalysisClient {
    private static Logger log = Logger.getLogger(LocalAnalysisClient.class);

    private Analysis service;
    private String userName;

    public LocalAnalysisClient(final String userName) {
        this.userName = userName;
        service = new Analysis() {
            @Override
            protected String getUsernameFromContext() {
                return userName;
            }
        };
    }

    public void deleteJobResultFile(int jobId, String value) throws WebServiceException {
        service.deleteJobResultFile(jobId, value);
    }

    public ProvenancePipelineResult createProvenancePipeline(String fileUrlOrJobNumber, String pipelineName) throws WebServiceException {
        ProvenanceFinder pf = new ProvenanceFinder(userName);
        ProvenancePipelineResult result = pf.createProvenancePipeline(fileUrlOrJobNumber, pipelineName);
        return result;
    }

    public JobInfo[] getChildren(int jobNumber) throws WebServiceException {
        return service.getChildJobInfos(jobNumber);
    }
}
