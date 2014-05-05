package org.genepattern.server.webapp.rest.api.v1.job.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.genepattern.server.UserAccountManager;
import org.genepattern.server.auth.IGroupMembershipPlugin;
import org.genepattern.server.domain.BatchJobDAO;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.webservice.JobInfo;

/**
 * Re-implementation (based on copy/paste) of existing (circa GP 3.8.1) job search.
 * @author pcarr
 *
 */
public class JobSearchLegacy {
    private static final Logger log = Logger.getLogger(JobSearchLegacy.class);

    public static SearchResults doSearch(final SearchQuery q) {
        int numItems=getJobCount(q);
        List<JobInfo> jobInfos=searchJobInfos(q);
        SearchResults searchResults=new SearchResults.Builder(q)
            .numItems(numItems)
            .jobInfos(jobInfos)
            .build();
        return searchResults;
    }

    public static int getJobCount(final SearchQuery q) {
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

}
