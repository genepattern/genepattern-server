package org.genepattern.server.webapp.rest.api.v1.job.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

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
public class JobSearchLegacy {
    private static final Logger log = Logger.getLogger(JobSearchLegacy.class);

    /**
     * Search the GP DB for job records which match the give SearchQuery.
     * @param q, the search query, usually generated from the REST API calls.
     * @return a SearchResults instance containing relevant details for formatting a JSON response 
     *     to a web client.
     */
    public static SearchResults doSearch(final SearchQuery q) {
        int numItems=getJobCount(q);
        List<JobInfo> jobInfos=searchJobInfos(q);
        List<String> groupIds=getGroupIds(q);
        List<String> batchIds=getBatchIds(q);
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
    private static int getJobCount(final SearchQuery q) {
        //if (jobCount < 0) {
        //final String selectedGroup=q.getSelectedGroup();
        if (q.isBatch()) {
            final int jobCount = new BatchJobDAO().getNumBatchJobs(q.getBatchId());
            return jobCount;
        }
        else if (q.isGroup()) {
            //  get the count of jobs in the selected group
            final Set<String> selectedGroups = new HashSet<String>();
            selectedGroups.add(q.getSelectedGroup());
            final int jobCount = new AnalysisDAO().getNumJobsInGroups(selectedGroups);
            return jobCount;
        }
        else if (!q.isShowEveryonesJobs()) {
            final int jobCount = new AnalysisDAO().getNumJobsByUser(q.getCurrentUser());
            return jobCount;
        }
        
        if (q.isCurrentUserAdmin()) {
            final int jobCount = new AnalysisDAO().getNumJobsTotal();
            return jobCount;
        }
        IGroupMembershipPlugin groupMembership = UserAccountManager.instance().getGroupMembership();
        Set<String> groups = groupMembership.getGroups(q.getCurrentUser());
        final int jobCount = new AnalysisDAO().getNumJobsByUser(q.getCurrentUser(), groups);
        return jobCount;
    }

    /**
     * Makes a DB call to the the list of JobInfo which match the search criteria.
     * 
     * @param q
     * @return
     */
    private static List<JobInfo> searchJobInfos(final SearchQuery q) {
        List<JobInfo> jobInfos = new ArrayList<JobInfo>();
        try {
            AnalysisDAO ds = new AnalysisDAO();
            if (q.isBatch()) {
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
            // rval.add(new SelectItem(BatchJob.BATCH_KEY+batchJob.getJobNo(), "Batch: "+batchJob.getJobNo()));
            sorted.add(""+batchJob.getJobNo());
        }
        List<String> batchIds=new ArrayList<String>(sorted);
        return ImmutableList.copyOf(batchIds);
    }

}
