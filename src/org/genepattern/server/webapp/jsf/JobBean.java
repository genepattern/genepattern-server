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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.genepattern.codegenerator.CodeGeneratorUtil;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.user.UserDAO;
import org.genepattern.server.user.UserPropKey;
import org.genepattern.server.util.AuthorizationManagerFactory;
import org.genepattern.server.webservice.server.dao.AdminDAO;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.server.webservice.server.local.LocalAnalysisClient;
import org.genepattern.util.GPConstants;
import org.genepattern.util.SemanticUtil;
import org.genepattern.util.StringUtils;
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

    /**
     * Indicates whether execution logs should be shown. Manipulated by checkbox on job results page, always false on
     * recent jobs page.
     */
    private boolean showExecutionLogs = false;

    /**
     * File sort direction (true for ascending, false for descending)
     */
    private boolean fileSortAscending = true;

    /**
     * Specifies file column to sort on. Possible values are name size lastModified
     */
    private String fileSortColumn = "name";

    private boolean showEveryonesJobs = true;

    /**
     * Specifies job column to sort on. Possible values are jobNumber taskName dateSubmitted dateCompleted status
     */
    private String jobSortColumn = "jobNumber";

    /**
     * Job sort direction (true for ascending, false for descending)
     */
    private boolean jobSortAscending = true;

    public JobBean() {
        TaskInfo[] tasks = new AdminDAO().getAllTasksForUser(UIBeanHelper.getUserId());
        kindToModules = SemanticUtil.getKindToModulesMap(tasks);
        String userId = UIBeanHelper.getUserId();
        this.showExecutionLogs = Boolean.valueOf(new UserDAO().getPropertyValue(userId, "showExecutionLogs", String
                .valueOf(showExecutionLogs)));

        // Attributes to support job results page
        this.fileSortAscending = Boolean.valueOf(new UserDAO().getPropertyValue(userId, "fileSortAscending", String
                .valueOf(fileSortAscending)));
        this.fileSortColumn = new UserDAO().getPropertyValue(userId, "fileSortColumn", fileSortColumn);
        this.showEveryonesJobs = Boolean.valueOf(new UserDAO().getPropertyValue(userId, "showEveryonesJobs", String
                .valueOf(showEveryonesJobs)));
        if (showEveryonesJobs
                && !AuthorizationManagerFactory.getAuthorizationManager().checkPermission("administrateServer",
                        UIBeanHelper.getUserId())) {
            showEveryonesJobs = false;

        }
        this.jobSortColumn = new UserDAO().getPropertyValue(userId, "jobSortColumn", jobSortColumn);
        this.jobSortAscending = Boolean.valueOf(new UserDAO().getPropertyValue(userId, "jobSortAscending", String
                .valueOf(jobSortAscending)));

    }

    public void createPipeline(ActionEvent e) {
        try {
            String jobNumber = UIBeanHelper.decode(UIBeanHelper.getRequest().getParameter("jobNumber"));
            if (jobNumber == null) {
                UIBeanHelper.setInfoMessage("No job specified.");
                return;
            }
            System.out.println(jobNumber);
            String pipelineName = "job" + jobNumber; // TODO prompt user for
            // name
            String lsid = new LocalAnalysisClient(UIBeanHelper.getUserId()).createProvenancePipeline(jobNumber,
                    pipelineName);

            if (lsid == null) {
                UIBeanHelper.setInfoMessage("Unable to create pipeline.");
                return;
            }
            UIBeanHelper.getResponse().sendRedirect(
                    UIBeanHelper.getRequest().getContextPath() + "/pipelineDesigner.jsp?name="
                            + UIBeanHelper.encode(lsid));
        } catch (WebServiceException wse) {
            log.error("Error creating pipeline.", wse);
        } catch (IOException e1) {
            log.error("Error creating pipeline.", e1);
        }
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
            resetJobs();
        } catch (NumberFormatException e) {
            log.error("Error deleting job.", e);
        }
    }

    public void deleteFile(ActionEvent event) throws WebServiceException {
        String value = UIBeanHelper.decode(UIBeanHelper.getRequest().getParameter("jobFile"));
        deleteFile(value);
        resetJobs();
    }

    public void downloadZip(ActionEvent event) {

        try {
            int jobNumber = Integer.parseInt(UIBeanHelper.decode(UIBeanHelper.getRequest().getParameter("jobNumber")));
            LocalAnalysisClient client = new LocalAnalysisClient(UIBeanHelper.getUserId());
            JobInfo job = client.checkStatus(jobNumber);
            if (job == null) {
                return;
            }

            JobInfo[] children = client.getChildren(jobNumber);

            List<ParameterInfo> outputFileParameters = new ArrayList<ParameterInfo>();
            if (children.length > 0) {
                for (JobInfo child : children) {
                    outputFileParameters.addAll(getOutputParameters(child));
                }

            } else {
                outputFileParameters.addAll(getOutputParameters(job));

            }

            HttpServletResponse response = UIBeanHelper.getResponse();
            response.setHeader("Content-Disposition", "attachment; filename=" + jobNumber + ".zip" + ";");
            response.setHeader("Content-Type", "application/octet-stream");
            // response.setHeader("Content-Type", "application/zip");
            response.setHeader("Cache-Control", "no-store"); // HTTP 1.1
            // cache
            // control
            response.setHeader("Pragma", "no-cache"); // HTTP 1.0 cache
            // control
            response.setDateHeader("Expires", 0);
            OutputStream os = response.getOutputStream();
            ZipOutputStream zos = new ZipOutputStream(os);

            String jobDir = System.getProperty("jobs");
            byte[] b = new byte[10000];
            for (ParameterInfo p : outputFileParameters) {
                String value = p.getValue();
                int index = StringUtils.lastIndexOfFileSeparator(value);

                String jobId = value.substring(0, index);
                String fileName = UIBeanHelper.decode(value.substring(index + 1, value.length()));
                File attachment = new File(jobDir + File.separator + value);
                if (!attachment.exists()) {
                    continue;
                }
                ZipEntry zipEntry = new ZipEntry((jobId.equals("" + jobNumber) ? "" : (jobNumber + "/")) + fileName);

                zos.putNextEntry(zipEntry);
                zipEntry.setTime(attachment.lastModified());
                zipEntry.setSize(attachment.length());
                FileInputStream is = null;
                try {
                    is = new FileInputStream(attachment);
                    int bytesRead;
                    while ((bytesRead = is.read(b, 0, b.length)) != -1) {
                        zos.write(b, 0, bytesRead);
                    }
                } finally {
                    if (is != null) {
                        is.close();
                    }
                }
                zos.closeEntry();

            }
            zos.flush();
            zos.close();
            os.close();
            UIBeanHelper.getFacesContext().responseComplete();
        } catch (IOException e) {
            log.error("Error downloading zip.", e);
        } catch (WebServiceException e) {
            log.error("Error downloading zip.", e);
        }

    }


    public String getTaskCode() {
        try {
            String language = UIBeanHelper.decode(UIBeanHelper.getRequest().getParameter("language"));
            String lsid = UIBeanHelper.decode(UIBeanHelper.getRequest().getParameter("taskLSID"));

            TaskInfo taskInfo = new LocalAdminClient(UIBeanHelper.getUserId()).getTask(lsid);
            if (taskInfo == null) {
                return "Task not found";
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

            AnalysisJob job = new AnalysisJob(System.getProperty("GenePatternURL"), jobInfo, JobBean
                    .isVisualizer(taskInfo));

            return CodeGeneratorUtil.getCode(language, job);
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

    public String loadTask(ActionEvent event) {
        String lsid = UIBeanHelper.decode(UIBeanHelper.getRequest().getParameter("module"));
        UIBeanHelper.getRequest().setAttribute("matchJob",
                UIBeanHelper.decode(UIBeanHelper.getRequest().getParameter("jobNumber")));
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
        InputStream is = null;

        try {
            String value = UIBeanHelper.decode(UIBeanHelper.getRequest().getParameter("jobFileName"));
            int index = StringUtils.lastIndexOfFileSeparator(value);
            String jobNumber = value.substring(0, index);
            String filename = value.substring(index + 1);
            File in = new File(GenePatternAnalysisTask.getJobDir(jobNumber), filename);
            if (!in.exists()) {
                UIBeanHelper.setInfoMessage("File " + filename + " does not exist.");
                return;
            }
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
        } catch (IOException e) {
            log.error("Error saving file.", e);

        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {

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
        try {
            String code = CodeGeneratorUtil.getCode(language, job);
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

            OutputStream os = response.getOutputStream();
            PrintWriter pw = new PrintWriter(os);
            pw.println(code);
            pw.flush();
            os.close();

            UIBeanHelper.getFacesContext().responseComplete();
        } catch (Exception e) {
            log.error("Error viewing code for job " + job.getJobInfo().getJobNumber(), e);
        }
    }

    protected void deleteFile(String encodedJobFileName) {
        try {
            int index = StringUtils.lastIndexOfFileSeparator(encodedJobFileName);
            int jobNumber = Integer.parseInt(encodedJobFileName.substring(0, index));
            // String filename = encodedJobFileName.substring(index + 1);
            new LocalAnalysisClient(UIBeanHelper.getUserId()).deleteJobResultFile(jobNumber, encodedJobFileName);
        } catch (NumberFormatException e) {
            log.error("Error parsing " + encodedJobFileName, e);
        } catch (WebServiceException e) {
            log.error("Error deleting file.", e);
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
        } catch (WebServiceException e) {
            log.error("Error deleting job " + jobNumber, e);
        }
    }

    private List<JobResultsWrapper> wrapJobs(JobInfo[] jobInfoArray) {
        List<JobResultsWrapper> wrappedJobs = new ArrayList<JobResultsWrapper>(jobInfoArray.length);
        for (int i = 0; i < jobInfoArray.length; i++) {
            JobResultsWrapper wrappedJob = new JobResultsWrapper(jobInfoArray[i], kindToModules, getSelectedFiles(),
                    getSelectedJobs());
            wrappedJobs.add(wrappedJob);
        }
        return wrappedJobs;
    }

    public List<JobResultsWrapper> getRecentJobs() {
        if (recentJobs == null) {
            String userId = UIBeanHelper.getUserId();
            assert userId != null;
            int recentJobsToShow = Integer.parseInt(new UserDAO().getPropertyValue(userId,
                    UserPropKey.RECENT_JOBS_TO_SHOW, "4"));
            LocalAnalysisClient analysisClient = new LocalAnalysisClient(userId);
            try {
                recentJobs = wrapJobs(analysisClient.getJobs(userId, -1, recentJobsToShow, false));

            } catch (WebServiceException wse) {
                log.error(wse);
                recentJobs = new ArrayList<JobResultsWrapper>();
            }
        }
        return recentJobs;
    }

    public List<JobResultsWrapper> getAllJobs() {
        if (allJobs == null) {
            String userId = UIBeanHelper.getUserId();
            LocalAnalysisClient analysisClient = new LocalAnalysisClient(userId);
            try {
                allJobs = wrapJobs(analysisClient.getJobs(showEveryonesJobs ? null : userId, -1, Integer.MAX_VALUE,
                        false));
            } catch (WebServiceException wse) {
                log.error(wse);
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

    public static boolean isVisualizer(TaskInfo taskInfo) {
        return "visualizer".equalsIgnoreCase((String) taskInfo.getTaskInfoAttributes().get(GPConstants.TASK_TYPE));
    }

    /**
     * Delete the selected jobs and files.
     * 
     * @return
     */
    public String delete() {
        String[] selectedJobs = UIBeanHelper.getRequest().getParameterValues("selectedJobs");
        deleteJobs(selectedJobs);

        String[] selectedFiles = UIBeanHelper.getRequest().getParameterValues("selectedFiles");
        if (selectedFiles != null) {
            for (String jobFileName : selectedFiles) {
                deleteFile(jobFileName);
            }
        }
        this.resetJobs();
        return null;

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

    public boolean isShowEveryonesJobs() {
        return showEveryonesJobs;
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

    public void setShowEveryonesJobs(boolean showEveryonesJobs) {
        if (showEveryonesJobs
                && !AuthorizationManagerFactory.getAuthorizationManager().checkPermission("administrateServer",
                        UIBeanHelper.getUserId())) {
            showEveryonesJobs = false;

        }
        this.showEveryonesJobs = showEveryonesJobs;
        new UserDAO().setProperty(UIBeanHelper.getUserId(), "showEveryonesJobs", String.valueOf(showEveryonesJobs));
        this.resetJobs();
    }

    /**
     * Action method. First sorts jobs, then for each job sorts files.
     * 
     * @return
     */
    public String sort() {

        sortJobs();
        sortFiles();
        return null;
    }

    /**
     * Delete a list of jobs.
     * 
     * @param jobNumbers
     */
    private void deleteJobs(String[] jobNumbers) {
        LocalAnalysisClient analysisClient = new LocalAnalysisClient(UIBeanHelper.getUserId());
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

    private void sortFiles() {
        final String column = getFileSortColumn();
        Comparator comparator = new Comparator() {

            public int compare(Object o1, Object o2) {
                OutputFileInfo c1 = (OutputFileInfo) o1;
                OutputFileInfo c2 = (OutputFileInfo) o2;
                if (column == null) {
                    return 0;
                } else if (column.equals("name")) {
                    return fileSortAscending ? c1.getName().compareTo(c2.getName()) : c2.getName().compareTo(
                            c1.getName());
                } else if (column.equals("size")) {
                    return fileSortAscending ? new Long(c1.getSize()).compareTo(c2.getSize()) : new Long(c2.getSize())
                            .compareTo(c1.getSize());
                } else if (column.equals("lastModified")) {
                    return fileSortAscending ? c1.getLastModified().compareTo(c2.getLastModified()) : c2
                            .getLastModified().compareTo(c1.getLastModified());
                }

                else {
                    return 0;
                }
            }
        };

        for (JobResultsWrapper jobResult : getAllJobs()) {
            sortFilesRecursive(jobResult, comparator);
        }

    }
    private void sortFilesRecursive(JobResultsWrapper jobResult, Comparator comparator) {
        Collections.sort(jobResult.getOutputFileParameterInfos(), comparator);
        for (JobResultsWrapper child : jobResult.getChildJobs()) {
            sortFilesRecursive(child, comparator);
        }

    }


    private void sortJobs() {
        final String column = getJobSortColumn();
        Comparator comparator = new Comparator() {

            public int compare(Object o1, Object o2) {
                JobResultsWrapper c1 = ((JobResultsWrapper) o1);
                JobResultsWrapper c2 = ((JobResultsWrapper) o2);
                if (column == null) {
                    return 0;
                } else if (column.equals("jobNumber")) {
                    return jobSortAscending ? new Integer(c1.getJobNumber()).compareTo(c2.getJobNumber())
                            : new Integer(c2.getJobNumber()).compareTo(c1.getJobNumber());
                } else if (column.equals("taskName")) {
                    return jobSortAscending ? c1.getTaskName().compareTo(c2.getTaskName()) : c2.getTaskName()
                            .compareTo(c1.getTaskName());
                } else if (column.equals("status")) {
                    return jobSortAscending ? c1.getStatus().compareTo(c2.getStatus()) : c2.getStatus().compareTo(
                            c1.getStatus());
                } else if (column.equals("dateCompleted")) {
                    if (c1.getDateCompleted() == null) {
                        if (c2.getDateCompleted() == null) {
                            return 0;
                        }
                        return jobSortAscending ? -1 : 1;
                    } else if (c2.getDateCompleted() == null) {
                        return jobSortAscending ? 1 : -1;
                    }

                    return jobSortAscending ? c1.getDateCompleted().compareTo(c2.getDateCompleted()) : c2
                            .getDateCompleted().compareTo(c1.getDateCompleted());
                } else if (column.equals("dateSubmitted")) {
                    return jobSortAscending ? c1.getDateSubmitted().compareTo(c2.getDateSubmitted()) : c2
                            .getDateSubmitted().compareTo(c1.getDateSubmitted());
                } else {
                    return 0;
                }
            }
        };
        
        Collections.sort(getAllJobs(), comparator);
    }

    public boolean isJobSortAscending() {
        return jobSortAscending;
    }
    
    /**
     * Force an update of the job list by nulling the current values.
     */
    private void resetJobs() {
        recentJobs = null;
        allJobs = null;
    }

    /**
     * Represents a job result. Wraps JobInfo and adds methods for getting the output files and the expansion state of
     * the associated UI panel
     */
    public class JobResultsWrapper {

        private List<JobResultsWrapper> childJobs;

        private JobInfo jobInfo;

        private int level = 0;

        private List<OutputFileInfo> outputFiles;

        private boolean selected = false;

        private int sequence = 0;

        public JobResultsWrapper(JobInfo jobInfo, Map<String, Collection<TaskInfo>> kindToModules,
                Set<String> selectedFiles, Set<String> selectedJobs) {
            this(jobInfo, kindToModules, selectedFiles, selectedJobs, 0, 0);
        }

        public JobResultsWrapper(JobInfo jobInfo, Map<String, Collection<TaskInfo>> kindToModules,
                Set<String> selectedFiles, Set<String> selectedJobs, int level, int sequence) {

            this.jobInfo = jobInfo;
            this.selected = selectedJobs.contains(String.valueOf(jobInfo.getJobNumber()));
            this.level = level;
            this.sequence = sequence;

            // Build the list of output files from the parameter info array.

            outputFiles = new ArrayList<OutputFileInfo>();
            ParameterInfo[] parameterInfoArray = jobInfo.getParameterInfoArray();
            if (parameterInfoArray != null) {
                File outputDir = new File(GenePatternAnalysisTask.getJobDir("" + jobInfo.getJobNumber()));
                for (int i = 0; i < parameterInfoArray.length; i++) {
                    if (parameterInfoArray[i].isOutputFile()) {
                        if (showExecutionLogs || !parameterInfoArray[i].getName().equals("gp_task_execution_log.txt")) {
                            File file = new File(outputDir, parameterInfoArray[i].getName());
                            Collection<TaskInfo> modules = kindToModules.get(SemanticUtil.getKind(file));
                            OutputFileInfo pInfo = new OutputFileInfo(parameterInfoArray[i], file, modules, jobInfo
                                    .getJobNumber());
                            pInfo.setSelected(selectedFiles.contains(pInfo.getValue()));
                            outputFiles.add(pInfo);
                        }
                    }
                }
            }

            // Child jobs
            childJobs = new ArrayList<JobResultsWrapper>();
            String userId = UIBeanHelper.getUserId();
            LocalAnalysisClient analysisClient = new LocalAnalysisClient(userId);
            try {
                JobInfo[] children = analysisClient.getChildren(jobInfo.getJobNumber());
                int seq = 1;
                int childLevel = getLevel() + 1;
                for (JobInfo child : children) {
                    childJobs.add(new JobResultsWrapper(child, kindToModules, selectedFiles, selectedJobs, childLevel,
                            seq));
                    seq++;
                }
            } catch (WebServiceException e) {
                log.error("Error getting child jobs", e);

            }
        }

        /**
         * Returns a list all descendant jobs, basically a flattened tree.
         * 
         * @return The descendant jobs.
         */
        public List<JobResultsWrapper> getDescendantJobs() {
            List<JobResultsWrapper> descendantJobs = new ArrayList<JobResultsWrapper>();
            descendantJobs.addAll(childJobs);
            for (JobResultsWrapper childJob : childJobs) {
                descendantJobs.addAll(childJob.getDescendantJobs());
            }
            return descendantJobs;
        }

        public List<OutputFileInfo> getAllFileInfos() {
            List<OutputFileInfo> allFiles = new ArrayList<OutputFileInfo>();
            allFiles.addAll(outputFiles);
            for (JobResultsWrapper child : childJobs) {
                allFiles.addAll(child.getAllFileInfos());
            }
            return allFiles;
        }

        public List<JobResultsWrapper> getChildJobs() {
            return childJobs;
        }

        public Date getDateCompleted() {
            return jobInfo.getDateCompleted();
        }

        public Date getDateSubmitted() {
            return jobInfo.getDateSubmitted();
        }

        public int getJobNumber() {
            return jobInfo.getJobNumber();
        }

        public int getLevel() {
            return level;
        }

        public List<OutputFileInfo> getOutputFileParameterInfos() {
            return outputFiles;
        }

        public int getSequence() {
            return sequence;
        }

        public String getStatus() {
            return jobInfo.getStatus();
        }

        public int getTaskID() {
            return jobInfo.getTaskID();
        }

        public String getTaskLSID() {
            return jobInfo.getTaskLSID();
        }

        public String getTaskName() {
            return jobInfo.getTaskName();
        }

        public String getUserId() {
            return jobInfo.getUserId();
        }

        /**
         * boolean property used to conditionally render or enable some menu items.
         * 
         * @return Whether the job is complete.
         */
        public boolean isComplete() {
            String status = jobInfo.getStatus();
            return status.equalsIgnoreCase("Finished") || status.equalsIgnoreCase("Error");
        }

        /**
         * This property supports saving of the "expanded" state of the job across requests. It is used to initialize
         * display properties of rows associated with this job.
         * 
         * @return
         */
        public boolean isExpanded() {
            String parameterName = "expansion_state_" + jobInfo.getJobNumber();
            String value = UIBeanHelper.getRequest().getParameter(parameterName);
            return (value == null || value.equals("true"));
        }

        public boolean isSelected() {
            return selected;
        }

        public void setLevel(int level) {
            this.level = level;
        }

        public void setSelected(boolean bool) {
            this.selected = bool;
        }

        public void setSequence(int sequence) {
            this.sequence = sequence;
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

        public OutputFileInfo(ParameterInfo p, File file, Collection<TaskInfo> modules, int jobNumber) {
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

        public long getSize() {
            return size;
        }

        public String getUIValue(ParameterInfo formalParam) {
            return p.getUIValue(formalParam);
        }

        public String getValue() {
            return p.getValue();
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
    }

    private static class KeyValueComparator implements Comparator<KeyValuePair> {

        public int compare(KeyValuePair o1, KeyValuePair o2) {
            return o1.getKey().compareToIgnoreCase(o2.getKey());
        }

    }

}