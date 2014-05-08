package org.genepattern.server.webapp.rest.api.v1.job.search;

import java.util.List;

/**
 * Helper class for building the drop-down menu on the job search page.
 *             filterLinks: {
                all: {}, <-- link
                my: {}, <-- link 
                byBatch: [ {}, {}, ..., {}], <-- links   
                byGroup: [ {}, {}, ..., {}] <-- links
            }

 * @author pcarr
 *
 */
public class FilterNav {
    private final List<String> groupIds;
    private final List<String> batchIds;

    public FilterNav(final List<String> groupIds, final List<String> batchIds) {
        this.groupIds=groupIds;
        this.batchIds=batchIds;
    }
    
    /**
     * Get the list of batchIds for the current user.
     * Each entry in the list is a valid value for the 'batchId' query parameter.
     * These values are not encoded.
     * 
     * @return
     */
    public List<String> getBatchIds() {
        return batchIds;
    }
    
    /**
     * Get the list of groupIds for the current user.
     * Each entry in the list is a valid value for the 'groupId' query parameter.
     * These values are not encoded.
     * 
     * @return
     */
    public List<String> getGroupIds() {
        return groupIds;
    }
    
    //private JobSearchLink all;
    //private JobSearchLink my;
    //private List<JobSearchLink> batchIds;
    //private List<JobSearchLink> groupIds;
    //private List<JobSearchLink> allLinks;
    //private FilterNav(final Builder b) {
    //}
    //public static final class Builder {
    //    
    //}

}
