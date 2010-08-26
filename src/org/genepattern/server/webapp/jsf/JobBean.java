/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2009) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.
 
 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.webapp.jsf;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.genepattern.codegenerator.CodeGeneratorUtil;
import org.genepattern.server.PermissionsHelper;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.user.UserDAO;
import org.genepattern.server.user.UserProp;
import org.genepattern.server.user.UserPropKey;
import org.genepattern.server.webservice.server.Analysis.JobSortOrder;
import org.genepattern.server.webservice.server.dao.AdminDAO;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.server.webservice.server.local.IAdminClient;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.server.webservice.server.local.LocalAnalysisClient;
import org.genepattern.util.SemanticUtil;
import org.genepattern.webservice.AnalysisJob;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.WebServiceException;

public class JobBean { 
    private static Logger log = Logger.getLogger(JobBean.class);
    private List<JobResultsWrapper> recentJobs;
    private List<JobResultsWrapper> allJobs;
    private Map<String, Collection<TaskInfo>> kindToModules;
    private Map<String, List<KeyValuePair>> kindToInputParameters = Collections.emptyMap();

    /**
     * Indicates whether execution logs should be shown. Manipulated by checkbox on job results page, always false on
     * recent jobs page.
     */
    private boolean showExecutionLogs = false;

    /**
     * File sort direction (true for ascending, false for descending)
     */
    private boolean fileSortAscending = false;

    /**
     * Specifies file column to sort on. Possible values are name size lastModified
     */
    private String fileSortColumn = "name";


    /**
     * Specifies job column to sort on. Possible values are jobNumber taskName dateSubmitted dateCompleted status
     */
    private String jobSortColumn = "jobNumber";

    /**
     * Job sort direction (true for ascending, false for descending)
     */
    private boolean jobSortAscending = false;

    /** Number of job results shown per page */
    private int pageSize;

    /** Current page displayed */
    private int pageNumber = 1;
    
    private UserSessionBean userSessionBean = null;
    public void setUserSessionBean(final UserSessionBean u) {
        this.userSessionBean = u;
    }
    
    private JobResultsFilterBean jobResultsFilterBean = null;
    public void setJobResultsFilterBean(JobResultsFilterBean j) {
        this.jobResultsFilterBean = j;
    }

    public JobBean() {
        try {
            pageSize = Integer.parseInt(System.getProperty("job.results.per.page", "20"));
        } 
        catch (NumberFormatException nfe) {
            pageSize = 20;
        }
	
        String userId = UIBeanHelper.getUserId();
        kindToModules = SemanticUtil.getKindToModulesMap(new AdminDAO().getLatestTasks(userId));
        
        UserDAO userDao = new UserDAO();
        Set<UserProp> userProps = userDao.getUserProps(userId);
        
        this.showExecutionLogs = Boolean.valueOf(UserDAO.getPropertyValue(userProps, "showExecutionLogs", String.valueOf(showExecutionLogs)));

        // Attributes to support job results page
        this.fileSortAscending = Boolean.valueOf(UserDAO.getPropertyValue(userProps, "fileSortAscending", String.valueOf(fileSortAscending)));
        this.fileSortColumn = UserDAO.getPropertyValue(userProps, "fileSortColumn", fileSortColumn);
        this.jobSortColumn = UserDAO.getPropertyValue(userProps, "jobSortColumn", jobSortColumn);
        this.jobSortAscending = Boolean.valueOf(UserDAO.getPropertyValue(userProps, "jobSortAscending", String.valueOf(jobSortAscending)));
    }

    public void createPipeline(ActionEvent e) {
        try {
            HttpServletRequest request = UIBeanHelper.getRequest();
            String jobNumber = null;
            try{
                jobNumber = UIBeanHelper.decode(request.getParameter("jobNumber"));
                String jobNumberOverride = request.getParameter("jobNumberOverride");
                if (jobNumberOverride != null) {
                    jobNumber = jobNumberOverride;
                }
            } 
            catch (Exception ex){
                ex.printStackTrace();
            }
            if (jobNumber == null) {
                UIBeanHelper.setErrorMessage("No job specified.");
                return;
            }
            
            // TODO prompt user for name
            String pipelineName = "job" + jobNumber; 
            String lsid = new LocalAnalysisClient(UIBeanHelper.getUserId()).createProvenancePipeline(jobNumber, pipelineName);
            if (lsid == null) {
                UIBeanHelper.setErrorMessage("Unable to create pipeline.");
                return;
            }
            UIBeanHelper.getResponse().sendRedirect(
                UIBeanHelper.getRequest().getContextPath() + "/pipelineDesigner.jsp?name=" + UIBeanHelper.encode(lsid));
        } 
        catch (WebServiceException wse) {
            log.error("Error creating pipeline.", wse);
        } 
        catch (IOException e1) {
            log.error("Error creating pipeline.", e1);
        }
    }
    
    public String deleteAction(){
    	delete(null);
    	return "homeNoRedirect";
    }
    
    /**
     * Delete the selected job. Should this also delete the files?
     * 
     * @param event
     */
    public void delete(ActionEvent event) {
        try {
            int jobNumber = Integer.parseInt(UIBeanHelper.decode(UIBeanHelper.getRequest().getParameter("jobNumber")));
            deleteJob(jobNumber);
            UIBeanHelper.setErrorMessage("Job "+jobNumber+" has been deleted");
        	
            resetJobs();
        } 
        catch (NumberFormatException e) {
            log.error("Error deleting job.", e);
            return;
        }
    }

    public void deleteFile(ActionEvent event) {
	String value = UIBeanHelper.decode(UIBeanHelper.getRequest().getParameter("jobFile"));
	deleteFile(value);
	resetJobs();
    }

    public String getTaskCode() {
	try {
	    String language = UIBeanHelper.decode(UIBeanHelper.getRequest().getParameter("language"));
	    String lsid = UIBeanHelper.decode(UIBeanHelper.getRequest().getParameter("taskLSID"));

	    IAdminClient adminClient = new LocalAdminClient(UIBeanHelper.getUserId());
	    TaskInfo taskInfo = adminClient.getTask(lsid);
	    if (taskInfo == null) {
		return "Module not found";
	    }
	    ParameterInfo[] parameters = taskInfo.getParameterInfoArray();

	    ParameterInfo[] jobParameters = new ParameterInfo[parameters != null ? parameters.length : 0];

	    if (parameters != null) {
		int i = 0;
		for (ParameterInfo p : parameters) {
		    String value = UIBeanHelper.getRequest().getParameter(p.getName());

		    jobParameters[i++] = new ParameterInfo(p.getName(), value, "");
		}
	    }

	    JobInfo jobInfo = new JobInfo(-1, -1, null, null, null, jobParameters, UIBeanHelper.getUserId(), lsid,
		    taskInfo.getName());

	    boolean isVisualizer = TaskInfo.isVisualizer(taskInfo.getTaskInfoAttributes());
	    AnalysisJob job = new AnalysisJob(UIBeanHelper.getServer(), jobInfo, isVisualizer);
	    return CodeGeneratorUtil.getCode(language, job, taskInfo, adminClient);
	} catch (WebServiceException e) {
	    log.error("Error getting code.", e);
	} catch (Exception e) {
	    log.error("Error getting code.", e);
	}
	return "";
    }

    public boolean isShowExecutionLogs() {
        return showExecutionLogs;
    }
    
    /**
     * Loads a module from an output file.
     * 
     * @return
     */
    public String loadTask() {
	String lsid = UIBeanHelper.decode(UIBeanHelper.getRequest().getParameter("module"));
	UIBeanHelper.getRequest().setAttribute("matchJob",
		UIBeanHelper.decode(UIBeanHelper.getRequest().getParameter("jobNumber")));
	UIBeanHelper.getRequest().setAttribute("outputFileName",
		UIBeanHelper.decode(UIBeanHelper.getRequest().getParameter("name")));
	RunTaskBean runTaskBean = (RunTaskBean) UIBeanHelper.getManagedBean("#{runTaskBean}");
	assert runTaskBean != null;
	runTaskBean.setTask(lsid);
	return "run task";
    }

    public String reload() {
	LocalAnalysisClient ac = new LocalAnalysisClient(UIBeanHelper.getUserId());
	try {
	    int jobNumber = Integer.parseInt(UIBeanHelper.decode(UIBeanHelper.getRequest().getParameter("jobNumber")));
	    JobInfo reloadJob = ac.getJob(jobNumber);
	    RunTaskBean runTaskBean = (RunTaskBean) UIBeanHelper.getManagedBean("#{runTaskBean}");
	    assert runTaskBean != null;
	    UIBeanHelper.getRequest().setAttribute("reloadJob", String.valueOf(reloadJob.getJobNumber()));
	    runTaskBean.setTask(reloadJob.getTaskLSID());
	} catch (WebServiceException e) {
	    log.error("Error reloading job.", e);
	} catch (NumberFormatException e) {
	    log.error("Error reloading job.", e);
	}
	return "run task";
    }

    public void saveFile(ActionEvent event) {
        HttpServletRequest request = UIBeanHelper.getRequest();
        
        String jobFileName = request.getParameter("jobFileName");
        String jobNumber2 = request.getParameter("jobNumber");
        String jobNumber3 = request.getParameter("jobNumberOverride");
        String jobNumber4 = request.getParameter("jnOverride");
	    
        
        System.out.println("SAVE on job #" + jobNumber2 + "  " + jobNumber3 + "  " + jobNumber4);
        System.out.println("SAVE on file #" + jobFileName);
        
        jobFileName = UIBeanHelper.decode(jobFileName);
        if (jobFileName == null || "".equals(jobFileName.trim())) {
            log.error("Error saving file, missing required parameter, 'jobFileName'.");
            return;
        }
	    
        //parse jobFileName for <jobNumber> and <filename>, add support for directories
        //from Job Summary page jobFileName="1/all_aml_test.preprocessed.gct"
        //from Job Status page jobFileName="/gp/jobResults/1/all_aml_test.preprocessed.gct"
        String contextPath = request.getContextPath();
        String pathToJobResults = contextPath + "/jobResults/";
        if (jobFileName.startsWith(pathToJobResults)) {
            jobFileName = jobFileName.substring(pathToJobResults.length());
        }

        int idx = jobFileName.indexOf('/');
        if (idx <= 0) {
            log.error("Error saving file, invalid parameter, jobFileName="+jobFileName);
            return;
        }
        String jobNumber = jobFileName.substring(0, idx);
        String filename = jobFileName.substring(idx+1);
        File in = new File(GenePatternAnalysisTask.getJobDir(jobNumber), filename);
        if (!in.exists()) {
            UIBeanHelper.setInfoMessage("File " + filename + " does not exist.");
            return;
        }
        InputStream is = null;
        try {
            HttpServletResponse response = UIBeanHelper.getResponse();
            response.setHeader("Content-Disposition", "attachment; filename=" + in.getName() + ";");
            response.setHeader("Content-Type", "application/octet-stream");
            response.setHeader("Cache-Control", "no-store"); // HTTP 1.1
            response.setHeader("Pragma", "no-cache"); // HTTP 1.0 cache
            response.setDateHeader("Expires", 0);

            OutputStream os = response.getOutputStream();
            is = new BufferedInputStream(new FileInputStream(in));
            byte[] b = new byte[10000];
            int bytesRead;
            while ((bytesRead = is.read(b)) != -1) {
                os.write(b, 0, bytesRead);
            }
            os.flush();
            os.close();
            UIBeanHelper.getFacesContext().responseComplete();
        } 
        catch (IOException e) {
            log.error("Error saving file.", e);
        } 
        finally {
            if (is != null) {
                try {
                    is.close();
                } 
                catch (IOException e) {
                }
            }
        }
    }

    public void setShowExecutionLogs(boolean showExecutionLogs) {
        this.showExecutionLogs = showExecutionLogs;
        new UserDAO().setProperty(UIBeanHelper.getUserId(), "showExecutionLogs", String.valueOf(showExecutionLogs));
        resetJobs();
    }

    public void terminateJob(ActionEvent event) {
	try {
	    int jobNumber = Integer.parseInt(UIBeanHelper.decode(UIBeanHelper.getRequest().getParameter("jobNumber")));
	    LocalAnalysisClient ac = new LocalAnalysisClient(UIBeanHelper.getUserId());
	    ac.terminateJob(jobNumber);
	    resetJobs();
	} catch (WebServiceException e) {
	    log.error("Error getting job " + UIBeanHelper.getRequest().getParameter("jobNumber"), e);
	} catch (NumberFormatException e) {
	    log.error(UIBeanHelper.getRequest().getParameter("jobNumber") + " is not a number.", e);
	}
    }

    public void viewCode(ActionEvent e) {
	try {
	    String language = UIBeanHelper.decode(UIBeanHelper.getRequest().getParameter("language"));
	    int jobNumber = Integer.parseInt(UIBeanHelper.decode(UIBeanHelper.getRequest().getParameter("jobNumber")));
	    AnalysisJob job = new AnalysisJob(UIBeanHelper.getUserId(), new LocalAnalysisClient(UIBeanHelper
		    .getUserId()).getJob(jobNumber));
	    viewCode(language, job, "" + jobNumber);
	} catch (WebServiceException x) {
	    log.error("Error getting job " + UIBeanHelper.getRequest().getParameter("jobNumber"), x);
	}
    }

    public void viewCode(String language, AnalysisJob job, String baseName) {
	OutputStream os = null;
	try {
	    HttpServletResponse response = UIBeanHelper.getResponse();
	    String filename = baseName + CodeGeneratorUtil.getFileExtension(language);
	    response.setHeader("Content-disposition", "inline; filename=\"" + filename + "\"");
	    response.setHeader("Content-Type", "text/plain");
	    response.setHeader("Cache-Control", "no-store"); // HTTP 1.1
	    // cache
	    // control
	    response.setHeader("Pragma", "no-cache"); // HTTP 1.0 cache
	    // control
	    response.setDateHeader("Expires", 0);
	    os = response.getOutputStream();

	    IAdminClient adminClient = new LocalAdminClient(UIBeanHelper.getUserId());
	    TaskInfo taskInfo = adminClient.getTask(job.getLSID());

	    String code = CodeGeneratorUtil.getCode(language, job, taskInfo, adminClient);

	    PrintWriter pw = new PrintWriter(os);
	    pw.println(code);
	    pw.flush();
	    os.close();

	    UIBeanHelper.getFacesContext().responseComplete();
	} catch (Exception e) {
	    log.error("Error viewing code for job " + job.getJobInfo().getJobNumber(), e);
	}
    }

    protected void deleteFile(String jobFileName) {
        //parse encodedJobFileName for <jobNumber> and <filename>, add support for directories
        //from Job Summary page jobFileName="1/all_aml_test.preprocessed.gct"
        //from Job Status page jobFileName="/gp/jobResults/1/all_aml_test.preprocessed.gct"
        String contextPath = UIBeanHelper.getRequest().getContextPath();
        String pathToJobResults = contextPath + "/jobResults/";
        if (jobFileName.startsWith(pathToJobResults)) {
            jobFileName = jobFileName.substring(pathToJobResults.length());
        }
        int idx = jobFileName.indexOf('/');
        if (idx <= 0) {
            UIBeanHelper.setErrorMessage("Error deleting file: "+jobFileName);
            return;
        }
        int jobNumber = -1;
        String jobId = jobFileName.substring(0, idx);
        try {
            jobNumber = Integer.parseInt(jobId);
        }
        catch (NumberFormatException e) {
            UIBeanHelper.setErrorMessage("Error deleting file: "+jobFileName+", "+e.getMessage());
            return;
        }
        try {
            // String filename = encodedJobFileName.substring(index + 1);
            String currentUserId = UIBeanHelper.getUserId();
            LocalAnalysisClient analysisClient = new LocalAnalysisClient(currentUserId);
            analysisClient.deleteJobResultFile(jobNumber, jobFileName);
        } 
        catch (WebServiceException e) {
            UIBeanHelper.setErrorMessage("Error deleting file: "+jobFileName+", "+e.getMessage());
            return;
        }
    }

    /**
     * Delete the selected job. Should this also delete the files?
     * 
     * @param event
     */
    protected void deleteJob(int jobNumber) {
        try {
            LocalAnalysisClient ac = new LocalAnalysisClient(UIBeanHelper.getUserId());
            ac.deleteJob(jobNumber);
            HibernateUtil.getSession().flush();
        } 
        catch (WebServiceException e) {
            log.error("Error deleting job " + jobNumber, e);
        }
    }

    private List<JobResultsWrapper> wrapJobs(JobInfo[] jobInfoArray) {
        boolean isAdmin = false;
        if (userSessionBean != null) {
            isAdmin = userSessionBean.isAdmin();
        }
        String currentUserId = UIBeanHelper.getUserId();

        List<JobResultsWrapper> wrappedJobs = new ArrayList<JobResultsWrapper>(jobInfoArray.length);
        for(JobInfo jobInfo : jobInfoArray) {
            PermissionsHelper ph = new PermissionsHelper(isAdmin, currentUserId, jobInfo.getJobNumber(), jobInfo.getUserId(), jobInfo.getJobNumber());
            JobPermissionsBean jobPermissionsBean = new JobPermissionsBean(ph);

            JobResultsWrapper wrappedJob = new JobResultsWrapper(
                    jobPermissionsBean,
                    jobInfo, 
                    kindToModules, 
                    getSelectedFiles(),
                    getSelectedJobs(), 
                    0, 
                    0, 
                    kindToInputParameters, 
                    showExecutionLogs);
            wrappedJob.setFileSortColumn(getFileSortColumn());
            wrappedJob.setFileSortAscending(fileSortAscending);
            wrappedJobs.add(wrappedJob);
        }
        return wrappedJobs;
    }

    private JobSortOrder getJobSortOrder() {
        if ("jobNumber".equals(jobSortColumn)) {
            return JobSortOrder.JOB_NUMBER;
        } 
        else if ("taskName".equals(jobSortColumn)) {
            return JobSortOrder.MODULE_NAME;
        } 
        else if ("dateSubmitted".equals(jobSortColumn)) {
            return JobSortOrder.SUBMITTED_DATE;
        } 
        else if ("dateCompleted".equals(jobSortColumn)) {
            return JobSortOrder.COMPLETED_DATE;
        } 
        else if ("status".equals(jobSortColumn)) {
            return JobSortOrder.JOB_STATUS;
        }
        return JobSortOrder.JOB_NUMBER;
    }

    public List<JobResultsWrapper> getRecentJobs() {
	if (recentJobs == null) {
	    String userId = UIBeanHelper.getUserId();
	    assert userId != null;
	    int recentJobsToShow = Integer.parseInt(new UserDAO().getPropertyValue(userId,
		    UserPropKey.RECENT_JOBS_TO_SHOW, "10"));
	    LocalAnalysisClient analysisClient = new LocalAnalysisClient(userId);
	    try {
		recentJobs = wrapJobs(analysisClient.getJobs(userId, -1, recentJobsToShow, false,
			JobSortOrder.JOB_NUMBER, false));
	    } catch (WebServiceException wse) {
		log.error(wse);
		recentJobs = new ArrayList<JobResultsWrapper>();
	    }
	}
	return recentJobs;
    }

    public List<JobResultsWrapper> getPagedJobs() {
        int offset = (getPageNumber() - 1) * pageSize;	
        return this.getJobs(offset, pageSize);
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public int getPageNumber() {
        //HACK:
        try {
            this.pageNumber = Integer.parseInt(UIBeanHelper.getRequest().getParameter("page"));
        }
        catch (NumberFormatException nfe) {
            this.pageNumber = 1;
        }
        return this.pageNumber;
    }

    public void goToPage() {
        this.pageNumber = Integer.parseInt(UIBeanHelper.getRequest().getParameter("page"));
    }

    public List<Integer> getPages() {
        final int MAX_PAGES = 25;
        int pageCount = getPageCount();
        int startNum = 1;
        int endNum = pageCount;
        if (pageCount > MAX_PAGES) {
            endNum = Math.max(getPageNumber()+(MAX_PAGES/2), MAX_PAGES);
            endNum = Math.min(endNum, pageCount);
            startNum = endNum - MAX_PAGES - 1;
            startNum = Math.max(startNum, 1);
        }
        List<Integer> pages = new ArrayList<Integer>();
        if (startNum > 1) {
            pages.add(1);
        }
        if (startNum > 2) {
            pages.add(-1); //GAP
        }
        for (int i = startNum; i <= endNum; i++) {
            pages.add(i);
        }
        if (endNum < (pageCount-1)) {
            pages.add(-1);
        }
        if (endNum < pageCount) {
            pages.add(pageCount);
        }
        return pages;
    }

    public void previousPage() {
	this.pageNumber--;
    }

    public void nextPage() {
	this.pageNumber++;
    }

    private int pageCount = -1;
    public int getPageCount() {
        if (jobResultsFilterBean != null) {
            int jobCount = jobResultsFilterBean.getJobCount();
            return (int) Math.ceil(jobCount / (double) pageSize);
        }
        log.error("null JobResultsFilterBean, can't compute pageCount in job results page.");
        return pageCount;
    }

    public List<JobResultsWrapper> getAllJobs() {
        int maxJobNumber = -1;
        int maxEntries = Integer.MAX_VALUE;
        return getJobs(maxJobNumber, maxEntries);
    }

    private List<JobResultsWrapper> getJobs(int maxJobNumber, int maxEntries) { 
        if (allJobs == null) {
            String userId = UIBeanHelper.getUserId(); 
            String selectedGroup = jobResultsFilterBean.getSelectedGroup();
            Set<String> selectedGroups = jobResultsFilterBean.getSelectedGroups();
            boolean showEveryonesJobs = jobResultsFilterBean.isShowEveryonesJobs();

            try {
                JobInfo[] jobInfos = new JobInfo[0];
                AnalysisDAO ds = new AnalysisDAO();
                if (selectedGroup != null) {
                    jobInfos = ds.getJobsInGroup(selectedGroups, maxJobNumber, maxEntries, false, getJobSortOrder(), jobSortAscending);
                }
                else {
                    jobInfos = ds.getJobs(showEveryonesJobs ? null : userId, maxJobNumber, maxEntries, false, getJobSortOrder(), jobSortAscending);
                }
                allJobs = wrapJobs( jobInfos );
            } 
            catch (Exception e) {
                log.error(e);
                allJobs = new ArrayList<JobResultsWrapper>();
            }
        }
        return allJobs;
    }

    private List<ParameterInfo> getOutputParameters(JobInfo job) {
	ParameterInfo[] params = job.getParameterInfoArray();
	List<ParameterInfo> paramsList = new ArrayList<ParameterInfo>();
	if (params != null) {
	    for (ParameterInfo p : params) {
		if (p.isOutputFile()) {
		    paramsList.add(p);
		}
	    }
	}
	return paramsList;
    }

    /**
     * Get the list of selected files (pathnames) from the request parameters. This is converted to a set to make
     * membership tests efficient.
     * 
     * @return The selected files.
     */
    private Set<String> getSelectedFiles() {
	HashSet<String> selectedJobs = new HashSet<String>();
	String[] tmp = UIBeanHelper.getRequest().getParameterValues("selectedFiles");
	if (tmp != null) {
	    for (String job : tmp) {
		selectedJobs.add(job);
	    }
	}
	return selectedJobs;
    }

    /**
     * Get the list of selected jobs (LSIDs) from the request parameters. This is converted to a set to make membership
     * tests efficient.
     * 
     * @return The selected jobs.
     */
    private Set<String> getSelectedJobs() {
	HashSet<String> selectedJobs = new HashSet<String>();
	String[] tmp = UIBeanHelper.getRequest().getParameterValues("selectedJobs");
	if (tmp != null) {
	    for (String job : tmp) {
		selectedJobs.add(job);
	    }
	}
	return selectedJobs;
    }

    /**
     * Delete the selected jobs and files.
     * 
     * @return
     */
    public String delete() {
        String[] selectedFiles = UIBeanHelper.getRequest().getParameterValues("selectedFiles");
        if (selectedFiles != null) {
            for (String jobFileName : selectedFiles) {
                deleteFile(jobFileName);
            }
        }
        String[] selectedJobs = UIBeanHelper.getRequest().getParameterValues("selectedJobs");
        deleteJobs(selectedJobs);
        this.resetJobs();
        return null;
    }
    
    public String sortFilesByName() {
        toggleFileSortColumn("name");
        return "success";
    }
    
    public String sortFilesBySize() {
        toggleFileSortColumn("size");
        return "success";
    }
    
    public String sortFilesByDateCompleted() {
        toggleFileSortColumn("lastModified");
        return "success";
    }
    
    private void toggleFileSortColumn(String fileSortColumn) {
        if (fileSortColumn.equals(this.fileSortColumn)) {
            this.setFileSortAscending(!this.fileSortAscending);            
        }
        else {
            this.setFileSortColumn(fileSortColumn);
        }
    }
    
    public String sortJobsById() {
        if ("jobNumber".equals(this.jobSortColumn)) {
            this.setJobSortAscending(!this.jobSortAscending);
        }
        else {
            this.setJobSortColumn("jobNumber");
        }
        return "success";
    }
    
    public String sortJobsByModule() {
        if ("taskName".equals(this.jobSortColumn)) {
            this.setJobSortAscending(!this.jobSortAscending);
        }
        else {
            this.setJobSortColumn("taskName");
        }
        return "success";
    }
    
    public String sortJobsByDateSubmitted() {
        if ("dateSubmitted".equals(this.jobSortColumn)) {
            this.setJobSortAscending(!this.jobSortAscending);
        }
        else {
            this.setJobSortColumn("dateSubmitted");
        }
        return "success";
    }
    
    public String sortJobsByDateCompleted() {
        if ("dateCompleted".equals(this.jobSortColumn)) {
            this.setJobSortAscending(!this.jobSortAscending);
        }
        else {
            this.setJobSortColumn("dateCompleted");
        }
        return "success";
    }

    public String sortJobsByStatus() {
        if ("status".equals(this.jobSortColumn)) {
            this.setJobSortAscending(!this.jobSortAscending);
        }
        else {
            this.setJobSortColumn("status");
        }
        return "success";
    }

    public String getFileSortColumn() {
        return fileSortColumn;
    }

    public String getJobSortColumn() {
        return jobSortColumn;
    }

    public boolean isFileSortAscending() {
        return fileSortAscending;
    }

    public void setFileSortAscending(boolean fileSortAscending) {
        this.fileSortAscending = fileSortAscending;
        new UserDAO().setProperty(UIBeanHelper.getUserId(), "fileSortAscending", String.valueOf(fileSortAscending));
    }

    public void setFileSortColumn(String fileSortField) {
        this.fileSortColumn = fileSortField;
        new UserDAO().setProperty(UIBeanHelper.getUserId(), "fileSortColumn", String.valueOf(fileSortField));
    }

    public void setJobSortAscending(boolean jobSortAscending) {
        this.jobSortAscending = jobSortAscending;
        new UserDAO().setProperty(UIBeanHelper.getUserId(), "jobSortAscending", String.valueOf(jobSortAscending));
    }

    public void setJobSortColumn(String jobSortField) {
        this.jobSortColumn = jobSortField;
        new UserDAO().setProperty(UIBeanHelper.getUserId(), "jobSortColumn", String.valueOf(jobSortColumn));
    }

    /**
     * Jobs are always sorted, so there's nothing to do here. This is just an action method to trigger a reload. Could
     * probably be better named.
     * 
     * @return
     */
    public String sort() {
        return null;
    }

    /**
     * Delete a list of jobs.
     * 
     * @param jobNumbers
     */
    private void deleteJobs(String[] jobNumbers) {
	List<Integer> jobErrors = new ArrayList<Integer>();

	if (jobNumbers != null) {
	    for (String job : jobNumbers) {
		int jobNumber = Integer.parseInt(job);
		try {
		    deleteJob(jobNumber);
		} catch (NumberFormatException e) {
		    log.error(e);
		}
	    }
	}

	// Create error messages
	StringBuffer sb = new StringBuffer();
	for (int i = 0, size = jobErrors.size(); i < size; i++) {
	    if (i > 0) {
		sb.append(", ");
	    }
	    sb.append(jobErrors.get(i));
	}
	if (jobErrors.size() > 0) {
	    String msg = "An error occurred while deleting job(s) " + sb.toString() + ".";
	    FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, msg));
	}

    }

    public boolean isJobSortAscending() {
	return jobSortAscending;
    }
    
    public String getJobSortFlag() {
        if (jobSortAscending) {
            return "up";
        }
        else {
            return "down";
        }
    }
    
    public String getFileSortFlag() {
        if (fileSortAscending) {
            return "up";
        }
        else {
            return "down";
        }
    }

    /**
     * Force an update of the job list by nulling the current values.
     */
    private void resetJobs() {
        recentJobs = null;
        allJobs = null;
    }

    public static class OutputFileInfo {

	private static final Comparator<KeyValuePair> COMPARATOR = new KeyValueComparator();

	boolean exists;

	Date lastModified;

	List<KeyValuePair> moduleMenuItems = new ArrayList<KeyValuePair>();

	ParameterInfo p;

	boolean selected = false;

	long size;

	int jobNumber;

	List<KeyValuePair> moduleInputParameters;

	String kind;

	public OutputFileInfo(ParameterInfo p, File file, Collection<TaskInfo> modules, int jobNumber, String kind) {
	    this.kind = kind;
	    this.p = p;
	    this.size = file.length();
	    this.exists = file.exists();
	    Calendar cal = Calendar.getInstance();
	    cal.setTimeInMillis(file.lastModified());
	    this.lastModified = cal.getTime();

	    if (modules != null) {
		for (TaskInfo t : modules) {
		    KeyValuePair mi = new KeyValuePair(t.getShortName(), UIBeanHelper.encode(t.getLsid()));
		    moduleMenuItems.add(mi);
		}
		Collections.sort(moduleMenuItems, COMPARATOR);
	    }

	    this.jobNumber = jobNumber;
	}

	public String getUrl() {
	    return UIBeanHelper.getServer() + "/jobResults/" + getValue();
	}

	public int getJobNumber() {
	    return jobNumber;
	}

	public String getDescription() {
	    return p.getDescription();
	}

	public String getFormattedSize() {
	    return JobHelper.getFormattedSize(size);
	}

	public String getLabel() {
	    return p.getLabel();
	}

	public Date getLastModified() {
	    return lastModified;
	}

	public List<KeyValuePair> getModuleMenuItems() {
	    return moduleMenuItems;
	}

	public String getName() {
	    return UIBeanHelper.encode(p.getName());
	}
	
	public String getTruncatedDisplayName() {
    	if (getName() != null && getName().length() > 70) {
    		return UIBeanHelper.encode(getName().substring(0, 35)+"..." + getName().substring(getName().length()-32, getName().length()));
    	} else {
    		return UIBeanHelper.encode(getName());
    	}
    }

	public long getSize() {
	    return size;
	}

	public String getUIValue(ParameterInfo formalParam) {
	    return p.getUIValue(formalParam);
	}

	public String getValue() {
	    return p.getValue();
	}

	/**
	 * @return a valid value to be used for the 'id' attribute of an html div tag. The '/' character is not allowed,
	 *         so replace all '/' with '_'.
	 */
	public String getValueId() {
	    String str = getValue().replace('/', '_');
	    return str;
	}

	public boolean hasChoices(String delimiter) {
	    return p.hasChoices(delimiter);
	}

	public boolean isSelected() {
	    return selected;
	}

	public void setSelected(boolean bool) {
	    this.selected = bool;
	}

	public String toString() {
	    return p.toString();
	}

	public List<KeyValuePair> getModuleInputParameters() {
	    return moduleInputParameters;
	}

	public String getKind() {
	    return kind;
	}
    }

    private static class KeyValueComparator implements Comparator<KeyValuePair> {

	public int compare(KeyValuePair o1, KeyValuePair o2) {
	    return o1.getKey().compareToIgnoreCase(o2.getKey());
	}

    }

    public void setSelectedModule(String selectedModule) {
	List<JobResultsWrapper> recentJobs = getRecentJobs();
	if (selectedModule == null || recentJobs == null || recentJobs.size() == 0) {
	    return;
	}
	kindToInputParameters = new HashMap<String, List<KeyValuePair>>();

	TaskInfo taskInfo = null;
	try {
	    taskInfo = new LocalAdminClient(UIBeanHelper.getUserId()).getTask(selectedModule);
	} catch (WebServiceException e) {
	    log.error("Could not get module", e);
	    return;
	}
	ParameterInfo[] inputParameters = taskInfo != null ? taskInfo.getParameterInfoArray() : null;
	List<KeyValuePair> unannotatedParameters = new ArrayList<KeyValuePair>();
	if (inputParameters != null) {
	    for (ParameterInfo inputParameter : inputParameters) {
		if (inputParameter.isInputFile()) {
		    List<String> fileFormats = SemanticUtil.getFileFormats(inputParameter);
		    String displayValue = (String) inputParameter.getAttributes().get("altName");

		    if (displayValue == null) {
			displayValue = inputParameter.getName();
		    }
		    displayValue = displayValue.replaceAll("\\.", " ");

		    KeyValuePair kvp = new KeyValuePair();
		    kvp.setKey(inputParameter.getName());
		    kvp.setValue(displayValue);

		    if (fileFormats.size() == 0) {
			unannotatedParameters.add(kvp);
		    }
		    for (String format : fileFormats) {
			List<KeyValuePair> inputParameterNames = kindToInputParameters.get(format);
			if (inputParameterNames == null) {
			    inputParameterNames = new ArrayList<KeyValuePair>();
			    kindToInputParameters.put(format, inputParameterNames);
			}
			inputParameterNames.add(kvp);
		    }
		}
	    }
	}

	// add unannotated parameters to end of list for each kind
	if (unannotatedParameters.size() > 0) {
	    for (Iterator<String> it = kindToInputParameters.keySet().iterator(); it.hasNext();) {
		List<KeyValuePair> inputParameterNames = kindToInputParameters.get(it.next());
		inputParameterNames.addAll(unannotatedParameters);
	    }
	}

	for (JobResultsWrapper job : recentJobs) {
	    List<OutputFileInfo> outputFiles = job.getOutputFileParameterInfos();
	    if (outputFiles != null) {
		for (OutputFileInfo o : outputFiles) {
		    List<KeyValuePair> moduleInputParameters = kindToInputParameters.get(o.getKind());

		    if (moduleInputParameters == null) {
			moduleInputParameters = unannotatedParameters;
		    }
		    o.moduleInputParameters = moduleInputParameters;
		}
	    }
	}

    }

}
