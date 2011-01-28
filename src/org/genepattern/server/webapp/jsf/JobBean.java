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
import java.util.Arrays;
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

import javax.faces.event.ActionEvent;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.genepattern.codegenerator.CodeGeneratorUtil;
import org.genepattern.server.JobInfoManager;
import org.genepattern.server.JobInfoWrapper;
import org.genepattern.server.JobManager;
import org.genepattern.server.PermissionsHelper;
import org.genepattern.server.UserAccountManager;
import org.genepattern.server.JobInfoWrapper.InputFile;
import org.genepattern.server.auth.IGroupMembershipPlugin;
import org.genepattern.server.domain.BatchJob;
import org.genepattern.server.domain.BatchJobDAO;
import org.genepattern.server.executor.JobTerminationException;
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
    private Map<String, List<KeyValuePair>> kindToInputParameters = Collections
            .emptyMap();

    /** Number of job results shown per page */
    private Integer _pageSize;

    private int getPageSize() {
        if (_pageSize == null) {
            try {
                _pageSize = Integer.parseInt(System.getProperty(
                        "job.results.per.page", "20"));
            }
            catch (NumberFormatException nfe) {
                _pageSize = 20;
            }
        }
        return _pageSize;
    }

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
    }

    private Set<UserProp> _userProps = null;

    private Set<UserProp> getUserProps() {
        if (_userProps == null) {
            String userId = UIBeanHelper.getUserId();
            UserDAO userDao = new UserDAO();
            _userProps = userDao.getUserProps(userId);
        }
        return _userProps;
    }

    public void createPipeline(ActionEvent e) {
        try {
            HttpServletRequest request = UIBeanHelper.getRequest();
            String jobNumber = null;
            try {
                jobNumber = UIBeanHelper.decode(request
                        .getParameter("jobNumber"));
                String jobNumberOverride = request
                        .getParameter("jobNumberOverride");
                if (jobNumberOverride != null) {
                    jobNumber = jobNumberOverride;
                }
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
            if (jobNumber == null) {
                UIBeanHelper.setErrorMessage("No job specified.");
                return;
            }

            // TODO prompt user for name
            String pipelineName = "job" + jobNumber;
            String lsid = new LocalAnalysisClient(UIBeanHelper.getUserId())
                    .createProvenancePipeline(jobNumber, pipelineName);
            if (lsid == null) {
                UIBeanHelper.setErrorMessage("Unable to create pipeline.");
                return;
            }
            UIBeanHelper.getResponse().sendRedirect(
                    UIBeanHelper.getRequest().getContextPath()
                            + "/pipelineDesigner.jsp?name="
                            + UIBeanHelper.encode(lsid));
        }
        catch (WebServiceException wse) {
            log.error("Error creating pipeline.", wse);
        }
        catch (IOException e1) {
            log.error("Error creating pipeline.", e1);
        }
    }

    public String deleteAction() {
        delete(null);
        return "homeNoRedirect";
    }

    /**
     * Delete the selected job. Should this also delete the files?
     * 
     * @param event
     */
    public void delete(ActionEvent event) {
        String jobNumberParam = UIBeanHelper.decode(UIBeanHelper.getRequest()
                .getParameter("jobNumber"));
        try {
            int jobNumber = Integer.parseInt(jobNumberParam);
            deleteJob(jobNumber);
            UIBeanHelper.setErrorMessage("Deleted job #" + jobNumber);
            resetJobs();
        }
        catch (NumberFormatException e) {
            log.error("Error deleting job.", e);
            return;
        }
        catch (WebServiceException e) {
            log.error("Error deleting job #" + jobNumberParam, e);
        }
    }

    public void deleteFile(ActionEvent event) {
        String value = UIBeanHelper.decode(UIBeanHelper.getRequest()
                .getParameter("jobFile"));
        deleteFile(value);
        resetJobs();
    }

    public String getTaskCode() {
        try {
            String language = UIBeanHelper.decode(UIBeanHelper.getRequest()
                    .getParameter("language"));
            String lsid = UIBeanHelper.decode(UIBeanHelper.getRequest()
                    .getParameter("taskLSID"));

            IAdminClient adminClient = new LocalAdminClient(
                    UIBeanHelper.getUserId());
            TaskInfo taskInfo = adminClient.getTask(lsid);
            if (taskInfo == null) {
                return "Module not found";
            }
            ParameterInfo[] parameters = taskInfo.getParameterInfoArray();
            ParameterInfo[] jobParameters = new ParameterInfo[parameters != null ? parameters.length
                    : 0];

            if (parameters != null) {
                int i = 0;
                for (ParameterInfo p : parameters) {
                    String value = UIBeanHelper.getRequest().getParameter(
                            p.getName());
                    jobParameters[i++] = new ParameterInfo(p.getName(), value,
                            "");
                }
            }

            JobInfo jobInfo = new JobInfo(-1, -1, null, null, null,
                    jobParameters, UIBeanHelper.getUserId(), lsid,
                    taskInfo.getName());
            boolean isVisualizer = TaskInfo.isVisualizer(taskInfo
                    .getTaskInfoAttributes());
            AnalysisJob job = new AnalysisJob(UIBeanHelper.getServer(),
                    jobInfo, isVisualizer);
            return CodeGeneratorUtil.getCode(language, job, taskInfo,
                    adminClient);
        }
        catch (WebServiceException e) {
            log.error("Error getting code.", e);
        }
        catch (Exception e) {
            log.error("Error getting code.", e);
        }
        return "";
    }

    private Boolean _showExecutionLogs = null;

    /**
     * Indicates whether execution logs should be shown. Manipulated by checkbox
     * on job results page, always false on recent jobs page.
     */
    public boolean isShowExecutionLogs() {
        if (_showExecutionLogs == null) {
            Set<UserProp> userProps = getUserProps();
            this._showExecutionLogs = Boolean.valueOf(UserDAO.getPropertyValue(
                    userProps, "showExecutionLogs", String.valueOf("false")));
        }
        return _showExecutionLogs;
    }

    /**
     * Loads a module from an output file.
     * 
     * @return
     */
    public String loadTask() {
        String lsid = UIBeanHelper.decode(UIBeanHelper.getRequest()
                .getParameter("module"));
        UIBeanHelper.getRequest().setAttribute("matchJob",
                UIBeanHelper.decode(UIBeanHelper.getRequest().getParameter(
                        "jobNumber")));
        UIBeanHelper.getRequest().setAttribute("outputFileName",
                UIBeanHelper.decode(UIBeanHelper.getRequest().getParameter(
                        "name")));
        UIBeanHelper.getRequest().setAttribute("outputFileDirName",
                UIBeanHelper.decode(UIBeanHelper.getRequest().getParameter(
                        "dirname")));
        UIBeanHelper.getRequest().setAttribute("outputFileSource",
                UIBeanHelper.decode(UIBeanHelper.getRequest().getParameter(
                        "source")));
        RunTaskBean runTaskBean = (RunTaskBean) UIBeanHelper
                .getManagedBean("#{runTaskBean}");           
        assert runTaskBean != null;
        runTaskBean.setTask(lsid);
        return "run task";
    }

    public String reload() {
        try {
            int jobNumber = Integer.parseInt(UIBeanHelper.decode(UIBeanHelper
                    .getRequest().getParameter("jobNumber")));
            AnalysisDAO ds = new AnalysisDAO();
            JobInfo reloadJob = ds.getJobInfo(jobNumber);
            RunTaskBean runTaskBean = (RunTaskBean) UIBeanHelper
                    .getManagedBean("#{runTaskBean}");
            assert runTaskBean != null;
            UIBeanHelper.getRequest().setAttribute("reloadJob",
                    String.valueOf(reloadJob.getJobNumber()));
            runTaskBean.setTask(reloadJob.getTaskLSID());
        }
        catch (Throwable t) {
            log.error("Error reloading job.", t);
        }
        return "run task";
    }

    public void saveFile(ActionEvent event) {
        HttpServletRequest request = UIBeanHelper.getRequest();

        String jobFileName = request.getParameter("jobFileName");
        String jobNumber2 = request.getParameter("jobNumber");
        String jobNumber3 = request.getParameter("jobNumberOverride");
        String jobNumber4 = request.getParameter("jnOverride");

        System.out.println("SAVE on job #" + jobNumber2 + "  " + jobNumber3
                + "  " + jobNumber4);
        System.out.println("SAVE on file #" + jobFileName);

        jobFileName = UIBeanHelper.decode(jobFileName);
        if (jobFileName == null || "".equals(jobFileName.trim())) {
            log.error("Error saving file, missing required parameter, 'jobFileName'.");
            return;
        }

        // parse jobFileName for <jobNumber> and <filename>, add support for
        // directories
        // from Job Summary page jobFileName="1/all_aml_test.preprocessed.gct"
        // from Job Status page
        // jobFileName="/gp/jobResults/1/all_aml_test.preprocessed.gct"
        String contextPath = request.getContextPath();
        String pathToJobResults = contextPath + "/jobResults/";
        if (jobFileName.startsWith(pathToJobResults)) {
            jobFileName = jobFileName.substring(pathToJobResults.length());
        }

        int idx = jobFileName.indexOf('/');
        if (idx <= 0) {
            log.error("Error saving file, invalid parameter, jobFileName="
                    + jobFileName);
            return;
        }
        String jobNumber = jobFileName.substring(0, idx);
        String filename = jobFileName.substring(idx + 1);
        File in = new File(GenePatternAnalysisTask.getJobDir(jobNumber),
                filename);
        if (!in.exists()) {
            UIBeanHelper
                    .setInfoMessage("File " + filename + " does not exist.");
            return;
        }
        InputStream is = null;
        try {
            HttpServletResponse response = UIBeanHelper.getResponse();
            response.setHeader("Content-Disposition", "attachment; filename="
                    + in.getName() + ";");
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
        this._showExecutionLogs = showExecutionLogs;
        new UserDAO().setProperty(UIBeanHelper.getUserId(),
                "showExecutionLogs", String.valueOf(showExecutionLogs));
        resetJobs();
    }

    public void terminateJob(ActionEvent event) {
        int jobNumber = -1;
        try {
            jobNumber = Integer.parseInt(UIBeanHelper.getRequest()
                    .getParameter("jobNumber"));
        }
        catch (NumberFormatException e) {
            log.error(UIBeanHelper.getRequest().getParameter("jobNumber")
                    + " is not a number.", e);
            return;
        }
        try {
            terminateJob(jobNumber);
        }
        catch (JobTerminationException e) {
            log.error(e);
            return;
        }
    }

    private void terminateJob(int jobNumber) throws JobTerminationException {
        String currentUser = UIBeanHelper.getUserId();
        boolean isAdmin = AuthorizationHelper.adminJobs(currentUser);
        JobManager.terminateJob(isAdmin, currentUser, jobNumber);
    }

    public void viewCode(ActionEvent e) {
        try {
            String language = UIBeanHelper.decode(UIBeanHelper.getRequest()
                    .getParameter("language"));
            int jobNumber = Integer.parseInt(UIBeanHelper.decode(UIBeanHelper
                    .getRequest().getParameter("jobNumber")));
            JobInfo jobInfo = new AnalysisDAO().getJobInfo(jobNumber);
            AnalysisJob job = new AnalysisJob(UIBeanHelper.getUserId(), jobInfo);
            viewCode(language, job, "" + jobNumber);
        }
        catch (Throwable t) {
            log.error("Error getting job "
                    + UIBeanHelper.getRequest().getParameter("jobNumber"), t);
        }
    }

    public void viewCode(String language, AnalysisJob job, String baseName) {
        OutputStream os = null;
        try {
            HttpServletResponse response = UIBeanHelper.getResponse();
            String filename = baseName
                    + CodeGeneratorUtil.getFileExtension(language);
            response.setHeader("Content-disposition", "inline; filename=\""
                    + filename + "\"");
            response.setHeader("Content-Type", "text/plain");
            response.setHeader("Cache-Control", "no-store"); // HTTP 1.1
            // cache
            // control
            response.setHeader("Pragma", "no-cache"); // HTTP 1.0 cache
            // control
            response.setDateHeader("Expires", 0);
            os = response.getOutputStream();

            IAdminClient adminClient = new LocalAdminClient(
                    UIBeanHelper.getUserId());
            TaskInfo taskInfo = adminClient.getTask(job.getLSID());

            String code = CodeGeneratorUtil.getCode(language, job, taskInfo,
                    adminClient);

            PrintWriter pw = new PrintWriter(os);
            pw.println(code);
            pw.flush();
            os.close();

            UIBeanHelper.getFacesContext().responseComplete();
        }
        catch (Exception e) {
            log.error("Error viewing code for job "
                    + job.getJobInfo().getJobNumber(), e);
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
            UIBeanHelper.setErrorMessage("Error deleting file: " + jobFileName);
            return;
        }
        int jobNumber = -1;
        String jobId = jobFileName.substring(0, idx);
        try {
            jobNumber = Integer.parseInt(jobId);
        }
        catch (NumberFormatException e) {
            UIBeanHelper.setErrorMessage("Error deleting file: " + jobFileName + ", " + e.getMessage());
            return;
        }
        try {
            // String filename = encodedJobFileName.substring(index + 1);
            String currentUserId = UIBeanHelper.getUserId();
            LocalAnalysisClient analysisClient = new LocalAnalysisClient(currentUserId);
            analysisClient.deleteJobResultFile(jobNumber, jobFileName);
            UIBeanHelper.setErrorMessage("Deleted file '" + jobFileName + "' from job #"+jobNumber);
        }
        catch (WebServiceException e) {
            UIBeanHelper.setErrorMessage("Error deleting file '" + jobFileName + "' from job #"+jobNumber+": "+ e.getMessage());
            return;
        }
    }

    /**
     * Delete the selected job, including child jobs, first terminating it if it
     * is running. This method deletes all output files and removes the job
     * entry from the database.
     */
    private List<Integer> deleteJob(int jobNumber) throws WebServiceException {
        boolean isAdmin = false;
        String userId = "";
        if (userSessionBean != null) {
            isAdmin = userSessionBean.isAdmin();
            userId = userSessionBean.getUserId();
        }
        return JobManager.deleteJob(isAdmin, userId, jobNumber);
    }

    private List<JobResultsWrapper> wrapJobs(List<JobInfo> jobInfos) {
        boolean isAdmin = false;
        if (userSessionBean != null) {
            isAdmin = userSessionBean.isAdmin();
        }
        String currentUserId = UIBeanHelper.getUserId();
        AdminDAO dao = new AdminDAO();
        TaskInfo[] latestTaskArray = dao.getLatestTasks(currentUserId);
        List<TaskInfo> latestTaskList = Arrays.asList(latestTaskArray);
        Map<String, Set<TaskInfo>> kindToModules = SemanticUtil
                .getKindToModulesMap(latestTaskList);
        final boolean showExecutionLogs = isShowExecutionLogs();
        List<JobResultsWrapper> wrappedJobs = new ArrayList<JobResultsWrapper>(
                jobInfos.size());
        for (JobInfo jobInfo : jobInfos) {
            PermissionsHelper ph = new PermissionsHelper(isAdmin,
                    currentUserId, jobInfo.getJobNumber(), jobInfo.getUserId(),
                    jobInfo.getJobNumber());
            JobPermissionsBean jobPermissionsBean = new JobPermissionsBean(ph);

            JobResultsWrapper wrappedJob = new JobResultsWrapper(
                    jobPermissionsBean, jobInfo, kindToModules,
                    getSelectedFiles(), getSelectedJobs(), 0, 0,
                    kindToInputParameters, showExecutionLogs);
            wrappedJob.setFileSortColumn(getFileSortColumn());
            wrappedJob.setFileSortAscending(isFileSortAscending());
            wrappedJobs.add(wrappedJob);
        }
        return wrappedJobs;
    }

    private JobSortOrder getJobSortOrder() {
        final String jobSortColumn = getJobSortColumn();
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
        if (recentJobs != null) {
            return recentJobs;
        }
        String userId = UIBeanHelper.getUserId();
        if (userId == null) {
            recentJobs = Collections.EMPTY_LIST;
            return recentJobs;
        }

        int recentJobsToShow = Integer.parseInt(UserDAO.getPropertyValue(
                getUserProps(), UserPropKey.RECENT_JOBS_TO_SHOW, "10"));
        AnalysisDAO ds = new AnalysisDAO();

        List<JobInfo> recentJobInfos = ds.getRecentJobsForUser(userId,
                recentJobsToShow, JobSortOrder.JOB_NUMBER);
        recentJobs = wrapJobs(new ArrayList<JobInfo>(recentJobInfos));
        return recentJobs;
    }

    public List<JobResultsWrapper> getPagedJobs() {
        final int pageSize = getPageSize();
        int offset = (getPageNumber() - 1) * pageSize;
        return this.getJobs(offset, pageSize);
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public int getPageNumber() {
        // HACK:
        try {
            this.pageNumber = Integer.parseInt(UIBeanHelper.getRequest()
                    .getParameter("page"));
        }
        catch (NumberFormatException nfe) {
            this.pageNumber = 1;
        }
        return this.pageNumber;
    }

    public void goToPage() {
        this.pageNumber = Integer.parseInt(UIBeanHelper.getRequest()
                .getParameter("page"));
    }

    public List<Integer> getPages() {
        final int MAX_PAGES = 25;
        int pageCount = getPageCount();
        int startNum = 1;
        int endNum = pageCount;
        if (pageCount > MAX_PAGES) {
            endNum = Math.max(getPageNumber() + (MAX_PAGES / 2), MAX_PAGES);
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

    public void previousPage() {
        this.pageNumber--;
    }

    public void nextPage() {
        this.pageNumber++;
    }

    private int pageCount = -1;

    public int getPageCount() {
        final int pageSize = getPageSize();
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
            // the first page is page 1
            final int pageNum = this.getPageNumber();
            final int pageSize = this.getPageSize();
            final JobSortOrder jobSortOrder = this.getJobSortOrder();
            final boolean ascending = isJobSortAscending();

            final String userId = UIBeanHelper.getUserId();
            boolean isAdmin = false;
            if (userSessionBean != null) {
                isAdmin = userSessionBean.isAdmin();
            }
            String selectedGroup = jobResultsFilterBean.getSelectedGroup();
            boolean showEveryonesJobs = jobResultsFilterBean
                    .isShowEveryonesJobs();

            boolean filterOnBatch = false;
            if (selectedGroup != null
                    && selectedGroup.startsWith(BatchJob.BATCH_KEY)) {
                filterOnBatch = true;
            }

            try {
                AnalysisDAO ds = new AnalysisDAO();
                List<JobInfo> jobInfos = new ArrayList<JobInfo>();
                if (filterOnBatch) {
                    JobInfo[] jobInfoArray = new BatchJobDAO().getBatchJobs(
                            userId, selectedGroup, maxJobNumber, maxEntries,
                            getJobSortOrder(), ascending);
                    // add all
                    for (JobInfo ji : jobInfoArray) {
                        jobInfos.add(ji);
                    }
                }
                else {
                    if (showEveryonesJobs) {
                        if (isAdmin) {
                            jobInfos = ds.getAllPagedJobsForAdmin(pageNum,
                                    pageSize, jobSortOrder, ascending);
                        }
                        else {
                            IGroupMembershipPlugin groupMembership = UserAccountManager
                                    .instance().getGroupMembership();
                            Set<String> groupIds = new HashSet<String>(
                                    groupMembership.getGroups(userId));
                            jobInfos = ds.getAllPagedJobsForUser(userId,
                                    groupIds, pageNum, pageSize, jobSortOrder,
                                    ascending);
                        }
                    }
                    else {
                        if (selectedGroup != null) {
                            jobInfos = ds.getPagedJobsInGroup(selectedGroup,
                                    pageNum, pageSize, jobSortOrder, ascending);
                        }
                        else {
                            jobInfos = ds.getPagedJobsOwnedByUser(userId,
                                    pageNum, pageSize, jobSortOrder, ascending);
                        }
                    }
                }
                allJobs = wrapJobs(jobInfos);
                
                for (JobResultsWrapper i : allJobs) {
                    i.setJobInfoWrapper(getJobInfoWrapper(i.getJobNumber()));
                }
            }
            catch (Exception e) {
                log.error(e);
                allJobs = new ArrayList<JobResultsWrapper>();
            }
        }
        return allJobs;
    }
    
    public JobInfoWrapper getJobInfoWrapper(int jobNumber) {
        String userId = UIBeanHelper.getUserId();
        HttpServletRequest request = UIBeanHelper.getRequest();
        String contextPath = request.getContextPath();
        String cookie = request.getHeader("Cookie");   
        JobInfoManager jobInfoManager = new JobInfoManager();
        return jobInfoManager.getJobInfo(cookie, contextPath, userId, jobNumber);
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
     * Get the list of selected files (pathnames) from the request parameters.
     * This is converted to a set to make membership tests efficient.
     * 
     * @return The selected files.
     */
    private Set<String> getSelectedFiles() {
        HashSet<String> selectedJobs = new HashSet<String>();
        String[] tmp = UIBeanHelper.getRequest().getParameterValues(
                "selectedFiles");
        if (tmp != null) {
            for (String job : tmp) {
                selectedJobs.add(job);
            }
        }
        return selectedJobs;
    }

    /**
     * Get the list of selected jobs (LSIDs) from the request parameters. This
     * is converted to a set to make membership tests efficient.
     * 
     * @return The selected jobs.
     */
    private Set<String> getSelectedJobs() {
        HashSet<String> selectedJobs = new HashSet<String>();
        String[] tmp = UIBeanHelper.getRequest().getParameterValues(
                "selectedJobs");
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
        String[] selectedJobs = UIBeanHelper.getRequest().getParameterValues(
                "selectedJobs");
        List<Integer> jobsToDelete = new ArrayList<Integer>();
        if (selectedJobs != null) {
            for (String selectedJob : selectedJobs) {
                try {
                    int jobNumber = Integer.parseInt(selectedJob);
                    jobsToDelete.add(jobNumber);
                }
                catch (NumberFormatException e) {
                    log.error(e);
                    UIBeanHelper.setErrorMessage("Error deleting job #"
                            + selectedJob + ": " + e.getLocalizedMessage());
                }
            }
        }

        String[] selectedFiles = UIBeanHelper.getRequest().getParameterValues(
                "selectedFiles");
        Map<Integer, List<JobFileEntry>> selectedFileEntries = new HashMap<Integer, List<JobFileEntry>>();
        if (selectedFiles != null) {
            for (String selectedFile : selectedFiles) {
                JobFileEntry entry = JobFileEntry.parse(selectedFile);
                if (entry != null) {
                    List<JobFileEntry> list = selectedFileEntries
                            .get(entry.jobNumber);
                    if (list == null) {
                        list = new ArrayList<JobFileEntry>();
                        selectedFileEntries.put(entry.jobNumber, list);
                    }
                    list.add(entry);
                }
            }
        }

        // delete the jobs
        List<Integer> deletedJobs = deleteJobs(jobsToDelete);

        // prevent duplicate deletion of files from deleted jobs
        for (Integer jobToDelete : deletedJobs) {
            List<JobFileEntry> list = selectedFileEntries.remove(jobToDelete);
            // for debugging only
            if (list != null) {
                for (JobFileEntry entry : list) {
                    log.debug("Ignoring duplicate fileEntry: "
                            + entry.jobNumber + ", " + entry.fileName);
                }
            }
        }

        // delete files
        for (List<JobFileEntry> list : selectedFileEntries.values()) {
            for (JobFileEntry entry : list) {
                deleteFile(entry.fileName);
            }
        }

        this.resetJobs();
        return null;
    }

    private static class JobFileEntry {
        Integer jobNumber;
        String fileName;

        private static JobFileEntry parse(String entry) {
            // parse encodedJobFileName for <jobNumber> and <filename>, add
            // support for directories
            // from Job Summary page
            // jobFileName="1/all_aml_test.preprocessed.gct"
            // from Job Status page
            // jobFileName="/gp/jobResults/1/all_aml_test.preprocessed.gct"
            String contextPath = UIBeanHelper.getRequest().getContextPath();
            String pathToJobResults = contextPath + "/jobResults/";
            if (entry.startsWith(pathToJobResults)) {
                entry = entry.substring(pathToJobResults.length());
            }
            int idx = entry.indexOf('/');
            if (idx <= 0) {
                UIBeanHelper.setErrorMessage("Error deleting file: " + entry);
                return null;
            }
            int jobNumber = -1;
            String jobId = entry.substring(0, idx);
            try {
                jobNumber = Integer.parseInt(jobId);
            }
            catch (NumberFormatException e) {
                UIBeanHelper.setErrorMessage("Error deleting file: " + entry
                        + ", " + e.getMessage());
                return null;
            }
            JobFileEntry jr = new JobFileEntry();
            jr.jobNumber = jobNumber;
            jr.fileName = entry;
            return jr;
        }
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
        if (fileSortColumn.equals(this._fileSortColumn)) {
            final boolean fileSortAscending = isFileSortAscending();
            this.setFileSortAscending(!fileSortAscending);
        }
        else {
            this.setFileSortColumn(fileSortColumn);
        }
    }

    public String sortJobsById() {
        final String jobSortColumn = this.getJobSortColumn();
        if ("jobNumber".equals(jobSortColumn)) {
            final boolean jobSortAscending = this.isJobSortAscending();
            this.setJobSortAscending(!jobSortAscending);
        }
        else {
            this.setJobSortColumn("jobNumber");
        }
        return "success";
    }

    public String sortJobsByModule() {
        final String jobSortColumn = this.getJobSortColumn();
        if ("taskName".equals(jobSortColumn)) {
            final boolean jobSortAscending = this.isJobSortAscending();
            this.setJobSortAscending(!jobSortAscending);
        }
        else {
            this.setJobSortColumn("taskName");
        }
        return "success";
    }

    public String sortJobsByDateSubmitted() {
        final String jobSortColumn = this.getJobSortColumn();
        if ("dateSubmitted".equals(jobSortColumn)) {
            final boolean jobSortAscending = this.isJobSortAscending();
            this.setJobSortAscending(!jobSortAscending);
        }
        else {
            this.setJobSortColumn("dateSubmitted");
        }
        return "success";
    }

    public String sortJobsByDateCompleted() {
        final String jobSortColumn = this.getJobSortColumn();
        if ("dateCompleted".equals(jobSortColumn)) {
            final boolean jobSortAscending = this.isJobSortAscending();
            this.setJobSortAscending(!jobSortAscending);
        }
        else {
            this.setJobSortColumn("dateCompleted");
        }
        return "success";
    }

    public String sortJobsByStatus() {
        final String jobSortColumn = this.getJobSortColumn();
        if ("status".equals(jobSortColumn)) {
            final boolean jobSortAscending = this.isJobSortAscending();
            this.setJobSortAscending(!jobSortAscending);
        }
        else {
            this.setJobSortColumn("status");
        }
        return "success";
    }

    private String _fileSortColumn = null;

    /**
     * Specifies file column to sort on. Possible values are name size
     * lastModified
     */
    public String getFileSortColumn() {
        if (_fileSortColumn == null) {
            this._fileSortColumn = UserDAO.getPropertyValue(getUserProps(),
                    "fileSortColumn", "name");

        }
        return this._fileSortColumn;
    }

    private String _jobSortColumn = null;

    /**
     * Specifies job column to sort on. Possible values are jobNumber taskName
     * dateSubmitted dateCompleted status
     */
    public String getJobSortColumn() {
        if (_jobSortColumn == null) {
            this._jobSortColumn = UserDAO.getPropertyValue(getUserProps(),
                    "jobSortColumn", "jobNumber");
        }
        return this._jobSortColumn;
    }

    private Boolean _fileSortAscending = null;

    /**
     * File sort direction (true for ascending, false for descending)
     */
    public boolean isFileSortAscending() {
        if (_fileSortAscending == null) {
            this._fileSortAscending = Boolean.valueOf(UserDAO.getPropertyValue(
                    getUserProps(), "fileSortAscending", "false"));
        }
        return _fileSortAscending;
    }

    public void setFileSortAscending(boolean fileSortAscending) {
        this._fileSortAscending = fileSortAscending;
        new UserDAO().setProperty(UIBeanHelper.getUserId(),
                "fileSortAscending", String.valueOf(fileSortAscending));
    }

    public void setFileSortColumn(String fileSortField) {
        this._fileSortColumn = fileSortField;
        new UserDAO().setProperty(UIBeanHelper.getUserId(), "fileSortColumn",
                String.valueOf(fileSortField));
    }

    public void setJobSortAscending(boolean jobSortAscending) {
        this._jobSortAscending = jobSortAscending;
        new UserDAO().setProperty(UIBeanHelper.getUserId(), "jobSortAscending",
                String.valueOf(jobSortAscending));
    }

    public void setJobSortColumn(String jobSortField) {
        this._jobSortColumn = jobSortField;
        new UserDAO().setProperty(UIBeanHelper.getUserId(), "jobSortColumn",
                String.valueOf(this._jobSortColumn));
    }

    /**
     * Jobs are always sorted, so there's nothing to do here. This is just an
     * action method to trigger a reload. Could probably be better named.
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
    private List<Integer> deleteJobs(List<Integer> jobNumbers) {
        if (jobNumbers == null) {
            log.error("Invalid null arg to deleteJobs");
            return Collections.EMPTY_LIST;
        }

        List<Integer> topLevelDeletedJobs = new ArrayList<Integer>();
        List<Integer> allDeletedJobs = new ArrayList<Integer>();
        for (Integer jobNumber : jobNumbers) {
            try {
                List<Integer> rval = deleteJob(jobNumber);
                topLevelDeletedJobs.add(jobNumber);
                allDeletedJobs.addAll(rval);
            }
            catch (WebServiceException e) {
                log.error(e);
                UIBeanHelper.setErrorMessage("Error deleting job #" + jobNumber
                        + ": " + e.getLocalizedMessage());
            }
        }
        if (topLevelDeletedJobs.size() == 1) {
            UIBeanHelper.setErrorMessage("Deleted job #"
                    + topLevelDeletedJobs.get(0));
        }
        else if (topLevelDeletedJobs.size() > 1) {
            UIBeanHelper.setErrorMessage("Deleted "
                    + topLevelDeletedJobs.size() + " jobs");
        }
        return allDeletedJobs;
    }

    /**
     * Job sort direction (true for ascending, false for descending)
     */
    private Boolean _jobSortAscending = null;

    public boolean isJobSortAscending() {
        if (_jobSortAscending == null) {
            this._jobSortAscending = Boolean.valueOf(UserDAO.getPropertyValue(
                    getUserProps(), "jobSortAscending", "false"));
        }
        return this._jobSortAscending;
    }

    public String getJobSortFlag() {
        if (isJobSortAscending()) {
            return "up";
        }
        else {
            return "down";
        }
    }

    public String getFileSortFlag() {
        if (isFileSortAscending()) {
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
        if (jobResultsFilterBean != null) {
            jobResultsFilterBean.resetJobCount();
        }
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

        public OutputFileInfo(ParameterInfo p, File file,
                Collection<TaskInfo> modules, int jobNumber, String kind) {
            this.kind = kind;
            this.p = p;
            this.size = file.length();
            this.exists = file.exists();
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(file.lastModified());
            this.lastModified = cal.getTime();

            if (modules != null) {
                for (TaskInfo t : modules) {
                    KeyValuePair mi = new KeyValuePair(t.getShortName(),
                            UIBeanHelper.encode(t.getLsid()));
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
            return p.getName();
        }

        public String getTruncatedDisplayName() {
            String name = "";
            if (p != null) {
                name = p.getName();
            }
            if (name != null && name.length() > 70) {
                name = name.substring(0, 35) + "..."
                        + name.substring(name.length() - 32);
            }
            return name;
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
         * @return a valid value to be used for the 'id' attribute of an html
         *         div tag. The '/' character is not allowed, so replace all '/'
         *         with '_'.
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
        if (selectedModule == null || selectedModule.length() == 0) {
            return;
        }
        List<JobResultsWrapper> recentJobs = getRecentJobs();
        if (recentJobs == null || recentJobs.size() == 0) {
            return;
        }

        AdminDAO adminDao = new AdminDAO();
        String currentUserId = UIBeanHelper.getUserId();
        TaskInfo taskInfo = adminDao.getTask(selectedModule, currentUserId);
        if (taskInfo == null) {
            log.error("Error getting TaskInfo for selectedModule="
                    + selectedModule);
            return;
        }

        kindToInputParameters = new HashMap<String, List<KeyValuePair>>();

        ParameterInfo[] inputParameters = taskInfo != null ? taskInfo
                .getParameterInfoArray() : null;
        List<KeyValuePair> unannotatedParameters = new ArrayList<KeyValuePair>();
        if (inputParameters != null) {
            for (ParameterInfo inputParameter : inputParameters) {
                if (inputParameter.isInputFile()) {
                    List<String> fileFormats = SemanticUtil
                            .getFileFormats(inputParameter);
                    String displayValue = (String) inputParameter
                            .getAttributes().get("altName");

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
                        List<KeyValuePair> inputParameterNames = kindToInputParameters
                                .get(format);
                        if (inputParameterNames == null) {
                            inputParameterNames = new ArrayList<KeyValuePair>();
                            kindToInputParameters.put(format,
                                    inputParameterNames);
                        }
                        inputParameterNames.add(kvp);
                    }
                }
            }
        }

        // add unannotated parameters to end of list for each kind
        if (unannotatedParameters.size() > 0) {
            for (Iterator<String> it = kindToInputParameters.keySet()
                    .iterator(); it.hasNext();) {
                List<KeyValuePair> inputParameterNames = kindToInputParameters
                        .get(it.next());
                inputParameterNames.addAll(unannotatedParameters);
            }
        }

        for (JobResultsWrapper job : recentJobs) {
            List<OutputFileInfo> outputFiles = job
                    .getOutputFileParameterInfos();
            if (outputFiles != null) {
                for (OutputFileInfo o : outputFiles) {
                    List<KeyValuePair> moduleInputParameters = kindToInputParameters
                            .get(o.getKind());

                    if (moduleInputParameters == null) {
                        moduleInputParameters = unannotatedParameters;
                    }
                    o.moduleInputParameters = moduleInputParameters;
                }
            }
        }
    }

}
