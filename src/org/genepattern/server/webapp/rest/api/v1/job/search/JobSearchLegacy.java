package org.genepattern.server.webapp.rest.api.v1.job.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.genepattern.server.UserAccountManager;
import org.genepattern.server.auth.IGroupMembershipPlugin;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.domain.BatchJobDAO;
import org.genepattern.server.webservice.server.Analysis.JobSortOrder;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.webservice.JobInfo;

import com.google.common.base.Strings;

/**
 * Re-implementation (based on copy/paste) of existing (circa GP 3.8.1) job search.
 * @author pcarr
 *
 */
public class JobSearchLegacy {
    private static final Logger log = Logger.getLogger(JobSearchLegacy.class);

    public static class SearchQuery {
        public static final int DEFAULT_PAGE_SIZE=20;
        
        final String currentUser;  //the GP User who is making the query
        final boolean currentUserIsAdmin; // is the current user an admin
        final String userId; // search for jobs owned by this user, can be '*' to indicate all jobs.
        final JobSortOrder jobSortOrder;
        final String selectedGroup;
        final String selectedBatchId;
        final boolean ascending;
        final int pageNum;
        final int pageSize;
        
        private SearchQuery(final Builder in) {
            this.currentUser=in.userContext.getUserId();
            this.currentUserIsAdmin=in.userContext.isAdmin();
            this.userId=in.userId;
            this.jobSortOrder=in.jobSortOrder;
            this.selectedGroup=in.selectedGroup;
            this.selectedBatchId=in.selectedBatchId;
            this.ascending=in.ascending;
            this.pageNum=in.pageNum;
            this.pageSize=in.pageSize;
        }
        
        public String getCurrentUser() {
            return currentUser;
        }
        public boolean isCurrentUserAdmin() {
            return currentUserIsAdmin;
        }
        public boolean isShowEveryonesJobs() {
            return "*".equals(userId);
        }
        public String getSelectedGroup() {
            return selectedGroup;
        }
        public boolean isBatch() {
            return !Strings.isNullOrEmpty(selectedBatchId);
        }
        public boolean isGroup() {
            return !Strings.isNullOrEmpty(selectedGroup);
        }
        
        public JobSortOrder getJobSortOrder() {
            return this.jobSortOrder;
        }
        public boolean isAscending() {
            return ascending;
        }
        
        public int getPageNum() {
            return pageNum;
        }
        
        public int getPageSize() {
            return pageSize;
        }
        
        // for the BATCH JOB QUERY, compute maxResults and firstResult from the pageSize and pageNum attributes
        public int getFirstResult() {
            return pageSize * (pageNum-1);
        }
        
        public static class Builder {
            private final GpContext userContext;
            private String userId=null; // null or not-set means, currentUser
            private JobSortOrder jobSortOrder=JobSortOrder.JOB_NUMBER;
            private String selectedGroup=null;
            private String selectedBatchId=null;
            private boolean ascending=true;
            private int pageNum=1;
            private int pageSize=DEFAULT_PAGE_SIZE;
            
            public Builder(GpContext userContext) {
                this.userContext=userContext;
            }
            
            public Builder userId(final String userId) {
                this.userId=userId;
                return this;
            }
            
            public Builder groupId(final String groupId) {
                this.selectedGroup=groupId;
                return this;
            }
            
            public Builder batchId(final String batchId) {
                this.selectedBatchId=batchId;
                return this;
            }
            
            public Builder pageNum(final int pageNum) {
                this.pageNum=pageNum;
                return this;
            }
            
            public Builder pageSize(final int pageSize) {
                this.pageSize=pageSize;
                return this;
            }
            
            public SearchQuery build() {
                //special-case: if necessary initalize the pageSize
                if (pageSize<=0) {
                    initPageSize();
                } 
                return new SearchQuery(this);
            }
            
            private void initPageSize() {
                //TODO: init from DB
                log.error("initPageSize not implemented, using hard-coded value: 20");
                this.pageSize=20;
            }
        }
        
    }
    
    /// utility methods
    public static List<Integer> getPages(final int pageNumber, final int pageCount) {
        final int MAX_PAGES = 25;
        //int pageCount = getPageCount();
        int startNum = 1;
        int endNum = pageCount;
        if (pageCount > MAX_PAGES) {
            //final int pageNumber=getPageNumber();
            endNum = Math.max(pageNumber + (MAX_PAGES / 2), MAX_PAGES);
            endNum = Math.min(endNum, pageCount);
            startNum = endNum - MAX_PAGES - 1;
            startNum = Math.max(startNum, 1);
        }
        List<Integer> pages = new ArrayList<Integer>();
        if (startNum > 1) {
            pages.add(1);
        }
        if (startNum > 2) {
            pages.add(-1); // GAP
        }
        for (int i = startNum; i <= endNum; i++) {
            pages.add(i);
        }
        if (endNum < (pageCount - 1)) {
            pages.add(-1);
        }
        if (endNum < pageCount) {
            pages.add(pageCount);
        }
        return pages;
    }

    public static List<JobInfo> doSearch(final SearchQuery q) {
        //final int pageNum = this.getPageNumber();
        //final int pageSize = this.getPageSize();
        //final JobSortOrder jobSortOrder = this.getJobSortOrder();
        //final boolean ascending = isJobSortAscending();

        //final String userId = UIBeanHelper.getUserId();
        //boolean isAdmin = false;
        //if (userSessionBean != null) {
        //    isAdmin = userSessionBean.isAdmin();
        //}
        //String selectedGroup = jobResultsFilterBean.getSelectedGroup();
        //boolean showEveryonesJobs = jobResultsFilterBean.isShowEveryonesJobs();

        //boolean filterOnBatch = false;
        //if (selectedGroup != null && selectedGroup.startsWith(BatchJob.BATCH_KEY)) {
        //    filterOnBatch = true;
        //}

        List<JobInfo> jobInfos = new ArrayList<JobInfo>();
        try {
            AnalysisDAO ds = new AnalysisDAO();
            //List<JobInfo> jobInfos = new ArrayList<JobInfo>();
            if (q.isBatch()) {
                JobInfo[] jobInfoArray = new BatchJobDAO().getBatchJobs(
                        q.getCurrentUser(), //userId, 
                        q.getSelectedGroup(), //selectedGroup, 
                        q.getFirstResult(), //maxJobNumber, 
                        q.getPageSize(), // maxEntries, 
                        q.getJobSortOrder(), //getJobSortOrder(), 
                        q.isAscending());
                // add all
                for (JobInfo ji : jobInfoArray) {
                    jobInfos.add(ji);
                }
            }
            else {
                if (q.isShowEveryonesJobs()) {
                    if (q.isCurrentUserAdmin()) {
                        jobInfos = ds.getAllPagedJobsForAdmin(
                                q.getPageNum(), 
                                q.getPageSize(), 
                                q.getJobSortOrder(), 
                                q.isAscending());
                    }
                    else {
                        IGroupMembershipPlugin groupMembership = UserAccountManager.instance().getGroupMembership();
                        Set<String> groupIds = new HashSet<String>(groupMembership.getGroups(q.getCurrentUser()));
                        jobInfos = ds.getAllPagedJobsForUser(
                                q.getCurrentUser(), 
                                groupIds, 
                                q.getPageNum(), 
                                q.getPageSize(), 
                                q.getJobSortOrder(), 
                                q.isAscending());
                    }
                }
                else {
                    if (q.isGroup()) {
                        jobInfos = ds.getPagedJobsInGroup(
                                q.getSelectedGroup(), 
                                q.getPageNum(), 
                                q.getPageSize(), 
                                q.getJobSortOrder(), 
                                q.isAscending());
                    }
                    else {
                        jobInfos = ds.getPagedJobsOwnedByUser(
                                q.getCurrentUser(), 
                                q.getPageNum(), 
                                q.getPageSize(), 
                                q.getJobSortOrder(), 
                                q.isAscending());
                    }
                }
            }
        }
        catch (Throwable t) {
            log.error(t);
            return Collections.emptyList();
        }
        return jobInfos;
    }

}
