package org.genepattern.server.webapp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.util.GPConstants;
import org.genepattern.util.PropertyFactory;
import org.genepattern.webservice.AdminProxy;
import org.genepattern.webservice.AnalysisJob;
import org.genepattern.webservice.AnalysisService;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.JobStatus;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.AnalysisWebServiceProxy;
import org.genepattern.webservice.TaskInfo;

public class RunPipeline {
	public static final String SERIALIZED_MODEL = "serializedModel";

	public static final String STDOUT = "stdout";

	public static final String STDERR = "stderr";

	static String GP_URL = "";

	static RunPipelineOutputDecoratorIF decorator = new RunPipelineBasicDecorator();

	static AnalysisWebServiceProxy analysisProxy;

	static String analysisServiceURL;

	static String analysisServiceSiteName;

	static AdminProxy adminProxy;

	private RunPipeline() {
	} // prevent instantiation

	/**
	 * expects minimum of two args. pipeline name, username, args to pipeline
	 */
	public static void main(String args[]) throws Exception {
		Properties additionalArguments = new Properties();

		if (args.length < 2) {
			System.out.println("usage: RunPipeline pipelineFile username args");
			System.exit(1);
		}
		if (args.length > 2) {
			for (int i = 2; i < args.length; i++) {
				// assume args are in the form name=value
				String arg = args[i];

				StringTokenizer strtok = new StringTokenizer(arg, "=");
				String key = strtok.nextToken();
				StringBuffer valbuff = new StringBuffer("");
				int count = 0;
				while (strtok.hasMoreTokens()) {
					valbuff.append(strtok.nextToken());
					if ((strtok.hasMoreTokens()))
						valbuff.append("=");
					count++;
				}
				//String value = strtok.nextToken();
				additionalArguments.put(key, valbuff.toString());

			}
		}
		//System.out.println(""+additionalArguments);

		String pipelineFileName = args[0];
		String userID = args[1];

		setUp(userID);

		runPipelineModel(getPipelineModel(pipelineFileName),
				additionalArguments);

	}

	/**
	 * the pipelineFileName may be either a local file or a URL. Figure out
	 * which it is and get it either way
	 */
	public static PipelineModel getPipelineModel(String pipelineFileName)
			throws Exception {
		File file = new File(pipelineFileName);
		BufferedReader reader = null;

		if (!file.exists()) {
			// must be a URL, try to retrieve it
			URL url = new URL(pipelineFileName);
			HttpURLConnection uconn = (HttpURLConnection) url.openConnection();
			int rc = uconn.getResponseCode();
			reader = new BufferedReader(new InputStreamReader(uconn
					.getInputStream()));
		} else {
			reader = new BufferedReader(new FileReader(pipelineFileName));
			file.deleteOnExit();
		}

		StringBuffer xml = new StringBuffer();
		String line = null;
		while ((line = reader.readLine()) != null) {
			xml.append(line);
			xml.append("\n");
		}
		File pipelineXML = new File("pipeline.xml");
		BufferedWriter writer = new BufferedWriter(new FileWriter(pipelineXML));
		writer.write(xml.toString());
		writer.flush();
		writer.close();
		pipelineXML.deleteOnExit();

		PipelineModel model = PipelineModel.toPipelineModel(xml.toString());
		model.setLsid(System.getProperty(GPConstants.LSID));
		return model;
	}

	protected static void runPipelineModel(PipelineModel model, Properties args)
			throws Exception {

		setStatus(JobStatus.JOB_PROCESSING);

		Vector vTasks = model.getTasks();
		JobSubmission jobSubmission = null;
		TaskInfo taskInfo = null;
		ParameterInfo[] parameterInfo = null;
		int taskNum = 0;
		JobInfo results[] = new JobInfo[vTasks.size()];
		decorator.beforePipelineRuns(model);
		try {
			for (Enumeration eTasks = vTasks.elements(); eTasks
					.hasMoreElements(); taskNum++) {
				jobSubmission = (JobSubmission) eTasks.nextElement();
				try {

					parameterInfo = jobSubmission.giveParameterInfoArray();

					ParameterInfo[] params = setInheritedJobParameters(
							parameterInfo, results);

					params = setJobParametersFromArgs(jobSubmission.getName(),
							taskNum + 1, params, results, args);

					decorator.recordTaskExecution(jobSubmission, taskNum + 1,
							vTasks.size());

					JobInfo taskResult = executeTask(jobSubmission, params,
							taskNum, results);

					notifyPipelineOfOutputFiles(taskResult, jobSubmission
							.getName(), taskNum);

					decorator.recordTaskCompletion(taskResult, jobSubmission
							.getName()
							+ (taskNum + 1));
					results[taskNum] = taskResult;

				} catch (Exception e) {
					System.err.println("execution for "
							+ jobSubmission.getName() + " task failed:");
					System.err.println(e.getMessage());
					e.printStackTrace();
					System.err.println("");
					throw e;
				}
			}
		} finally {
			decorator.afterPipelineRan(model);
		}
		setStatus(JobStatus.JOB_FINISHED);
	}

	/**
	 * notify the server that the output files of the jobs in the pipeline
	 * belong to the pipeline job. stdout is always the next to last output
	 * file, and stderr is the very last (when they exist)
	 */
	protected static void notifyPipelineOfOutputFiles(JobInfo jobInfo,
			String taskName, int taskNum) throws Exception {

		ParameterInfo[] jobParams = jobInfo.getParameterInfoArray();
		URL stdout = null;
		URL stderr = null;
		for (int j = 0; j < jobParams.length; j++) {
			String fullFileName = "";
			if (jobParams[j].isOutputFile()) {
				String fileName = jobParams[j].getValue();
				int lastIdx = fileName.lastIndexOf(File.separator);
				if (lastIdx != -1)
					fileName = fileName.substring(lastIdx + 1);
				lastIdx = fileName.lastIndexOf("/");
				if (lastIdx != -1)
					fileName = fileName.substring(lastIdx + 1);
				fullFileName = jobInfo.getJobNumber() + File.separator
						+ fileName;

				String updateUrl = GP_URL + "updatePipelineStatus.jsp?jobID="
						+ System.getProperty("jobID") + "&" + GPConstants.NAME
						+ "=" + taskName + (taskNum + 1);
				updateUrl += "&filename=" + fullFileName;
				URL url = new URL(updateUrl);
				if (fileName.equals(STDOUT)) {
					stdout = url;
					continue;
				}
				if (fileName.equals(STDERR)) {
					stderr = url;
					continue;
				}
				HttpURLConnection uconn = (HttpURLConnection) url
						.openConnection();
				int rc = uconn.getResponseCode();
			}

		}
		if (stdout != null) {
			HttpURLConnection uconn = (HttpURLConnection) stdout
					.openConnection();
			int rc = uconn.getResponseCode();
		}
		if (stderr != null) {
			HttpURLConnection uconn = (HttpURLConnection) stderr
					.openConnection();
			int rc = uconn.getResponseCode();
		}
	}

	/**
	 * Notify the server of the pipeline's status (Process, Finished, etc)
	 */
	protected static void setStatus(int status) throws Exception {
		String statusUrl = GP_URL + "updatePipelineStatus.jsp?jobID="
				+ System.getProperty("jobID") + "&jobStatus=" + status;
		//System.out.println("URL=" + statusUrl);
		URL url = new URL(statusUrl);
		HttpURLConnection uconn = (HttpURLConnection) url.openConnection();
		int rc = uconn.getResponseCode();
		//System.out.println("Called the URL " + rc);

	}

	/**
	 * submit the job and wait for it to complete
	 */
	protected static JobInfo executeTask(JobSubmission jobSubmission,
			ParameterInfo[] params, int taskNum, JobInfo[] results)
			throws Exception {

		if (jobSubmission.isVisualizer())
			return new JobInfo(); // note: visualizers not checked to see if
								  // found on server
		String lsidOrTaskName = jobSubmission.getLSID();
		if (lsidOrTaskName == null || lsidOrTaskName.equals("")) {
			lsidOrTaskName = jobSubmission.getName();
		}
		TaskInfo task = adminProxy.getTask(lsidOrTaskName);

		if (task == null) {
			System.err.println("Task " + lsidOrTaskName + " not found."); // write
										  // to
										  // stderr
										  // file
			System.exit(0);
		}

		AnalysisService svc = new AnalysisService(analysisServiceSiteName, task);
		AnalysisJob job = submitJob(svc, params);
		JobInfo jobInfo = waitForErrorOrCompletion(job);
		return jobInfo;
	}

	public static ParameterInfo[] setInheritedJobParameters(
			ParameterInfo[] parameterInfo, JobInfo[] results) {
		for (int i = 0; i < parameterInfo.length; i++) {
			ParameterInfo aParam = parameterInfo[i];
			if (aParam.getAttributes() != null) {
				if (aParam.getAttributes().get(PipelineModel.INHERIT_TASKNAME) != null) {
					String url = getInheritedFilename(aParam.getAttributes(),
							results);
					aParam.setValue(url);
				}
			}
		}
		return parameterInfo;
	}

	protected static String getInheritedFilename(Map attributes,
			JobInfo[] results) {
		// these params must be removed so that the soap lib doesn't try to send
		// the
		// file as ana attachment
		String taskStr = (String) attributes
				.get(PipelineModel.INHERIT_TASKNAME);
		String fileStr = (String) attributes
				.get(PipelineModel.INHERIT_FILENAME);
		attributes.remove("TYPE");
		attributes.put(ParameterInfo.MODE, ParameterInfo.URL_INPUT_MODE);

		int task = Integer.parseInt(taskStr);
		JobInfo job = results[task];
		String fileName = getOutputFileName(job, fileStr);
		try {
			fileName = URLEncoder.encode(fileName, "UTF-8");
		} catch (UnsupportedEncodingException uee) {
			// ignore
		}
		String url = GP_URL + "retrieveResults.jsp?job=" + job.getJobNumber()
				+ "&filename=" + fileName;
		return url;
	}

	/**
	 * Look for parameters that are passed in on the command line and put them
	 * into the ParameterInfo array
	 */
	public static ParameterInfo[] setJobParametersFromArgs(String name,
			int taskNum, ParameterInfo[] parameterInfo, JobInfo[] results,
			Properties args) {
		for (int i = 0; i < parameterInfo.length; i++) {
			ParameterInfo aParam = parameterInfo[i];
			if (aParam.getAttributes() != null) {
				if (aParam.getAttributes().get(PipelineModel.RUNTIME_PARAM) != null) {
					aParam.getAttributes().remove(PipelineModel.RUNTIME_PARAM);
					String key = name + taskNum + "." + aParam.getName();
					String val = (String) args.get(key);
					//System.out.println("looking for " + key + " found " + val
					// + " in " + args);
					aParam.setValue(val);
				}
			}
		}
		return parameterInfo;

	}

	/**
	 * return the file name for the previously run job by index or name
	 */
	public static String getOutputFileName(org.genepattern.webservice.JobInfo job, String fileStr) {
		String fileName = null;
		String fn = null;
		int j;
		ParameterInfo[] jobParams = job.getParameterInfoArray();

		// try semantic match on output files first

		// TODO: Nada's file analyzer gets integrated here.
		// For now, just match on filename extension
		for (j = 0; j < jobParams.length; j++) {
			if (jobParams[j].isOutputFile()) {
				fn = jobParams[j].getValue(); // get the filename
				
				if (fn.endsWith("." + fileStr)) {
					// if there's match with the extension, then we've found it (for now)
					fileName = fn;
					break;
				}
			}
		}

		if (fileName == null) {
			// no match on extension, try assuming that it is an integer number (1..5)
			try {
				int fileIdx = Integer.parseInt(fileStr);
				// success, find the nth output file
				int jobFileIdx = 1;
				for (j = 0; j < jobParams.length; j++) {
					if (jobParams[j].isOutputFile()) {
						if (jobFileIdx == fileIdx) {
							fileName = jobParams[j].getValue();
							break;
						}
						jobFileIdx++;
					}
				}
			} catch (NumberFormatException nfe) {
				// not an extension, not a number, look for stdout or stderr
				
				// fileStr is stderr or stdout instead of an index
				if (fileStr.equals(STDOUT) || fileStr.equals(STDERR)) {
					fileName = fileStr;
				}
			}
		}

		if (fileName != null) {
			int lastIdx = fileName.lastIndexOf(File.separator);
			if (lastIdx != -1) {
				fileName = fileName.substring(lastIdx + 1);
			}
			lastIdx = fileName.lastIndexOf("/");
			if (lastIdx != -1) {
				fileName = fileName.substring(lastIdx + 1);
			}
		}
		return fileName;
	}

	/**
	 * submit a job based on a service and its parameters
	 */
	public static AnalysisJob submitJob(AnalysisService svc,
			ParameterInfo[] parmInfos) throws Exception {
		TaskInfo tinfo = svc.getTaskInfo();
		final JobInfo job = analysisProxy.submitJob(tinfo.getID(), parmInfos);
		final AnalysisJob aJob = new AnalysisJob(svc.getServer(), tinfo
				.getName(), job);
		return aJob;
	}

	/**
	 * Wait for a job to end or error. This loop will wait for a max of 36
	 * seconds for 10 tries doubling the wait time each time after 6 seconds to
	 * a max of a 16 seconds wait
	 */
	public static JobInfo waitForErrorOrCompletion(AnalysisJob job)
			throws Exception {
		int maxtries = 100; // used only to increment sleep, not a hard limit
		int sleep = 1000;
		return waitForErrorOrCompletion(job, maxtries, sleep);
	}

	public static JobInfo waitForErrorOrCompletion(AnalysisJob job,
			int maxTries, int initialSleep) throws Exception {
		String status = "";
		JobInfo info = null;
		int count = 0;
		int sleep = initialSleep;

		while (!(status.equalsIgnoreCase("ERROR") || (status
				.equalsIgnoreCase("Finished")))) {
			count++;
			Thread.currentThread().sleep(sleep);
			info = analysisProxy.checkStatus(job.getJobInfo().getJobNumber());
			status = info.getStatus();
			//  if (count > maxTries) break;
			sleep = incrementSleep(initialSleep, maxTries, count);
		}
		return info;
	}

	/**
	 * make the sleep time go up as it takes longer to exec. eg for 100 tries of
	 * 1000ms (1 sec) first 20 are 1 sec each next 20 are 2 sec each next 20 are
	 * 4 sec each next 20 are 8 sec each any beyond this are 16 sec each
	 */
	protected static int incrementSleep(int init, int maxTries, int count) {
		if (count < (maxTries * 0.2))
			return init;
		if (count < (maxTries * 0.4))
			return init * 2;
		if (count < (maxTries * 0.6))
			return init * 4;
		if (count < (maxTries * 0.8))
			return init * 8;
		return init * 16;
	}

	protected static void setUp(String userID) {
		try {
			final String separator = java.io.File.separator;
			final String password = "";

			if (System.getProperty("omnigene.conf") == null) {
				System.setProperty("omnigene.conf", new File("")
						.getAbsolutePath());
			}

			if (System.getProperty("jobID") == null) {
				File dir = new File(System.getProperty("user.dir"));
				System.setProperty("jobID", dir.getName());
			}
			if (userID != null) {
				System.setProperty("userID", userID);
			}

			// get a new output decorator if one has been specified
			if (System.getProperty("decorator") != null) {
				String decoratorClass = System.getProperty("decorator");

				decorator = (RunPipelineOutputDecoratorIF) (Class
						.forName(decoratorClass)).newInstance();
			}

			PropertyFactory property = PropertyFactory.getInstance();
			Properties genepatternProps = property
					.getProperties("genepattern.properties");
			//		GP_URL = genepatternProps.getProperty("GenePatternURL");
			GP_URL = "http://"
					+ InetAddress.getLocalHost().getCanonicalHostName() + ":"
					+ genepatternProps.getProperty("GENEPATTERN_PORT") + "/gp/";

			Properties omnigeneProperties = property
					.getProperties("omnigene.properties");
			analysisServiceURL = omnigeneProperties
					.getProperty("analysis.service.URL");
			analysisServiceSiteName = omnigeneProperties.getProperty(
					"analysis.service.site.name", "Broad Institute");
			String server = "http://"
					+ InetAddress.getLocalHost().getCanonicalHostName() + ":"
					+ genepatternProps.getProperty("GENEPATTERN_PORT");
			analysisProxy = new AnalysisWebServiceProxy(server, userID);
			adminProxy = new AdminProxy(server, userID);
			/*******************************************************************
			 * String classPath = System.getProperty("java.class.path");
			 * System.out.println("classpath was " + classPath); if
			 * (!classPath.endsWith(System.getProperty("path.separator"))) {
			 * classPath = classPath + System.getProperty("path.separator"); }
			 * classPath = classPath + genepatternProps.getProperty("webappDir") +
			 * "/WEB-INF/lib/hsqldb.jar"; System.setProperty("java.class.path",
			 * classPath); System.out.println("classpath is " +
			 * System.getProperty("java.class.path"));
			 ******************************************************************/
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected static void dumpParameters(ParameterInfo[] params, String where) {
		System.out.println("");
		System.out.println(where);
		for (int i = 0; params != null && i < params.length; i++) {
			System.out.println("RunPipeline.executeTask: "
					+ params[i].getName() + "=" + params[i].toString());
		}
	}

}

