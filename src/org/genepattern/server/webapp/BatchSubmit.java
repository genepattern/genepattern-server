package org.genepattern.server.webapp;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.RequestContext;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.servlet.ServletRequestContext;
import org.apache.log4j.Logger;
import org.genepattern.server.domain.AnalysisJobDAO;
import org.genepattern.server.domain.BatchJob;
import org.genepattern.server.domain.BatchJobDAO;
import org.genepattern.server.executor.JobSubmissionException;
import org.genepattern.server.handler.AddNewJobHandler;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.server.webservice.server.local.LocalAnalysisClient;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.WebServiceException;

public class BatchSubmit  {
	
	private static final long serialVersionUID = 4823931484381936462L;

	private static Logger log =  Logger.getLogger(BatchSubmit.class);
	private String userName;
	private Map<String, String> formValues = new HashMap<String,String>();
	private Map<String, MultiFileParameter> multiFileValues = new HashMap<String, MultiFileParameter>();
	private boolean isBatch;
	private boolean listSizesMatch = true;
	private boolean matchedFiles = true;
	private Integer id;
   List<ParameterInfo> missingParameters = new ArrayList<ParameterInfo>();
	//Collect input parameters.  
	//Find multi-file input file parameters
	//Validate that only one file parameter has multiple files
	//	Todo in the future:  Allow groups of matched files,
	//			Create resolution screen for mismatches
	//Submit a job for each file.
	public BatchSubmit(HttpServletRequest request) throws IOException, FileUploadException   {
		userName =  (String) request.getSession().getAttribute(GPConstants.USERID);
		readFormValuesAndLoadAttachedFiles(request);		
	}	
	
	public List<ParameterInfo> getMissingParameters(){
		return missingParameters;
	}
	
	public void submitJobs() throws UnsupportedEncodingException, WebServiceException {
   	isBatch = false;

		//Look up the task name, stored in a hidden field
		String taskLsid = formValues.get("taskLSID");
		if (taskLsid == null){
			//Try the task name instead
			  taskLsid = formValues.get("taskName");		       
		}
		taskLsid = (taskLsid) != null ? URLDecoder.decode(taskLsid, "UTF-8") : null;
		
		//And get all the parameters that need to be filled in for this task		
		ParameterInfo parameterInfoArray[] = null;
		TaskInfo taskInfo;
		if (taskLsid != null) {
			taskInfo = new LocalAdminClient(userName).getTask(taskLsid);
			parameterInfoArray = taskInfo.getParameterInfoArray();	        
	    }else{
	    	return;
	    }
		

   	LocalAnalysisClient analysisClient = new LocalAnalysisClient(userName);                  
       //Now try and match the parameters to the form fields we've just read     
       for (int i = 0; i < parameterInfoArray.length; i++) {
           ParameterInfo pinfo = parameterInfoArray[i];
           String value;

           value = formValues.get(pinfo.getName());            
           if (value != null){
           	pinfo.setValue(value);
           }else{
           	//Perhaps, this form value has been submitted as a url
           	value = formValues.get(pinfo.getName()+"_url");
           	if (value != null){            		     
					pinfo.getAttributes().put(ParameterInfo.MODE, ParameterInfo.URL_INPUT_MODE);
					pinfo.getAttributes().remove(ParameterInfo.TYPE);	
					pinfo.setValue(value);
           	}
           }

           //Was this value required?
           if ((value == null) || (value.trim().length() == 0)) {            
           	//Is it going to be filled in by our multi file submit process
           	if (multiFileValues.get(pinfo.getName()+"_url") == null){
           		boolean isOptional = ((String) pinfo.getAttributes()
           				.get(GPConstants.PARAM_INFO_OPTIONAL[GPConstants.PARAM_INFO_NAME_OFFSET]))
           				.length() > 0;
           				if (!isOptional) {
           					missingParameters.add(pinfo);
           				}
           	}
           }            
       }

       if (missingParameters.size() > 0){
       	log.warn("Missing required parameters");
       	return;
       }

       if (!multiFileListsAreSameSize()){
       	listSizesMatch = false;
       	return;
       }
       if (!checkForMatchedParameters()){
       	matchedFiles = false;
       	return;
       }
       //Now, submit the job if there's no multi-file field
       //Or, submit multiple jobs for each filename in the multi-file field
   	if (multiFileValues.size() == 0){
            JobInfo job = submitJob(taskInfo.getID(), parameterInfoArray);       
   		id = job.getJobNumber();    		
       }else {        	
       	BatchJob batchJob = new BatchJob(userName);

       	
       	int numFiles = multiFileValues.values().iterator().next().getNumFiles();
       	for (int i=0; i < numFiles; i++){
       		for (String parameter: multiFileValues.keySet()){
       			String parameterValue = multiFileValues.get(parameter).getFilenames().get(i).fullPath();
       			assignParameter(undecorate(parameter), parameterValue, parameterInfoArray);
       		}
                JobInfo job = submitJob(taskInfo.getID(), parameterInfoArray);                   
				batchJob.getBatchJobs().add(new AnalysisJobDAO().findById(job.getJobNumber()));	
       	}        										
			new BatchJobDAO().save(batchJob);
			
			isBatch = true;
			id = batchJob.getJobNo();
		}
    }
	
    private JobInfo submitJob(int taskID, ParameterInfo[] parameters) throws WebServiceException {
        AddNewJobHandler req = new AddNewJobHandler(taskID, userName, parameters);
        try {
            JobInfo jobInfo = req.executeRequest();
            return jobInfo;
        }
        catch (JobSubmissionException e) {
            throw new WebServiceException(e);
        }
    }
	
	//If the user uploaded multiple files for multiple parameters,
	//make sure we can match sets of files for submission to the batch process
	private boolean multiFileListsAreSameSize() {
		if (multiFileValues.size() <= 1){
			return true;
		}
		
		int listSize = multiFileValues.values().iterator().next().getNumFiles();
		for (MultiFileParameter multiFile: multiFileValues.values()){
			if (multiFile.getNumFiles() != listSize){
				return false;
			}
		}
		return true;
	}
	//If the user uploaded multiple files for multiple parameters,
	//attempt to match them up for job submissions.  Automatic matching
	//can only be done by same filename - different extension.
	private boolean checkForMatchedParameters() {		
		//Make sure the filenames only differ by extension
		if (multiFileValues.size() > 1){
			MultiFileParameter firstParameter = multiFileValues.values().iterator().next();			
			int numFiles = firstParameter.getNumFiles();
			for (int i=0; i < numFiles; i++){				
				String rootFileName = firstParameter.getFilenames().get(i).filename();
				for (String parameter: multiFileValues.keySet()){
					String filename = multiFileValues.get(parameter).getFilenames().get(i).filename();
					if (rootFileName.compareTo(filename)!= 0){
						return false;
					}
				}
			}
		}		
		return true;
	}

	//The filename fields are decorated with the appendix "_url" to show that this
	//was a textfield.  Hack off the appendix here
	private String undecorate(String urlKey) {
		return urlKey.substring(0, urlKey.length() - 4);
	}

	private void assignParameter(String key, String val, ParameterInfo[] parameterInfoArray) {
		for (int i=0; i < parameterInfoArray.length; i++){
			ParameterInfo pinfo = parameterInfoArray[i];
			if (pinfo.getName().compareTo(key)==0){
				pinfo.setValue(val);
				return;
			}
		}
		log.error("Key value " + key+ " was not found in parameter info");		
	}

	private void readFormValuesAndLoadAttachedFiles(HttpServletRequest request) throws IOException, FileUploadException{
		//Though the batch files will have been uploaded already through our upload applet and MultiFileUploadReceiver,
		//the form may still contain single attached files.  Save them now.
		
		
		
		RequestContext reqContext = new ServletRequestContext(request);
		if (FileUploadBase.isMultipartContent(reqContext)){
			FileItemFactory factory = new DiskFileItemFactory();
			ServletFileUpload upload = new ServletFileUpload(factory);
			
			List<FileItem> submittedData = upload.parseRequest(reqContext);
			Iterator<FileItem> it = submittedData.iterator();
			while (it.hasNext()){
				FileItem submission = it.next();
				if (!submission.isFormField()){
					loadAttachedFile(userName + "_run", submission);
				} else{
					readFormParameter(submission);
				}
			}					
		}else{
			throw new FileUploadException("Expecting form with encoding multipart/form-data");
		}	
	}

	private void readFormParameter(FileItem submission) {
		String formValue = submission.getString();

		MultiFileParameter multiFile = new MultiFileParameter(formValue);
		if (multiFile.getNumFiles() > 1 || formValue.endsWith(";")){
			multiFileValues.put(submission.getFieldName(), multiFile);
		}else{
			formValues.put(submission.getFieldName(), formValue);
		}
		log.debug("Storing "+submission.getFieldName()+" : "+submission.getString());
	}

	private void loadAttachedFile(String prefix, FileItem submission)
			throws IOException {
		//We expect to find an attached file.  But perhaps, this field was never filled in
		//if the user specified a URL instead.
		if (submission.getSize()> 0) {
			//use createTempFile to guarantee a unique name, but then change it to a directory
			File tempDir = File.createTempFile(prefix, null);
			tempDir.delete();
			tempDir.mkdir();
			
			File file = new File(tempDir, submission.getName());
			try {
				submission.write(file);
			} catch (Exception e) {
				throw new IOException("Could not write file");
			}
			formValues.put(submission.getFieldName(), file.getCanonicalPath());
			log.debug("Storing "+submission.getFieldName()+" : "+file.getCanonicalPath());
		}
	}
	
	public String getId(){
		return Integer.toString(id);	
	}
	
	public boolean isBatch(){
		return isBatch;
	}	
	
	public boolean listSizesMatch(){
		return listSizesMatch;
	}
	public boolean matchedFiles(){
		return matchedFiles;
	}
	
	private class MultiFileParameter{	
		private List<Filename> filenames = new ArrayList();		
		private final CompareByFilename comparator = new CompareByFilename();
		public MultiFileParameter (String formValue){
			String[] multiFiles = formValue.split(";");
			for (int i=0; i < multiFiles.length; i++){
				if (multiFiles[i].trim().length() > 0){
					filenames.add(new Filename(multiFiles[i]));
				}
			}	
			Collections.sort(filenames, comparator);
		}
		public int getNumFiles(){
			return filenames.size();		
		}
		public List<Filename> getFilenames(){
			return filenames;		
		}
		
	}	
	
	/*
	 * Copyright (c) 1995 - 2008 Sun Microsystems, Inc. All rights reserved.
	 * 
	 * Redistribution and use in source and binary forms, with or without
	 * modification, are permitted provided that the following conditions are met:
	 *  - Redistributions of source code must retain the above copyright notice,
	 * this list of conditions and the following disclaimer.
	 *  - Redistributions in binary form must reproduce the above copyright notice,
	 * this list of conditions and the following disclaimer in the documentation
	 * and/or other materials provided with the distribution.
	 *  - Neither the name of Sun Microsystems nor the names of its contributors may
	 * be used to endorse or promote products derived from this software without
	 * specific prior written permission.
	 * 
	 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
	 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
	 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
	 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
	 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
	 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
	 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
	 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
	 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
	 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
	 * POSSIBILITY OF SUCH DAMAGE.
	 */

	/**
	 * This class assumes that the string used to initialize fullPath has a
	 * directory path, filename, and extension. The methods won't work if it
	 * doesn't.
	 */
	
	private class Filename {
	  private String fullPath;
	  private char pathSeparator = File.separatorChar;
	  private char extensionSeparator = '.';

	  public Filename(String str) {
	    fullPath = str;
	  }

	  public String filename() { // gets filename without extension
	    int dot = fullPath.lastIndexOf(extensionSeparator);
	    int sep = fullPath.lastIndexOf(pathSeparator);
	    if (dot > sep){
	    	//file has an extension
	    	return fullPath.substring(sep + 1, dot);
	    }else{
	    	//special case, no file extension
	    	return fullPath.substring(sep+1);
	    }
	  }
	  
	  public String fullPath(){
		  return fullPath;	  
	  }	 
	}
	private class CompareByFilename implements Comparator<Filename>{
		 public int compare(Filename o1, Filename o2) {
				return o1.filename().compareTo(o2.filename());
		}
	}
	
	
}