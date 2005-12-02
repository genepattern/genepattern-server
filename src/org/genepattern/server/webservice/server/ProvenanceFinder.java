package org.genepattern.server.webservice.server;

import java.io.*;

import java.rmi.RemoteException;
import java.net.InetAddress;
import java.util.*; 		 
import java.net.URLEncoder;

import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.data.pipeline.JobSubmission;

import org.genepattern.util.GPConstants;
import org.genepattern.webservice.ParameterFormatConverter;
import org.genepattern.server.webapp.PipelineController;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
	 
import org.genepattern.webservice.FileWrapper;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.JobStatus;
import org.genepattern.webservice.OmnigeneException;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;
import org.genepattern.webservice.WebServiceException;
import org.genepattern.webservice.AnalysisJob;
import org.genepattern.server.webservice.server.local.LocalAnalysisClient;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.server.webservice.server.DirectoryManager;

public class ProvenanceFinder {
	public static String serverURL = null;
	protected String userID = null;
	protected LocalAnalysisClient service = null;
	protected ArrayList filesToCopy = new ArrayList();


	public ProvenanceFinder(String user){
		userID = user;
		service = new LocalAnalysisClient(userID);
		
		if (serverURL == null) {
			try {
				serverURL = "http://"+ InetAddress.getLocalHost().getCanonicalHostName() + ":"+ System.getProperty("GENEPATTERN_PORT");
				serverURL = serverURL.toUpperCase();

				
			} catch (Exception e){
				e.printStackTrace();
			}
		}
	}


	public String createProvenancePipeline(Set jobs, String pipelineName){
		String lsid = null;

		try {
			PipelineModel model = this.createPipelineModel((TreeSet)jobs, pipelineName);	
			PipelineController controller = new PipelineController(model);
			lsid = controller.generateTask();
			model.setLsid(lsid);
	
			copyFilesToPipelineDir(lsid);
		} catch (Exception e){
			e.printStackTrace();

		}		
		return lsid;
	}

	public String createProvenancePipeline(String filename, String pipelineName){
		String lsid = null;
		Set jobs  = this.findJobsThatCreatedFile(filename);
		lsid = createProvenancePipeline(jobs, pipelineName);
		return lsid;
	}

	public Set findJobsThatCreatedFile(String fileURL){
		ArrayList files = new ArrayList();
		Set jobs = new TreeSet(new Comparator(){
			public int compare(Object o1, Object o2){
				JobInfo j1 = (JobInfo)o1;		
				JobInfo j2 = (JobInfo)o2;		
				if (j1.getJobNumber() > j2.getJobNumber()) return 1;
				else if (j1.getJobNumber() < j2.getJobNumber()) return -1;
				else return 0;
			}
			
			public boolean equals(Object obj){
				return this == obj;
			}
		});
		
		files.add(fileURL);
		
		while (!files.isEmpty()){
			String aFile = (String)files.get(0);
			if (aFile == null) continue;
			JobInfo job = findJobThatCreatedFile(aFile);
			if (job != null) jobs.add(job);
			files.addAll(getLocalInputFiles(job));	
			files.remove(0);
		}
		return jobs;
	}


	/**
	 * Given a file URL find the Job that created it or return null.  Must be a job output file
	 */
	public JobInfo findJobThatCreatedFile(String fileURL){		
		String jobNoStr = getJobNoFromURL(fileURL);
		if (jobNoStr == null){
			try {
				// maybe just a job # passed in
				Integer.parseInt(fileURL);
				jobNoStr = fileURL;
			} catch (NumberFormatException nfe){
			}

		}
		int jobid = -1;
		try {
			jobid = Integer.parseInt(jobNoStr);	
			return service.getJob(jobid);

		} catch (Exception e){
			return null;
		}
	}



	/**
	 * Given an reverse ordered set of jobs (ordered by decreasing Job #) create a pipeline model that represents it with
	 * the appropriate file inheritence representing the original jobs
	 */
	protected PipelineModel createPipelineModel(TreeSet jobs, String pipelineName) throws OmnigeneException, WebServiceException {
		filesToCopy = new ArrayList();
		// create an array list with the taskinfos at their taskid location for
		// easier retrieval later
		Map taskCatalog = new LocalAdminClient(userID).getTaskCatalogByLSID();
		HashMap taskList = new HashMap();
		HashMap jobOrder = new HashMap ();		

		for (Iterator iter = taskCatalog.values().iterator(); iter.hasNext(); ){
			TaskInfo ti = (TaskInfo)iter.next();
			taskList.put(new Integer(ti.getID()), ti);
		}
		System.out.println("JI=" + jobs);
		String taskLSID = "";
		
		PipelineModel model = new PipelineModel();
		model.setName(pipelineName); //XXX
		model.setDescription("describe it here");//XXX
		model.setAuthor(userID);
		model.setUserID(userID);
		model.setLsid(taskLSID ); // temp pipeline
		model.setVersion("0");
		model.setPrivacy(GPConstants.PRIVATE);
		int i=0;
		for (Iterator iter = jobs.iterator(); iter.hasNext(); i++){
			JobInfo job = (JobInfo)iter.next();

			jobOrder.put(new Integer(job.getJobNumber()), new Integer(i)); // map old job number to order in pipeline

			TaskInfo mTaskInfo = (TaskInfo)taskList.get(new Integer(job.getTaskID()));			
			TaskInfoAttributes mTia = mTaskInfo.giveTaskInfoAttributes();
			boolean isVisualizer = ((String)mTia.get(GPConstants.TASK_TYPE)).equals(GPConstants.TASK_TYPE_VISUALIZER);


			ParameterInfo[] adjustedParams = createPipelineParams(job.getParameterInfoArray(), mTaskInfo.getParameterInfoArray(), jobOrder);


			boolean[] runTimePrompt = (adjustedParams!= null ? new boolean[adjustedParams.length] : null);
			if (runTimePrompt != null){
				for (int j=0; j < adjustedParams.length; j++){
					runTimePrompt[j] = false;					
				}
				
			}


			JobSubmission jobSubmission = new JobSubmission(mTaskInfo.getName(), 
				mTaskInfo.getDescription(), 
				mTia.get(GPConstants.LSID), 
				adjustedParams, 
				runTimePrompt, // runtime prompts will always be false in these generated pipelines
				isVisualizer, 
				mTaskInfo);

			model.addTask(jobSubmission);


		}
		return model;
	}

	protected void copyFilesToPipelineDir(String pipelineLSID){
		String attachmentDir = null;
		try {
			attachmentDir = DirectoryManager.getTaskLibDir(pipelineLSID);
		} catch (Exception e){
			System.out.println("Could not copy files for pipeline: " + pipelineLSID);
			e.printStackTrace();
			return;
		}

		File dir = new File(attachmentDir);
		dir.mkdir();
		byte[] buf = new byte[100000];
		int j;

		for (Iterator iter = filesToCopy.iterator(); iter.hasNext(); ){
			File aFile = (File)iter.next();
			
			try {
				FileInputStream is = new FileInputStream(aFile);
				FileOutputStream os = new FileOutputStream(new File(dir, aFile.getName()));
				while ((j = is.read(buf, 0, buf.length)) > 0) {
					os.write(buf, 0, j);
				}
				is.close();
				os.close();
			} catch (Exception e){
				System.out.println("Could not copy file " + aFile.getAbsolutePath() +" todir " + dir.getAbsolutePath());
				e.printStackTrace();
			}
		}
		filesToCopy.clear();
	}



	protected String getJobNoFromURL(String fileURL){
		return getParamFromURL(fileURL, "job");
	}

	protected String getParamFromURL(String fileURL, String key){
		// if it is null or not a local file we can do nothing
	
		if (fileURL == null) return null;
		if (!(fileURL.toUpperCase().startsWith(serverURL)) && !fileURL.startsWith("http://127.0.0.1") && !fileURL.startsWith("http://localhost")) {
			return null;
		}
		
		// if it is not a result file do nothing		
		if (!(fileURL.indexOf("retrieveResults.jsp") >= 1)) return null;
		
		// now we think we have a local result file url so grab the job # 
		int idx = fileURL.indexOf(key+"=");
		if (idx < 0) return null; // can't find a job #
		
		int endIdx = fileURL.indexOf("&", idx);
		if (endIdx == -1) endIdx = fileURL.length();

		String jobNoStr = fileURL.substring(idx+1+key.length(), endIdx);
		return jobNoStr;
	}

	protected ArrayList getLocalInputFiles(JobInfo job){
		ArrayList inputFiles = new ArrayList(); 
		if (job == null) return inputFiles;

		ParameterInfo[] params = job.getParameterInfoArray();
		if (params != null){
			for (int i=0; i < params.length; i++){
				String pvalue = params[i].getValue();
				HashMap attributes = params[i].getAttributes();

				String val = getURLFromParam(params[i]);


				if (val != null) inputFiles.add(val);
			}
		}
 		return inputFiles;
	}


	public String getURLFromParam(ParameterInfo pinfo){
		HashMap attributes = pinfo.getAttributes();
		String pvalue = pinfo.getValue();
		if (pvalue.toUpperCase().startsWith(serverURL)){
			return pvalue;
		} else if ("FILE".equals(attributes.get("TYPE"))){

			if ("CACHED_IN".equals(attributes.get("MODE")) ){
				int idx = pvalue.indexOf("/");
				String jobstr = pvalue.substring(0, idx);
				String filename = pvalue.substring(idx+1);

				return serverURL + "/gp/retrieveResults.jsp?job=" + jobstr + "&filename=" + filename;
			} else {
		return pvalue;	

			}
		}
		return null;
	}

	/**
	 * realing input files from the old job params to the new pipeline. Look for input files matching the request pattern
	 * like we did to find these jobs and replace with gpUseResult() calls. Create a new ParameterInfo array to return.
	 */	
	protected ParameterInfo[] createPipelineParams(ParameterInfo[] oldJobParams, ParameterInfo[] taskParams, HashMap jobOrder) throws WebServiceException{
		ParameterInfo[] newParams = new ParameterInfo[taskParams.length]; 
		
		for (int i=0; i < taskParams.length; i++){
			ParameterInfo taskParam = taskParams[i]; 
			ParameterInfo oldJobParam = null;
			for (int j=0; j < oldJobParams.length; j++){
				oldJobParam = oldJobParams[j];
				if (oldJobParam.getName().equalsIgnoreCase(taskParam.getName())) break;
			}
			HashMap attrs = oldJobParam.getAttributes();
			
			String value= getURLFromParam(oldJobParam);
		
			String jobNoStr = getJobNoFromURL( value );


			if (jobNoStr == null){ 	
				// for files that are on the server, replace with generic URL with LSID to be substituted at runtime			
				// for anything else leave it unmodified
				value = oldJobParam.getValue();				

				File inFile= new File(value);
				if (inFile.exists()){
					filesToCopy.add(inFile); 
					value = "<GenePatternURL>getFile.jsp?task=" + GPConstants.LEFT_DELIMITER + GPConstants.LSID + GPConstants.RIGHT_DELIMITER + "&file=" + URLEncoder.encode(inFile.getName());
				}

			} else {
				// figure out the jobs order in the new pipeline and use gpUseResult
				Integer jobNo = new Integer(jobNoStr );
				Integer pipeNo = (Integer)jobOrder.get(jobNo);
				
				attrs.put(PipelineModel.INHERIT_TASKNAME,""+pipeNo);
				System.out.println("InheritTask: " + pipeNo);

				JobInfo priorJob = service.getJob(jobNo.intValue());
				String name = getParamFromURL(value ,"filename");
				//
				// XXX use file index for now,  Change to file type when I understand how
				// to get the right information
				//
				ParameterInfo[] pjp = priorJob.getParameterInfoArray();
				int fileIdx = 0;
				for (int j = 0; j < pjp.length; j++) {
					if (pjp[j].isOutputFile()) {
						fileIdx++;
						if (pjp[j].getValue().endsWith(name)){
							attrs.put(PipelineModel.INHERIT_FILENAME, ""+fileIdx);
						}
					}
				}
				

				// now we figure out which file to use from the job				
				value = "";
			}
			newParams[i] = new ParameterInfo(taskParam.getName(), value, taskParam.getDescription() );
			newParams[i].setAttributes(attrs);

		}
		return newParams;
	}

	

}