/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webapp.rest.api.v1.job.search;

import java.util.*;

import org.apache.log4j.Logger;
import org.genepattern.server.UserAccountManager;
import org.genepattern.server.auth.IGroupMembershipPlugin;
import org.genepattern.server.domain.BatchJob;
import org.genepattern.server.domain.BatchJobDAO;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.webservice.JobInfo;

import com.google.common.collect.ImmutableList;

/**
 * Re-implementation (based on copy/paste) of existing (circa GP 3.8.1) job search.
 * This is part of the migration from JSF to RESTful API calls. 
 * 
 * @author pcarr
 *
 */
public abstract class JobSearch {
    private static final Logger log = Logger.getLogger(JobSearch.class);

    
    /**
     * Validate that this is a valid search, mainly to ensure against incorrect query parameters coming from a non-admin user.
     * For example, 
     *     (a) non-admin user can't view jobs by userId (unless it's their own userId).
     *         This is an artifact of the legacy implementation. We can improve this, but it's not ready yet.
     *         Need new DB queries.
     *     
     *     (b) non-admin user can't view jobs by groupId, unless the currentUser is in the group
     *         Note: not yet implemented.
     *     
     *     (c) Undefined behavior for non-admin user to view jobs by batchId. Need more testing of this scenario.
     *     
     * For legacy support we should only support the following scenarios:
     *     (a) admin user, view all jobs
     *     (b) non-admin user, view all jobs means all of their jobs plus any jobs which are shared with them
     *     (c) by group, means ignore the 'userId' query param
     *     (d) by batch, means ignore the 'userId' query param
     *     
     *     
     * @param q
     * @return true if this is a valid search query.
     */
    private static boolean validateSearchQuery(final SearchQuery q) {
        if (q.isCurrentUserAdmin()) {
            //admin can do all searches
            return true;
        }
        if (q.isShowAll()) {
            return true;
        }
        
        //otherwise, make sure that the userId matches the currentUser
        if (q.getUserId() != null && !q.getUserId().equals(q.getCurrentUser())) {
            log.debug("currentUser="+q.getCurrentUser()+" is not authorized to view jobs owned by userId="+q.getUserId());
            return false;
        }
        
        //TODO: validate groupId, make sure that the current user is in the selected group
        
        //TODO: validate batchId, make sure that the current user is authorized to view jobs in the selected batch
        return true;
    }
    
    /**
     * Search the GP DB for job records which match the give SearchQuery.
     * @param q, the search query, usually generated from the REST API calls.
     * @return a SearchResults instance containing relevant details for formatting a JSON response 
     *     to a web client.
     */
    public static SearchResults doSearch(final SearchQuery q) {
        final List<String> groupIds=getGroupIds(q);
        final List<String> batchIds=getBatchIds(q);
        if (!validateSearchQuery(q)) {
            final List<JobInfo> emptyJobInfos=Collections.emptyList();
            return new SearchResults.Builder(q)
                .numItems(0)
                .jobInfos(emptyJobInfos)
                .groupIds(groupIds)
                .batchIds(batchIds)
            .build();
        }
        
        final int numItems=getJobCount(q);
        final List<JobInfo> jobInfos=searchJobInfos(q);
        SearchResults searchResults=new SearchResults.Builder(q)
            .numItems(numItems)
            .jobInfos(jobInfos)
            .groupIds(groupIds)
            .batchIds(batchIds)
            .build();
        return searchResults;
    }
    
    /**
     * Makes a DB call to get a count of jobs which match the given search query.
     * @param q
     * @return
     */
    private static int getJobCount(final SearchQuery q)
    {

        if(!q.isShowAll() && q.getUserId() != null && !q.getUserId().equals(q.currentUser))
        {
            final int jobCount = new AnalysisDAO().getNumJobsByUser(q.getUserId());

            return jobCount;
        }
        else if(q.isCommentFilter() || q.isTagFilter())
        {
            int jobCount = 0;

            Set<String> groupIds = null;

            String userId = null;
            if(q.getGroupId() != null)
            {
                groupIds.add(q.getGroupId());
            }
            else if(q.isShowAll())
            {
                if(!q.isCurrentUserAdmin())
                {
                    IGroupMembershipPlugin groupMembership = UserAccountManager.instance().getGroupMembership();
                    groupIds = new HashSet<String>(groupMembership.getGroups(q.getCurrentUser()));
                }
            }
            else
            {
                userId = q.getUserId();
            }

            if (q.isTagFilter())
            {
                jobCount = new AnalysisDAO().getJobsWithTagCount(
                        q.getTag().toLowerCase(),
                        userId,
                        q.getBatchId(),
                        groupIds);
            }

            if (q.isCommentFilter())
            {
                jobCount = new AnalysisDAO().getJobsWithCommentCount(
                        q.getComment().toLowerCase(),
                        userId,
                        q.getBatchId(),
                        groupIds);
            }

            return jobCount;
        }
        else {
            if (q.isBatchFilter()) {
                final int jobCount = new BatchJobDAO().getNumBatchJobs(q.getBatchId());
                return jobCount;
            } else if (q.isGroupFilter()) {
                //  get the count of jobs in the selected group
                final Set<String> selectedGroups = new HashSet<String>();
                selectedGroups.add(q.getGroupId());
                final int jobCount = new AnalysisDAO().getNumJobsInGroups(selectedGroups);
                return jobCount;
            } else if (!q.isShowAll()) {
                final String forUserId;
                if (q.isUserFilter()) {
                    forUserId = q.getUserId();
                } else {
                    forUserId = q.getCurrentUser();
                }
                final int jobCount = new AnalysisDAO().getNumJobsByUser(forUserId);
                return jobCount;
            }

            // show all jobs
            if (q.isCurrentUserAdmin()) {
                final int jobCount = new AnalysisDAO().getNumJobsTotal();
                return jobCount;
            }
            IGroupMembershipPlugin groupMembership = UserAccountManager.instance().getGroupMembership();
            Set<String> groups = groupMembership.getGroups(q.getCurrentUser());
            final int jobCount = new AnalysisDAO().getNumJobsByUser(q.getCurrentUser(), groups);
            return jobCount;
        }
    }

    /**
     * Makes a DB call to the the list of JobInfo which match the search criteria.
     *
     * @param q
     * @return
     */
    private static List<JobInfo> searchJobInfos(final SearchQuery q)
    {
        AnalysisDAO ds = new AnalysisDAO();

        List<JobInfo> jobInfos = new ArrayList<JobInfo>();
        try {
            //create a link hash set to maintain order and remove duplicate job info objects
            if(!q.isShowAll() && q.getUserId() != null && !q.getUserId().equals(q.currentUser))
            {
                //search for job wih a specific owner not necessarily the current user
                //first get the intersection of jobs that matched the filter above
                jobInfos = searchJobsOwnedByUser(q, ds, q.getUserId());

            }
            else if(q.isCommentFilter() || q.isTagFilter())
            {
                if (q.isTagFilter())
                {
                    //first get the intersection of jobs that matched the filter above
                    jobInfos = searchJobsByTag(q, ds);
                }

                if (q.isCommentFilter())
                {
                    //first get the intersection of jobs that matched the filter above
                    jobInfos = searchJobsByComment(q, ds);
                }
            }
            else
            {
                if (q.isBatchFilter()) {
                    searchBatchJobs(q, jobInfos);
                } else if (!q.isShowAll() && q.isUserFilter()) {
                    jobInfos = searchJobsOwnedByUser(q, ds, q.getCurrentUser());
                } else if (q.isGroupFilter()) {
                    jobInfos = searchGroupJobs(q, ds);
                } else {
                    if (q.isCurrentUserAdmin()) {
                        jobInfos = searchAdminJobs(q, ds);
                    } else {
                        jobInfos = searchUserVisibleJobs(q, ds);
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

    private static void searchBatchJobs(SearchQuery q, List<JobInfo> jobInfos) {
        JobInfo[] jobInfoArray = new BatchJobDAO().getBatchJobs(
                q.getCurrentUser(), //userId,
                q.getBatchId(), //selectedGroup,
                q.getFirstResult(), //maxJobNumber,
                q.getPageSize(), // maxEntries,
                q.getJobSortOrder(), //getJobSortOrder(),
                q.isAscending());
        // add all
        for (JobInfo ji : jobInfoArray) {
            jobInfos.add(ji);
        }
    }

    private static List<JobInfo> searchJobsOwnedByUser(SearchQuery q, AnalysisDAO ds, String forUserId) {
        List<JobInfo> jobInfos;

        if (log.isDebugEnabled()) {
            log.debug("currentUser="+q.getCurrentUser());
            log.debug("forUserId="+forUserId);
        }
        jobInfos = ds.getPagedJobsOwnedByUser(
                forUserId,
                q.getPageNum(),
                q.getPageSize(),
                q.getJobSortOrder(),
                q.isAscending());
        return jobInfos;
    }

    private static List<JobInfo> searchGroupJobs(SearchQuery q, AnalysisDAO ds) {
        List<JobInfo> jobInfos;
        jobInfos = ds.getPagedJobsInGroup(
                q.getGroupId(),
                q.getPageNum(),
                q.getPageSize(),
                q.getJobSortOrder(),
                q.isAscending());
        return jobInfos;
    }

    private static List<JobInfo> searchUserVisibleJobs(SearchQuery q, AnalysisDAO ds) {
        List<JobInfo> jobInfos;
        IGroupMembershipPlugin groupMembership = UserAccountManager.instance().getGroupMembership();
        Set<String> groupIds = new HashSet<String>(groupMembership.getGroups(q.getCurrentUser()));
        jobInfos = ds.getAllPagedJobsForUser(
                q.getCurrentUser(),
                groupIds,
                q.getPageNum(),
                q.getPageSize(),
                q.getJobSortOrder(),
                q.isAscending());
        return jobInfos;
    }

    private static List<JobInfo> searchAdminJobs(SearchQuery q, AnalysisDAO ds) {
        List<JobInfo> jobInfos;
        jobInfos = ds.getAllPagedJobsForAdmin(
                q.getPageNum(),
                q.getPageSize(),
                q.getJobSortOrder(),
                q.isAscending());
        return jobInfos;
    }

    private static List<JobInfo> searchJobsByTag(SearchQuery q, AnalysisDAO ds) {

        List<JobInfo> jobInfos;
        Set<String> groupIds = null;

        String userId = q.getUserId();
        if(q.getGroupId() != null)
        {
            groupIds.add(q.getGroupId());
        }
        else if(q.isShowAll())
        {
            userId = null;

            if(!q.isCurrentUserAdmin())
            {
                IGroupMembershipPlugin groupMembership = UserAccountManager.instance().getGroupMembership();
                groupIds = new HashSet<String>(groupMembership.getGroups(q.getCurrentUser()));
            }
        }

        jobInfos = ds.getPagedJobsWithTag(q.getTag().toLowerCase(),
                userId,
                q.getBatchId(),
                groupIds,
                q.getPageNum(),
                q.getPageSize(),
                q.getJobSortOrder(),
                q.isAscending());

        return jobInfos;
    }

    private static List<JobInfo> searchJobsByComment(SearchQuery q, AnalysisDAO ds) {

        List<JobInfo> jobInfos;
        Set<String> groupIds = null;

        String userId = null;
        if(q.getGroupId() != null)
        {
            groupIds.add(q.getGroupId());
        }
        else if(q.isShowAll())
        {
            if(!q.isCurrentUserAdmin())
            {
                IGroupMembershipPlugin groupMembership = UserAccountManager.instance().getGroupMembership();
                groupIds = new HashSet<String>(groupMembership.getGroups(q.getCurrentUser()));
            }
        }
        else
        {
            userId = q.getUserId();
        }

        jobInfos = ds.getPagedJobsWithComment(
                q.getComment().toLowerCase(),
                userId,
                q.getBatchId(),
                groupIds,
                q.getPageNum(),
                q.getPageSize(),
                q.getJobSortOrder(),
                q.isAscending());

        return jobInfos;
    }

    /**
     * Makes a DB and/or system call to get the list of groups for the current user.
     * Based on JSF implementation in the JobResultsFilterBean.
     */
    public static List<String> getGroupIds(final SearchQuery q) {
        IGroupMembershipPlugin groupMembership = UserAccountManager.instance().getGroupMembership();
        SortedSet<String> sorted=new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        sorted.addAll( groupMembership.getGroups(q.getCurrentUser()));
        List<String> groups=new ArrayList<String>(sorted);
        return ImmutableList.copyOf(groups);
    }
    
    /**
     * Makes a DB call to get the list of batchIds for the current user.
     * Based on JSF implementation in the JobResultsFilterBean.
     */
    public static List<String> getBatchIds(final SearchQuery q) {
        List<BatchJob> batches = new BatchJobDAO().findByUserId(q.getCurrentUser());
        SortedSet<String> sorted=new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        for (BatchJob batchJob: batches){
            sorted.add(""+batchJob.getJobNo());
        }
        List<String> batchIds=new ArrayList<String>(sorted);
        return ImmutableList.copyOf(batchIds);
    }

}
