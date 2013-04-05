package org.genepattern.server.rest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Data structure returned after posting a job or batch of jobs.
 * @author pcarr
 *
 */
public class JobReceipt {
    private List<String> jobIds=new ArrayList<String>();
    private String batchId="";
    
    public JobReceipt() {
    }
    
    public void setBatchId(final String batchId) {
        this.batchId=batchId;
    }
    
    public void addJobId(final String jobId) {
        jobIds.add(jobId);
    }
    
    public List<String> getJobIds() {
        return Collections.unmodifiableList(jobIds);
    }
    
    public String getBatchId() {
        return batchId;
    }

}
