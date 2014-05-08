package org.genepattern.server.webapp.rest.api.v1.job.search;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.auth.GroupPermission;
import org.genepattern.server.webapp.rest.api.v1.Rel;
import org.genepattern.server.webapp.rest.api.v1.job.search.SearchQuery.QueryLink;
import org.genepattern.server.webapp.rest.api.v1.job.search.SearchQuery.QueryParam;

import com.google.common.collect.ImmutableList;

/**
 * Helper class for building the drop-down menu on the job search page.
 * These elements will be included in the top-level 'nav' element of the job search.
 * <pre>
   filterLinks: [ {}, {}, ... , {} ], <-- list of links
   groupIds: [ "", "", ..., "" ], <-- list of groups for the current user
   batchIds: [ "", "", ..., "" ] <-- list of batchIds for the current user
 * </pre>
 * 
 * @author pcarr
 *
 */
public class FilterNav {
    private static final Logger log = Logger.getLogger(FilterNav.class);

    private final List<String> groupIds;
    private final List<String> batchIds;
    private final List<QueryLink> filterLinks;

    public FilterNav(final SearchQuery q, final List<String> groupIds, final List<String> batchIds) {
        this.groupIds=groupIds;
        this.batchIds=batchIds;
        this.filterLinks=ImmutableList.copyOf( initFilterLinks(q) );
    }
    
    private List<QueryLink> initFilterLinks(final SearchQuery q) {
        List<QueryLink> filters=new ArrayList<QueryLink>();
        try {
            // first item is 'My job results'
            filters.add(new QueryLink(q, "My job results", Rel.related, 
                    new QueryParam(SearchQuery.Q_USER_ID, q.getCurrentUser())));
            // next item is 'All job results'
            filters.add(new QueryLink(q, "All job results", Rel.related, 
                    new QueryParam(SearchQuery.Q_USER_ID, "*")));

            //add groups, if necessary
            for(final String groupId : groupIds) {
                if (groupId.endsWith(GroupPermission.PUBLIC)) {
                    // special-case for 'Public job results' 
                    filters.add(new QueryLink(q, "Public job results", Rel.related, 
                            new QueryParam(SearchQuery.Q_GROUP_ID, GroupPermission.PUBLIC))); // "*"
                }
                else {
                    filters.add(new QueryLink(q, "In group: "+groupId, Rel.related, 
                            new QueryParam(SearchQuery.Q_GROUP_ID, groupId)));
                }
            }

            // add batch jobs, if necessary
            for(final String batchId : batchIds) {
                filters.add(new QueryLink(q, "Batch: "+batchId, Rel.related, 
                        new QueryParam(SearchQuery.Q_BATCH_ID, batchId)));

            }
        }
        catch (Throwable t) {
            log.error("Unexpected error initializing filterLinks", t);
        }
        return filters;
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
    
    /**
     * Get filter links, kind of like the pageNav class,
     * return a list of links which can be used to generate a drop-down menu for filtering
     * based on groupId or batchId.
     */
    public List<QueryLink> getFilterLinks() {
        return filterLinks;
    }
    
}
