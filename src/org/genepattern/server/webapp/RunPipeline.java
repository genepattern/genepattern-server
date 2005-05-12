package org.genepattern.server.webapp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.IOException;
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

import org.xml.sax.InputSource;

import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.AdminProxy;
import org.genepattern.webservice.AnalysisJob;
import org.genepattern.webservice.AnalysisService;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.JobStatus;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.AnalysisWebServiceProxy;
import org.genepattern.webservice.TaskInfo;

public class RunPipeline {

	public static final String STDOUT = GPConstants.STDOUT;

	public static final String STDERR = GPConstants.STDERR;

   RunPipelineOutputDecoratorIF decorator;
   
   PipelineModel model;
   
   /** server to run the pipeline on */
   String server;
   
   /** job id for the pipeline */
   int jobId;

   AnalysisWebServiceProxy analysisProxy;
   
   AdminProxy adminProxy;

	public RunPipeline(String server, String userID, int jobId, PipelineModel model, RunPipelineOutputDecoratorIF decorator) throws Exception {
      this.analysisProxy = new AnalysisWebServiceProxy(server, userID);
      this.adminProxy = new AdminProxy(server, userID);
      this.server = server;
	  System.setProperty("userID", userID);
      this.jobId = jobId;
      this.model = model;
      this.decorator = decorator==null?new RunPipelineBasicDecorator():decorator;  
	} 

	/**
	 * expects minimum of two args. pipeline name, username, args to pipeline
    * Additionally the system properties
    * jobID, LSID, genepattern.properties are required, while the decorator 
    * system property is optional
	 */
	public static void main(String args[]) throws Exception {
		Properties additionalArguments = new Properties();
		if (args.length < 2) {
			System.out.println("usage: RunPipeline pipelineFile username args");
         System.out.println(java.util.Arrays.asList(args));
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
				additionalArguments.put(key, valbuff.toString());

			}
		}
	
		String pipelineFileName = args[0];
		String userId = args[1];
 
      String pipelineLSID = System.getProperty(GPConstants.LSID);
      int jobId = -1;
      if (System.getProperty("jobID") == null) { // null when run using java client
         File dir = new File(System.getProperty("user.dir"));
         jobId = Integer.parseInt(dir.getName());
         System.setProperty("jobID", "" + jobId);
      } else {
         jobId = Integer.parseInt(System.getProperty("jobID"));
      }
      RunPipelineOutputDecoratorIF decorator = null;
      if (System.getProperty("decorator") != null) {
				String decoratorClass = System.getProperty("decorator");
            decorator = (RunPipelineOutputDecoratorIF) (Class
						.forName(decoratorClass)).newInstance();
      }
      String genePatternPropertiesFile = System.getProperty("genepattern.properties") +  java.io.File.separator + "genepattern.properties";
      java.io.FileInputStream fis = new java.io.FileInputStream(genePatternPropertiesFile);
      
      Properties genepatternProps = new Properties();
      genepatternProps.load(fis);
      fis.close();
	  String server = "http://"
					+ InetAddress.getLocalHost().getCanonicalHostName() + ":"
					+ genepatternProps.getProperty("GENEPATTERN_PORT");
      PipelineModel pipelineModel = getPipelineModel(pipelineFileName, pipelineLSID);
      RunPipeline rp = new RunPipeline(server, userId, jobId, pipelineModel, decorator);
		rp.runPipeline(additionalArguments);
	}

	/**
	 * the pipelineFileName may be either a local file or a URL. Figure out
	 * which it is and get it either way
	 */
	private static PipelineModel getPipelineModel(String pipelineFileName, String lsid)
			throws Exception {
		File file = new File(pipelineFileName);
		BufferedReader reader = null;
      PipelineModel model = null;
      try {
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
         model = PipelineModel.toPipelineModel(new InputSource(reader), false);
         model.setLsid(lsid);
         return model;
      } finally {
         if(reader!=null) {
            reader.close();  
         }
      }
		
	}
   
   
   /**
   * @param args maps parameter name to parameter value
   */
	public void runPipeline(Map args)
			throws Exception {

		setStatus(JobStatus.PROCESSING);

		Vector vTasks = model.getTasks();
		JobSubmission jobSubmission = null;
		TaskInfo taskInfo = null;
		ParameterInfo[] parameterInfo = null;
		int taskNum = 0;
		boolean okayToRun = true;
		for (Enumeration eTasks = vTasks.elements(); eTasks.hasMoreElements(); taskNum++) {
			jobSubmission = (JobSubmission) eTasks.nextElement();
			taskInfo = adminProxy.getTask(jobSubmission.getLSID());
			if (taskInfo == null) {
				okayToRun = false;
				System.err.println("No such task " + jobSubmission.getName() + " (" + jobSubmission.getLSID() + ")");
				decorator.error(model, "No such task " + jobSubmission.getName() + " (" + jobSubmission.getLSID() + ")");
			}

		}
		if (!okayToRun) {
			setStatus(JobStatus.ERROR);
			return;
		}

		taskNum = 0;
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
		setStatus(JobStatus.FINISHED);
	}


	/**
	 * Notify the server of the pipeline's status (Process, Finished, etc)
	 */
	protected void setStatus(String status) throws Exception {
	      analysisProxy.setJobStatus(jobId, status);
	}

   protected JobInfo executeVisualizer(AnalysisService svc, ParameterInfo[] params) {
      return new JobInfo(); // note: visualizers not checked to see if
								  // found on server
   }
   
	/**
	 * submit the job and wait for it to complete
	 */
	protected JobInfo executeTask(JobSubmission jobSubmission,
			ParameterInfo[] params, int taskNum, JobInfo[] results)
			throws Exception {

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
         return new JobInfo();
		}

		AnalysisService svc = new AnalysisService(server, task);
      if(jobSubmission.isVisualizer()){
         return executeVisualizer(svc, params);        
      }
		AnalysisJob job = submitJob(svc, params);
		JobInfo jobInfo = waitForErrorOrCompletion(job);
		return jobInfo;
	}

	protected ParameterInfo[] setInheritedJobParameters(
			ParameterInfo[] parameterInfo, JobInfo[] results) throws FileNotFoundException {
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

	protected String getInheritedFilename(Map attributes,
			JobInfo[] results) throws FileNotFoundException {
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
		String url = server + "/gp/retrieveResults.jsp?job=" + job.getJobNumber()
				+ "&filename=" + fileName;
		return url;
	}

	/**
	 * Look for parameters that are passed in on the command line and put them
	 * into the ParameterInfo array
	 */
	protected ParameterInfo[] setJobParametersFromArgs(String name,
			int taskNum, ParameterInfo[] parameterInfo, JobInfo[] results,
			Map args) {
		for (int i = 0; i < parameterInfo.length; i++) {
			ParameterInfo aParam = parameterInfo[i];
			if (aParam.getAttributes() != null) {
				if (aParam.getAttributes().get(PipelineModel.RUNTIME_PARAM) != null) {
					aParam.getAttributes().remove(PipelineModel.RUNTIME_PARAM);
					String key = name + taskNum + "." + aParam.getName();
					String val = (String) args.get(key);
					aParam.setValue(val);
				}
			}
		}
		return parameterInfo;

	}

	/**
	 * return the file name for the previously run job by index or name
	 */
	public static String getOutputFileName(org.genepattern.webservice.JobInfo job, String fileStr) throws FileNotFoundException {
		String fileName = null;
		String fn = null;
		int j;
		ParameterInfo[] jobParams = job.getParameterInfoArray();
		String jobDir = System.getProperty("jobs");
		// try semantic match on output files first

		// For now, just match on filename extension
semantic_search_loop:
		for (j = 0; j < jobParams.length; j++) {
			if (jobParams[j].isOutputFile()) {
				fn = jobParams[j].getValue(); // get the filename
				if (isFileType(new File(jobDir, fn), fileStr)) {
					fileName = fn;
					break semantic_search_loop;
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
		if (fileName == null) {
/*
			System.err.println("output files from job " + job.getJobNumber() + ":");
			for (j = 0; j < jobParams.length; j++) {
				if (jobParams[j].isOutputFile()) {
					fn = jobParams[j].getValue(); // get the filename
					File f = new File(jobDir, fn);
					try {
						System.err.println(f.getCanonicalPath() + " is a " + getFileType(f));
					} catch (IOException ioe) {
						ioe.printStackTrace();
					}
				}
			}
*/
			throw new FileNotFoundException("Unable to find output file from job " + job.getJobNumber() + " that matches " + fileStr);
		}
		return fileName;
	}

	// TODO: Nada's file analyzer gets integrated here.
	public static boolean isFileType(File file, String fileFormat) {
		String fileType = getFileType(file);
		return fileType.equalsIgnoreCase(fileFormat);
	}

	public static String getFileType(File file) {
		//ODF
		if (file.getName().toLowerCase().endsWith("." + GPConstants.ODF.toLowerCase())) {	
			return ODFModelType(file);
		} else {
			String filename = file.getName();
			return filename.substring(filename.lastIndexOf(".")+1);
		}
	}

	public static String ODFModelType(File file)
	{
		String model="";
		BufferedReader inputB;
		try {
			if (!file.exists()) {
				System.err.println("Can't find " + file.getCanonicalPath());
			}
			//System.out.println(file.getCanonicalPath());
			inputB = new BufferedReader(new FileReader(file));
			String modelLine=inputB.readLine();
			while(modelLine != null && !modelLine.startsWith("Model")){
				modelLine = inputB.readLine();
			}
			inputB.close();
			if (modelLine!=null){
				model = modelLine.substring(modelLine.indexOf("=")+1).trim();
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		return model;
	}

	/**
	 * submit a job based on a service and its parameters
	 */
	protected AnalysisJob submitJob(AnalysisService svc, ParameterInfo[] parmInfos) throws Exception {
	      if(parmInfos!=null) {
	         for(int i = 0; i < parmInfos.length; i++) {
	            if(parmInfos[i].isInputFile()) {
			String file = parmInfos[i].getValue(); // bug 724
			String val = file;
			if (!(file.startsWith("http:") || file.startsWith("ftp:") || file.startsWith("file:"))) {
				val = new File(file).toURI().toString();
			}
			parmInfos[i].setValue(val);
			parmInfos[i].getAttributes().remove("TYPE");
			parmInfos[i].getAttributes().remove("MODE");
	            }
	         }
	      }
		TaskInfo tinfo = svc.getTaskInfo();
		final JobInfo job = analysisProxy.submitJob(tinfo.getID(), parmInfos, jobId);
		final AnalysisJob aJob = new AnalysisJob(svc.getServer(), job);
		return aJob;
	}

	/**
	 * Wait for a job to end or error. This loop will wait for a max of 36
	 * seconds for 10 tries doubling the wait time each time after 6 seconds to
	 * a max of a 16 seconds wait
	 */
	protected JobInfo waitForErrorOrCompletion(AnalysisJob job)
			throws Exception {
		int maxtries = 100; // used only to increment sleep, not a hard limit
		int sleep = 1000;
		return waitForErrorOrCompletion(job, maxtries, sleep);
	}

	protected JobInfo waitForErrorOrCompletion(AnalysisJob job,
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

	protected static void dumpParameters(ParameterInfo[] params, String where) {
		System.out.println("");
		System.out.println(where);
		for (int i = 0; params != null && i < params.length; i++) {
			System.out.println("RunPipeline.executeTask: "
					+ params[i].getName() + "=" + params[i].toString());
		}
	}

}

