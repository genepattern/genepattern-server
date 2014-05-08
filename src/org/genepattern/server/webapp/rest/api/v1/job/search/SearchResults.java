package org.genepattern.server.webapp.rest.api.v1.job.search;

import java.util.Collections;
import java.util.List;
import com.google.common.collect.ImmutableList;
import org.genepattern.webservice.JobInfo;

public class SearchResults {
    private final int numItems;
    // the list of JobInfo to display on the current page
    private final List<JobInfo> resultsInPage;
    // page navigation links
    private final PageNav pageNav;
    // pre-built search filters
    private final FilterNav filterNav;
    
    private final long searchDate; //the timestamp 

    public SearchResults(final Builder in) {
        this.numItems=in.numItems;
        this.resultsInPage=ImmutableList.copyOf( in.jobInfos );
        this.searchDate=in.now;
        this.pageNav=new PageNav(in.query, numItems);
        this.filterNav=new FilterNav(in.groupIds, in.batchIds);
    }

    public int getNumResults() {
        if (resultsInPage==null) {
            return 0;
        }
        return resultsInPage.size();
    }

    public List<JobInfo> getJobInfos() {
        return resultsInPage;
    }

    public PageNav getPageNav() {
        return pageNav;
    }
    
    public FilterNav getFilterNav() {
        return filterNav;
    }

    public static class Builder {
        private final SearchQuery query;
        private int numItems;
        private List<JobInfo> jobInfos;
        private final long now=System.currentTimeMillis(); //approximate date of the search
        private List<String> groupIds;
        private List<String> batchIds;
        
        public Builder(final SearchQuery query) {
            this.query=query;
        }
        public Builder numItems(final int numItems) {
            this.numItems=numItems;
            return this;
        }
        public Builder jobInfos(final List<JobInfo> jobInfos) {
            this.jobInfos=jobInfos;
            return this;
        }
        public Builder groupIds(final List<String> groupIds) {
            this.groupIds=groupIds;
            return this;
        }
        public Builder batchIds(final List<String> batchIds) {
            this.batchIds=batchIds;
            return this;
        }

        public SearchResults build() {
            if (groupIds==null) {
                groupIds=Collections.emptyList();
            }
            if (batchIds==null) {
                batchIds=Collections.emptyList();
            }
            return new SearchResults(this);
        }
    }
}
