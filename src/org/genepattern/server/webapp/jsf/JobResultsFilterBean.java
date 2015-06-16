/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webapp.jsf;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.faces.model.SelectItem;

import org.genepattern.server.UserAccountManager;
import org.genepattern.server.auth.GroupPermission;
import org.genepattern.server.auth.IGroupMembershipPlugin;
import org.genepattern.server.domain.BatchJob;
import org.genepattern.server.domain.BatchJobDAO;
import org.genepattern.server.user.UserDAO;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;

/**
 * Backing bean for the drop-down menu on the Job Results page.
 * Save values to database so they can be loaded in the next session.
 * 
 * @author pcarr
 */
public class JobResultsFilterBean implements Serializable {
    private String userId = null;
    private boolean showEveryonesJobs = false;
    private String selectedGroup = null;
    private Set<String> selectedGroups = new HashSet<String>();

    //cached values
    private int jobCount = -1;

    public JobResultsFilterBean() {
        //requires a valid user id
        setUserId(UIBeanHelper.getUserId());
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
        this.jobCount = -1;
        
        this.showEveryonesJobs = 
            Boolean.valueOf(new UserDAO().getPropertyValue(userId, "showEveryonesJobs", String.valueOf(showEveryonesJobs)));
        this.selectedGroups.clear();
        this.selectedGroup = new UserDAO().getPropertyValue(userId, "jobResultsFilter", null);
        if (selectedGroup != null) {
            this.selectedGroups.add(selectedGroup);
            this.showEveryonesJobs = false;
        }
    }
    
    public Object getJobFilter() {
        String displayBatch = (String) UIBeanHelper.getSession().getAttribute(JobBean.DISPLAY_BATCH);
        if (displayBatch != null) {
            selectedGroup = BatchJob.BATCH_KEY + displayBatch;
        }
        
        if (selectedGroup != null) {
            return selectedGroup;
        }
        if (showEveryonesJobs) {
            return "#ALL_JOBS";
        }
        else {
            return "#MY_JOBS";
        }
    }

    public void setJobFilter(Object obj) {
        this.jobCount = -1;
        this.selectedGroup = null;
        this.selectedGroups.clear();
        this.showEveryonesJobs = false;
        
        String menuVal = null;
        if (obj instanceof String) {
            menuVal = (String) obj;
        }

        if (menuVal != null && menuVal.equals("#MY_JOBS")) {
        }
        else if (menuVal != null && menuVal.equals("#ALL_JOBS")) {
            this.showEveryonesJobs = true;
        }
        else if (menuVal != null) {
            selectedGroup = menuVal;
            selectedGroups.add(menuVal);
        }
        
        UserDAO userDao = new UserDAO();
        userDao.setProperty(UIBeanHelper.getUserId(), "showEveryonesJobs", String.valueOf(showEveryonesJobs));
        userDao.setProperty(UIBeanHelper.getUserId(), "jobResultsFilter", selectedGroup);
    }
    
    public List<SelectItem> getJobFilterMenu() {
        List<SelectItem> rval = new ArrayList<SelectItem>();
        rval.add(new SelectItem("#MY_JOBS", "My job results"));
        rval.add(new SelectItem("#ALL_JOBS", "All job results"));
        
        //add groups to the list
        String userId = UIBeanHelper.getUserId();
        IGroupMembershipPlugin groupMembership = UserAccountManager.instance().getGroupMembership();
        Set<String> groups = new HashSet<String>(groupMembership.getGroups(userId));
        List<BatchJob> batches = new BatchJobDAO().findByUserId(userId);
        	
        if (groups.contains(GroupPermission.PUBLIC)) {
            rval.add(new SelectItem(GroupPermission.PUBLIC, "Public job results"));
            groups.remove(GroupPermission.PUBLIC);
        }
        for(String group : groups) {
            rval.add(new SelectItem(group, "In group: " + group));
        }
        for (BatchJob batchJob: batches){
        	rval.add(new SelectItem(BatchJob.BATCH_KEY+batchJob.getJobNo(), "Batch: "+batchJob.getJobNo()));
        }
        return rval;
    }
    
    public void resetJobCount() {
        this.jobCount = -1;
    }

    public int getJobCount() {
        if (jobCount < 0) {
            if (selectedGroup != null) {
            	if (selectedGroup.startsWith(BatchJob.BATCH_KEY)){
            		jobCount = new BatchJobDAO().getNumBatchJobs(selectedGroup);
            		return jobCount;
            	}else{
            		//	get the count of jobs in the selected group
            		this.jobCount = new AnalysisDAO().getNumJobsInGroups(selectedGroups);
            		return this.jobCount;
            	}
            }
            else if (!showEveryonesJobs) {
                this.jobCount = new AnalysisDAO().getNumJobsByUser(userId);
                return this.jobCount;
            }

            boolean isAdmin = AuthorizationHelper.adminServer(); 
            if (isAdmin) {
                this.jobCount = new AnalysisDAO().getNumJobsTotal();
                return this.jobCount;
            }
            IGroupMembershipPlugin groupMembership = UserAccountManager.instance().getGroupMembership();
            Set<String> groups = groupMembership.getGroups(userId);
            this.jobCount = new AnalysisDAO().getNumJobsByUser(userId, groups);
            return this.jobCount;
        }
        return jobCount;
    }
    
    //legacy support (not to be called from JSF pages, but rather from JobBean.java)
    public boolean isShowEveryonesJobs() {
        return showEveryonesJobs;
    }
    
    public void setSelectedGroup(String selectedGroup) {
	    this.selectedGroup = selectedGroup;
    }

    public String getSelectedGroup() {
	    return selectedGroup;
    }
	
    public Set<String> getSelectedGroups() {
        return selectedGroups;
    }
}
