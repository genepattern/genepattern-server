package org.genepattern.server.webapp.rest.api.v1.job.search;

import java.util.List;
import com.google.common.collect.ImmutableList;
import org.genepattern.webservice.JobInfo;

public class SearchResults {
    private final int numItems;
    // the list of JobInfo to display on the current page
    private final List<JobInfo> resultsInPage;
    // page navigation links
    private final PageNav pageLinks;
    private final long searchDate; //the timestamp 

    public SearchResults(final Builder in) {
        this.numItems=in.numItems;
        this.resultsInPage=ImmutableList.copyOf( in.jobInfos );
        this.searchDate=in.now;
        this.pageLinks=new PageNav(in.query, numItems);
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

    public PageNav getPageLinks() {
        return pageLinks;
    }

    public static class Builder {
        private final SearchQuery query;
        private int numItems;
        private List<JobInfo> jobInfos;
        private final long now=System.currentTimeMillis(); //approximate date of the search
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

        public SearchResults build() {
            return new SearchResults(this);
        }
    }
}
