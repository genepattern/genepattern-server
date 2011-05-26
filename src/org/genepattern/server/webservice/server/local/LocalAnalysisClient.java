/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2011) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.
 
 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.webservice.server.local;

import org.apache.log4j.Logger;
import org.genepattern.server.webservice.server.Analysis;
import org.genepattern.server.webservice.server.ProvenanceFinder.ProvidencePipelineResult;
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

    public ProvidencePipelineResult createProvenancePipeline(String fileUrlOrJobNumber, String pipelineName) throws WebServiceException {
        return service.createProvenancePipeline(fileUrlOrJobNumber, pipelineName);
    }

    public JobInfo[] getChildren(int jobNumber) throws WebServiceException {
        return service.getChildJobInfos(jobNumber);
    }
}
