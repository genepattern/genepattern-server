/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webapp.rest.api.v1.job.search;

import java.util.Collections;
import java.util.List;
import com.google.common.collect.ImmutableList;
import org.json.JSONException;

import org.genepattern.webservice.JobInfo;
import org.json.JSONArray;
import org.json.JSONObject;

public class SearchResults {
    private final int numItems;

    // the list of JobInfo to display on the current page
    private final List<JobInfo> resultsInPage;
    // page navigation links
    private final PageNav pageNav;
    // pre-built search filters
    private final FilterNav filterNav;
    
    private final long searchDate; //the timestamp 

    public SearchResults(int numItems, List<JobInfo> resultsInPage, PageNav pageNav, FilterNav filterNav, long searchDate) {
        this.numItems = numItems;
        this.resultsInPage = resultsInPage;
        this.pageNav = pageNav;
        this.filterNav = filterNav;
        this.searchDate = searchDate;
    }

    public SearchResults(final Builder in) {
        this(in.numItems, ImmutableList.copyOf( in.jobInfos ),
                new PageNav(in.query, in.numItems),
                new FilterNav(in.query, in.groupIds, in.batchIds),
                in.now);
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
    
        /**
         * Construct JSON representation of the navigation details for a search result.
         * <pre>
          nav: {
            page:
            numPages:
            numItems:
            navLinks: {
                prev: { "rel": "", "name": "", "href": "" }, <-- link
                first: {}, <-- link
                prevItems: [ {}, {}, ..., {} ], <-- links
                current: {}, <-- link
                nextItems: [ {}, {}, ..., {}], <-- links
                last: {},
                next: {} <-- link
            }
            groupIds: [ "", "", ..., ""], <-- list of available groupIds for the currentUser
            batchIds: [ "", "", ... , ""], <-- list of available batchIds for the currentUser
            filterLinks: <-- list of links used to build the filter drop-down menu, e.g. 'all jobs', 'my jobs', 'jobs in group', 'jobs in batch'
                [ {}, {}, ..., {} ] <-- links
          }
         </pre>
         *
         *
         */    public JSONObject navigationDetailsToJson() throws JSONException {

        JSONObject nav=new JSONObject();
        nav.put("page", getPageNav().getCurrent().getPage());
        nav.put("numPages", getPageNav().getNumPages());
        nav.put("numItems", getPageNav().getNumItems());
        JSONObject navLinks=getPageNav().navLinksToJson();
        nav.put("navLinks", navLinks);

        // include batchIds
        if (getFilterNav().getBatchIds().size()>0) {
            JSONArray batchIds=new JSONArray();
            for(final String batchId_entry : getFilterNav().getBatchIds()) {
                batchIds.put(batchId_entry);
            }
            nav.put("batchIds", batchIds);
        }
        // include groupIds
        if (getFilterNav().getGroupIds().size()>0) {
            JSONArray groupIds=new JSONArray();
            for(final String groupId_entry : getFilterNav().getGroupIds()) {
                groupIds.put(groupId_entry);
            }
            nav.put("groupIds", groupIds);
        }
        // include filter links
        if (getFilterNav().getFilterLinks().size()>0) {
            JSONArray filterLinks=new JSONArray();
            for(final QueryLink link : getFilterNav().getFilterLinks()) {
                filterLinks.put( link.toJson() );
            }
            nav.put("filterLinks", filterLinks);
        }
        return nav;
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
