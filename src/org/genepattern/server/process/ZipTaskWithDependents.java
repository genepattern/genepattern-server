package org.genepattern.server.process;


import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Vector;
import java.util.zip.ZipOutputStream;

import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.server.analysis.TaskInfo;
import org.genepattern.server.analysis.TaskInfoAttributes;
import org.genepattern.server.analysis.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.util.OmnigeneException;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;

       

public class ZipTaskWithDependents extends ZipTask {
    
    public ZipTaskWithDependents(){
    }
    
        
    public File packageTask(String name, String userID) throws Exception{

	if (name == null || name.length() == 0) {
		throw new Exception("Must specify task name as name argument to this page");
	}

	TaskInfo taskInfo = null;
	try {
		taskInfo = GenePatternAnalysisTask.getTaskInfo(name, userID);
	} catch(OmnigeneException e){
            //this is a new task, no taskID exists
            // do nothing
	    throw new Exception("no such task: " + name);
        }
	return packageTask(taskInfo, userID);
    }

   
    public File packageTask(TaskInfo taskInfo, String userID) throws Exception {
	
	TaskInfoAttributes tia = taskInfo.giveTaskInfoAttributes();
        String serializedModel = (String)tia.get(GPConstants.SERIALIZED_MODEL);
	if (serializedModel != null && serializedModel.trim().length() > 0) {
		String name = taskInfo.getName();
		// use an LSID-unique name so that different versions of same named task don't collide within zip file
		String suffix = "_" + Integer.toString(Math.abs(tia.get(GPConstants.LSID).hashCode()), 36); // [a-z,0-9]
		// create zip file
		File zipFile = File.createTempFile(name + suffix, ".zip");
		ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile));
		try {
 	  		// find dependent tasks (if a pipeline) and add them to the zip file as zip files
			zipDependentTasks(zos, taskInfo, userID);
			zos.finish();
			zos.close();
			return zipFile;      
		} catch (Exception e) {
 			zos.close();
			zipFile.delete();
			throw e;
		}
 	} else {
		return super.packageTask(taskInfo, userID);
 	}
    }
 
	public void zipDependentTasks(ZipOutputStream zos, TaskInfo taskInfo, String userID) throws Exception {

		ZipOutputStream localZos = zos;
		
  		Vector files = new Vector();
		File tmpDir = new File(System.getProperty("java.io.tmpdir"), taskInfo.getName()+"_dep_" + System.currentTimeMillis());
		try {
	  		tmpDir.mkdir();
			File parent = super.packageTask(taskInfo, userID);
			zipTaskFile(zos, parent);
			parent.delete();
          
			String serializedModel = (String)taskInfo.getTaskInfoAttributes().get(GPConstants.SERIALIZED_MODEL);
			if (serializedModel != null && serializedModel.trim().length() > 0) {
	
	    			PipelineModel model = null;
    				try {			
    	        	    		model = PipelineModel.toPipelineModel(serializedModel);
  	  			} catch (Exception e){
    					e.printStackTrace();
	    				return;
  		  		}
	
  		  		Vector vTasks = model.getTasks();
    				int taskNum = 0;
	  			String message = "";

	  			// validate availability of all dependent tasks
  		  		for (Enumeration eTasks = vTasks.elements(); eTasks.hasMoreElements(); taskNum++) {
    					JobSubmission jobSubmission = (JobSubmission)eTasks.nextElement();
    					String taskLsid = jobSubmission.getLSID();
    					TaskInfo depti = GenePatternAnalysisTask.getTaskInfo(taskLsid, userID);
	  	           		if (depti == null) {
    						message = message + taskInfo.getName() + " refers to task # " + 
							  (taskNum+1) + " " + jobSubmission.getName();
    						depti = GenePatternAnalysisTask.getTaskInfo(jobSubmission.getName(), userID);
    						if (depti != null) {
    							LSID available = new LSID(depti.giveTaskInfoAttributes().get(GPConstants.LSID));
    							LSID requested = new LSID(taskLsid);
  	  						if (available.isSimilar(requested)) {
	    							message = message + " version " + requested.getVersion() + 
    										" but version " + available.getVersion() + " is available.\n";
    							} else {
    								message = message  + " (" + taskLsid + ").\n";
    							}
  	  					} else {
    							message = message + " which does not exist "  + " (" + taskLsid + ").\n";
	    					}
  		  			}
    				}
    				if (message.length() > 0) throw new Exception(message);

	  			// done validating, now actually do the zipping
				taskNum = 0;
				Vector vIncludedLSIDs = new Vector();
  		  		for (Enumeration eTasks = vTasks.elements(); eTasks.hasMoreElements(); taskNum++) {
    					JobSubmission jobSubmission = (JobSubmission)eTasks.nextElement();
    					String taskLsid = jobSubmission.getLSID();
					// don't include the same LSID more than once
					if (vIncludedLSIDs.contains(taskLsid)) continue;
					vIncludedLSIDs.add(taskLsid);
    					TaskInfo depti = GenePatternAnalysisTask.getTaskInfo(taskLsid, userID);
    					File dependent = packageTask(depti, userID);
    					dependent.deleteOnExit();
					String comment = taskLsid;
  					zipTaskFile(zos, dependent, comment);
					dependent.delete();
    				}
			}
		} finally {
 			tmpDir.delete();
		}
	}
}
