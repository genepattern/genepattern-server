package org.genepattern.server.webapp.jsf;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.NumberFormat;
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

import javax.faces.event.ActionEvent;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.genepattern.codegenerator.CodeGeneratorUtil;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.webservice.server.dao.AdminDAO;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.server.webservice.server.local.LocalAnalysisClient;
import org.genepattern.util.GPConstants;
import org.genepattern.util.SemanticUtil;
import org.genepattern.webservice.AnalysisJob;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.WebServiceException;

public abstract class JobBean {
	private static Logger log = Logger.getLogger(JobBean.class);

	private List<JobResultsWrapper> jobs;

	Map<String, Collection<TaskInfo>> kindToModules;

	/**
	 * Indicates whether execution logs should be shown.  Manipulated by
	 * checkbox on job results page,  always false on recent jobs page.
	 */
	private boolean showExecutionLogs = false;

	/**
	 * Job sort direction (true for ascending, false for descending)
	 */
	protected boolean jobSortAscending = true;

	public JobBean() {
		TaskInfo[] tasks = new AdminDAO().getAllTasksForUser(UIBeanHelper
				.getUserId());
		kindToModules = SemanticUtil.getKindToModulesMap(tasks);
	}

	abstract protected JobInfo[] getJobInfos();

	public boolean isShowExecutionLogs() {
		return showExecutionLogs;
	}

	public void setShowExecutionLogs(boolean showExecutionLogs) {
		this.showExecutionLogs = showExecutionLogs;
		updateJobs();
	}

	protected void updateJobs() {
		JobInfo[] jobInfoArray = getJobInfos();
		jobs = new ArrayList<JobResultsWrapper>(jobInfoArray.length);
		for (int i = 0; i < jobInfoArray.length; i++) {
			JobResultsWrapper wrappedJob = new JobResultsWrapper(jobInfoArray[i],
					kindToModules, getSelectedFiles(), getSelectedJobs());
			jobs.add(wrappedJob);
		}
	}

	public String loadTask(ActionEvent event) {
		String lsid = UIBeanHelper.decode(UIBeanHelper.getRequest()
				.getParameter("module"));
		RunTaskBean runTaskBean = (RunTaskBean) UIBeanHelper
				.getManagedBean("#{runTaskBean}");
		assert runTaskBean != null;
		runTaskBean.setTask(lsid);
		return "run task";
	}

	/**
	 * Get the list of selected jobs (LSIDs) from the request parameters.  This
	 * is converted to a set to make membership tests efficient.
	 * @return
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
	 * Get the list of selected files (pathnames) from the request parameters.  This
	 * is converted to a set to make membership tests efficient.
	 * @return
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

	public List<JobResultsWrapper> getJobs() {
		if (jobs == null) {
			updateJobs();
		}
		return jobs;
	}

	public void createPipeline(ActionEvent e) {
		try {
			String jobNumber = UIBeanHelper.decode(UIBeanHelper.getRequest()
					.getParameter("jobNumber"));
			String pipelineName = "job" + jobNumber; // TODO prompt user for
			// name
			String lsid = new LocalAnalysisClient(UIBeanHelper.getUserId())
					.createProvenancePipeline(jobNumber, pipelineName);

			if (lsid == null) {
				UIBeanHelper.setInfoMessage("Unable to create pipeline.");
				return;
			}
			UIBeanHelper.getResponse().sendRedirect(
					UIBeanHelper.getRequest().getContextPath()
							+ "/pipelineDesigner.jsp?name="
							+ UIBeanHelper.encode(lsid));
		} catch (IOException e1) {
			log.error(e1);
		}

	}

	public String reload() {
		LocalAnalysisClient ac = new LocalAnalysisClient(UIBeanHelper
				.getUserId());
		try {
			int jobNumber = Integer.parseInt(UIBeanHelper.decode(UIBeanHelper
					.getRequest().getParameter("jobNumber")));
			JobInfo reloadJob = ac.getJob(jobNumber);
			RunTaskBean runTaskBean = (RunTaskBean) UIBeanHelper
					.getManagedBean("#{runTaskBean}");
			assert runTaskBean != null;
			UIBeanHelper.getRequest().setAttribute("reloadJob",
					String.valueOf(reloadJob.getJobNumber()));
			runTaskBean.setTask(reloadJob.getTaskLSID());
		} catch (WebServiceException e) {
			log.error(e);
		} catch (NumberFormatException e) {
			log.error(e);
		}
		return "run task";
	}

	public void deleteFile(ActionEvent event) throws WebServiceException {
		String value = UIBeanHelper.decode(UIBeanHelper.getRequest()
				.getParameter("jobFile"));
		deleteFile(value);
	}

	protected void deleteFile(String encodedJobFileName) {
		try {
			int index = encodedJobFileName.indexOf("/");
			int jobNumber = Integer.parseInt(encodedJobFileName.substring(0,
					index));
			String filename = encodedJobFileName.substring(index + 1);
			new LocalAnalysisClient(UIBeanHelper.getUserId())
					.deleteJobResultFile(jobNumber, jobNumber + "/" + filename);
		} catch (NumberFormatException e) {
			log.error(e);
		} catch (WebServiceException e) {
			log.error(e);
		}
	}

	public void saveFile(ActionEvent event) {
		InputStream is = null;

		try {
			String value = UIBeanHelper.decode(UIBeanHelper.getRequest()
					.getParameter("jobFileName"));
			int index = value.indexOf("/");
			String jobNumber = value.substring(0, index);
			String filename = value.substring(index + 1);
			File in = new File(GenePatternAnalysisTask.getJobDir(jobNumber),
					filename);
			if (!in.exists()) {
				UIBeanHelper.setInfoMessage("File " + filename
						+ " does not exist.");
				return;
			}
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
		} catch (IOException e) {
			log.error(e);

		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					log.error(e);
				}
			}
		}

	}

	public void downloadZip(ActionEvent event) {

		try {
			int jobNumber = Integer.parseInt(UIBeanHelper.decode(UIBeanHelper
					.getRequest().getParameter("jobNumber")));
			LocalAnalysisClient client = new LocalAnalysisClient(UIBeanHelper
					.getUserId());
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
			response.setHeader("Content-Disposition", "attachment; filename="
					+ jobNumber + ".zip" + ";");
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
				int index = value.lastIndexOf("/");
				if (index == -1) {
					index = value.lastIndexOf("\\");
				}
				String jobId = value.substring(0, index);
				String fileName = UIBeanHelper.decode(value.substring(
						index + 1, value.length()));
				File attachment = new File(jobDir + File.separator + value);
				if (!attachment.exists()) {
					continue;
				}
				ZipEntry zipEntry = new ZipEntry(
						(jobId.equals("" + jobNumber) ? "" : (jobNumber + "/"))
								+ fileName);

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
			log.error(e);
		} catch (WebServiceException e) {
			log.error(e);
		}

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
	 * Delete the selected job. Should this also delete the files?
	 * 
	 * @param event
	 */
	public void delete(ActionEvent event) {
		try {
			int jobNumber = Integer.parseInt(UIBeanHelper.decode(UIBeanHelper
					.getRequest().getParameter("jobNumber")));
			deleteJob(jobNumber);
		} catch (NumberFormatException e) {
			log.error(e);
		}
	}

	/**
	 * Delete the selected job. Should this also delete the files?
	 * 
	 * @param event
	 */
	protected void deleteJob(int jobNumber) {
		try {
			LocalAnalysisClient ac = new LocalAnalysisClient(UIBeanHelper
					.getUserId());
			ac.deleteJob(jobNumber);
			HibernateUtil.getSession().flush();

		} catch (WebServiceException e) {
			log.error(e);
		}
	}

	public String getTaskCode() {
		try {
			String language = UIBeanHelper.decode(UIBeanHelper.getRequest()
					.getParameter("language"));
			String lsid = UIBeanHelper.decode(UIBeanHelper.getRequest()
					.getParameter("taskLSID"));

			TaskInfo taskInfo = new LocalAdminClient(UIBeanHelper.getUserId())
					.getTask(lsid);
			if (taskInfo == null) {
				return "Task not found";
			}
			ParameterInfo[] parameters = taskInfo.getParameterInfoArray();

		        ParameterInfo[] jobParameters = new ParameterInfo[parameters != null ? parameters.length: 0];

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
					jobParameters, UIBeanHelper.getUserId(), lsid, taskInfo
							.getName());

			AnalysisJob job = new AnalysisJob(System
					.getProperty("GenePatternURL"), jobInfo, JobBean
					.isVisualizer(taskInfo));

			return CodeGeneratorUtil.getCode(language, job);
		} catch (WebServiceException e) {
			log.error(e);
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e);
		}
		return "";
	}

	public static boolean isVisualizer(TaskInfo taskInfo) {
		return "visualizer".equalsIgnoreCase((String) taskInfo
				.getTaskInfoAttributes().get(GPConstants.TASK_TYPE));
	}

	public void viewCode(ActionEvent e) {
		try {
			String language = UIBeanHelper.decode(UIBeanHelper.getRequest()
					.getParameter("language"));
			int jobNumber = Integer.parseInt(UIBeanHelper.decode(UIBeanHelper
					.getRequest().getParameter("jobNumber")));
			AnalysisJob job = new AnalysisJob(UIBeanHelper.getUserId(),
					new LocalAnalysisClient(UIBeanHelper.getUserId())
							.getJob(jobNumber));
			viewCode(language, job, "" + jobNumber);
		} catch (WebServiceException x) {
			log.error(x);
		}
	}

	public void viewCode(String language, AnalysisJob job, String baseName) {
		try {
			String code = CodeGeneratorUtil.getCode(language, job);
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

			OutputStream os = response.getOutputStream();
			PrintWriter pw = new PrintWriter(os);
			pw.println(code);
			pw.flush();
			os.close();

			UIBeanHelper.getFacesContext().responseComplete();
		} catch (Exception e) {
			log.error(e);
		}
	}

	public void terminateJob(ActionEvent event) {
		try {
			int jobNumber = Integer.parseInt(UIBeanHelper.decode(UIBeanHelper
					.getRequest().getParameter("jobNumber")));
			LocalAnalysisClient ac = new LocalAnalysisClient(UIBeanHelper
					.getUserId());
			ac.terminateJob(jobNumber);
		} catch (WebServiceException e) {
			log.error(e);
		} catch (NumberFormatException e) {
			log.error(e);
		}
	}

	/**
	 * Represents a job result. Wraps JobInfo and adds methods for getting the
	 * output files and the expansion state of the associated UI panel
	 */
	public class JobResultsWrapper {

		private JobInfo jobInfo;

		private List<OutputFileInfo> outputFiles;

		private boolean selected = false;

		private List<JobResultsWrapper> childJobs;

		private int level = 0;

		private int sequence = 0;

		public JobResultsWrapper(JobInfo jobInfo,
				Map<String, Collection<TaskInfo>> kindToModules,
				Set<String> selectedFiles, Set<String> selectedJobs) {
			this(jobInfo, kindToModules, selectedFiles, selectedJobs, 0, 0);
		}

		public JobResultsWrapper(JobInfo jobInfo,
				Map<String, Collection<TaskInfo>> kindToModules,
				Set<String> selectedFiles, Set<String> selectedJobs, int level,
				int sequence) {

			this.jobInfo = jobInfo;
			this.selected = selectedJobs.contains(String.valueOf(jobInfo
					.getJobNumber()));
			this.level = level;
			this.sequence = sequence;

			// Build the list of output files from the parameter ino array.
			
			outputFiles = new ArrayList<OutputFileInfo>();
			ParameterInfo[] parameterInfoArray = jobInfo
					.getParameterInfoArray();
			if (parameterInfoArray != null) {
				File outputDir = new File(GenePatternAnalysisTask.getJobDir(""
						+ jobInfo.getJobNumber()));
				for (int i = 0; i < parameterInfoArray.length; i++) {
					if (parameterInfoArray[i].isOutputFile()) {
						if (showExecutionLogs
								|| !parameterInfoArray[i].getName().equals(
										"gp_task_execution_log.txt")) {
							File file = new File(outputDir,
									parameterInfoArray[i].getName());
							Collection<TaskInfo> modules = kindToModules
									.get(SemanticUtil.getKind(file));
							OutputFileInfo pInfo = new OutputFileInfo(
									parameterInfoArray[i], file, modules);

							pInfo.setSelected(selectedFiles.contains(pInfo
									.getValue()));
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
				JobInfo[] children = analysisClient.getChildren(jobInfo
						.getJobNumber());
				int seq = 1;
				int childLevel = getLevel() + 1;
				for (JobInfo child : children) {
					childJobs.add(new JobResultsWrapper(child, kindToModules,
							selectedFiles, selectedJobs, childLevel, seq));
					seq++;
				}
			} catch (WebServiceException e) {
				log.error("Error getting child jobs", e);

			}
		}

		public List<JobResultsWrapper> getChildJobs() {
			return childJobs;
		}

		/**
		 * Returns a list all descendant jobs, basically a the flattened tree.
		 * 
		 * @return
		 */
		public List<JobResultsWrapper> getDescendantJobs() {
			List<JobResultsWrapper> descendantJobs = new ArrayList<JobResultsWrapper>();
			descendantJobs.addAll(childJobs);
			for (JobResultsWrapper childJob : childJobs) {
				descendantJobs.addAll(childJob.getDescendantJobs());
			}
			return descendantJobs;
		}

		public void setSelected(boolean bool) {
			this.selected = bool;
		}

		public boolean isSelected() {
			return selected;
		}

		/**
		 * This property supports saving of the "expanded" state of the job across 
		 * requests.  It is used to initialize display properties of rows associated
		 * with this job.
		 * 
		 * @return
		 */
		public boolean isExpanded() {
			String parameterName = "expansion_state_" + jobInfo.getJobNumber();
			String value = UIBeanHelper.getRequest()
					.getParameter(parameterName);
			return (value == null || value.equals("true"));
		}

		/**
		 * boolean property used to conditionally render or enable some menu items.
		 * 
		 * @return
		 */
		public boolean isComplete() {
			String status = jobInfo.getStatus();
			return status.equalsIgnoreCase("Finished")
					|| status.equalsIgnoreCase("Error");
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

		public List<OutputFileInfo> getOutputFileParameterInfos() {
			return outputFiles;
		}

		public List<OutputFileInfo> getAllFileInfos() {
			List<OutputFileInfo> allFiles = new ArrayList<OutputFileInfo>();
			allFiles.addAll(outputFiles);
			for (JobResultsWrapper child : childJobs) {
				allFiles.addAll(child.getAllFileInfos());
			}
			return allFiles;
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

		public int getLevel() {
			return level;
		}

		public void setLevel(int level) {
			this.level = level;
		}

		public int getSequence() {
			return sequence;
		}

		public void setSequence(int sequence) {
			this.sequence = sequence;
		}

	}

	private static class KeyValueComparator implements Comparator<KeyValuePair> {

		public int compare(KeyValuePair o1, KeyValuePair o2) {
			return o1.getKey().compareToIgnoreCase(o2.getKey());
		}

	}

	public static class OutputFileInfo {
		static NumberFormat numberFormat;

		ParameterInfo p;

		long size;

		Date lastModified;

		boolean exists;

		boolean selected = false;

		List<KeyValuePair> moduleMenuItems = new ArrayList<KeyValuePair>();

		private static final Comparator COMPARATOR = new KeyValueComparator();

		static {
			numberFormat = NumberFormat.getInstance();
			numberFormat.setMaximumFractionDigits(1);
		}

		public OutputFileInfo(ParameterInfo p, File file,
				Collection<TaskInfo> modules) {
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
		}

		public List<KeyValuePair> getModuleMenuItems() {
			return moduleMenuItems;
		}

		public void setSelected(boolean bool) {
			this.selected = bool;
		}

		public boolean isSelected() {
			return selected;
		}

		public long getSize() {
			return size;
		}

		public String getFormattedSize() {
			if (size >= 1073741824) {
				double gigabytes = size / 1073741824.0;
				return numberFormat.format(gigabytes) + " GB";
			} else if (size >= 1048576) {
				double megabytes = size / 1048576.0;
				return numberFormat.format(megabytes) + " MB";
			} else {
				return Math.max(1, Math.ceil(size / 1024.0)) + " KB";
			}
		}

		public Date getLastModified() {
			return lastModified;
		}

		public String getDescription() {
			return p.getDescription();
		}

		public String getLabel() {
			return p.getLabel();
		}

		public String getName() {
			return UIBeanHelper.encode(p.getName());
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

		public String toString() {
			return p.toString();
		}
	}

}