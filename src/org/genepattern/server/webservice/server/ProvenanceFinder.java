package org.genepattern.server.webservice.server;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.net.InetAddress;
import java.util.*; 		 

import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.data.pipeline.JobSubmission;

import org.genepattern.util.GPConstants;
import org.genepattern.webservice.ParameterFormatConverter;
		 
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

public class ProvenanceFinder {
	public static String serverURL = null;
	public String userID = null;
	LocalAnalysisClient service = null;
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


//
// input uploaded file 	http://cp21e-789.broad.mit.edu:8080/gp/getFile.jsp?task=&file=ConvertLineEndings_ted_1120817843576%2FDCM_VCDEApproval.txt
//
// output file url 	http://cp21e-789.broad.mit.edu:8080/gp/retrieveResults.jsp?job=0&filename=DCM_VCDEApproval.cvt.txt
//
/**
* Given a file URL find the Job that created it or return null.  Must be a job output file
*/
	public JobInfo findJobThatCreatedFile(String fileURL){

		String jobNoStr = getJobNoFromURL(fileURL);
		int jobid = -1;
		try {
			jobid = Integer.parseInt(jobNoStr);	
			return service.getJob(jobid);

		} catch (Exception e){
			return null;
		}
	}

	public String getJobNoFromURL(String fileURL){
		return getParamFromURL(fileURL, "job");
	}

	public String getParamFromURL(String fileURL, String key){
		// if it is null or not a local file we can do nothing
	
		if (fileURL == null) return null;
		if (!(fileURL.toUpperCase().startsWith(serverURL))) return null;
		
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

	public ArrayList getLocalInputFiles(JobInfo job){
		ArrayList inputFiles = new ArrayList(); 
		ParameterInfo[] params = job.getParameterInfoArray();

		for (int i=0; i < params.length; i++){
			String pvalue = params[i].getValue();
			
			if (pvalue.toUpperCase().startsWith(serverURL)) inputFiles.add(pvalue);

		}
 		return inputFiles;
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
			JobInfo job = findJobThatCreatedFile(aFile);
			if (job != null) jobs.add(job);
			files.addAll(getLocalInputFiles(job));	
			files.remove(0);
		}
		return jobs;
	}


	/**
	 * Given an reverse ordered set of jobs (ordered by decreasing Job #) create a pipeline model that represents it with
	 * the appropriate file inheritence representing the original jobs
	 */
	public PipelineModel createPipelineModel(TreeSet jobs, String pipelineName) throws OmnigeneException, WebServiceException {

		// create an array list with the taskinfos at their taskid location for
		// easier retrieval later
		Map taskCatalog = new LocalAdminClient(userID).getTaskCatalogByLSID();
		HashMap taskList = new HashMap();
		HashMap jobOrder = new HashMap ();		

		for (Iterator iter = taskCatalog.values().iterator(); iter.hasNext(); ){
			TaskInfo ti = (TaskInfo)iter.next();
			taskList.put(new Integer(ti.getID()), ti);
		}
		
		String taskLSID = "";
		
		PipelineModel model = new PipelineModel();
		model.setName(pipelineName); //XXX
		model.setDescription("describe it here");//XXX
		model.setAuthor(userID);
		model.setUserID(userID);
		model.setLsid(taskLSID ); // temp pipeline
		model.setVersion("0");
		model.setPrivacy(GPConstants.PRIVATE);
		int i=1;
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
	
	/**
	 * realing input files from the old job params to the new pipeline. Look for input files matching the request pattern
	 * like we did to find these jobs and replace with gpUseResult() calls. Create a new ParameterInfo array to return.
	 */	
	public ParameterInfo[] createPipelineParams(ParameterInfo[] oldJobParams, ParameterInfo[] taskParams, HashMap jobOrder) throws WebServiceException{
		ParameterInfo[] newParams = new ParameterInfo[taskParams.length]; 
		
		for (int i=0; i < taskParams.length; i++){
			ParameterInfo taskParam = taskParams[i]; 
			ParameterInfo oldJobParam = null;
			for (int j=0; j < oldJobParams.length; j++){
				oldJobParam = oldJobParams[j];
				if (oldJobParam.getName().equalsIgnoreCase(taskParam.getName())) break;
			}
			HashMap attrs = oldJobParam.getAttributes();
			
			String value=null;
			String jobNoStr = getJobNoFromURL( oldJobParam.getValue());
			if (jobNoStr == null){ 	
				value = oldJobParam.getValue();				
			} else {
				// figure out the jobs order in the new pipeline and use gpUseResult
				Integer jobNo = new Integer(jobNoStr );
				Integer pipeNo = (Integer)jobOrder.get(jobNo);
				attrs.put("inheritTaskName",""+pipeNo);
								JobInfo priorJob = service.getJob(jobNo.intValue());
				String name = getParamFromURL(oldJobParam.getValue() ,"filename");

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
							attrs.put("inheritFilename", ""+fileIdx);
						}
					}
				}
				

				// now we figure out which file to use from the job				
				value = "gpUseResult("+pipeNo+", '"+fileIdx+"')";
			}
			newParams[i] = new ParameterInfo(taskParam.getName(), value, taskParam.getDescription() );
			newParams[i].setAttributes(attrs);

		}
		return newParams;
	}

	

}