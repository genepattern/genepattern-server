package org.genepattern.server.webapp;

import java.io.BufferedWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.genepattern.analysis.JobInfo;
import org.genepattern.analysis.JobStatus;
import org.genepattern.analysis.ParameterInfo;
import org.genepattern.analysis.TaskInfo;
import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.server.analysis.genepattern.GenePatternAnalysisTask;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;

public class RunPipelineForJsp {
    public static int jobID = -1;
    
    public RunPipelineForJsp(){
    }
    
   /** 
   * Checks the given pipeline to see if all tasks that it uses are installed on the server. Writes an error message to the given <code>PrintWriter</code> if there are missing tasks.
   * @param model the pipeline model
   * @param out the writer
   * @param userID the user id
   * @return <code>true</code> if the pipeline is missing tasks
   */
   public static boolean isMissingTasks(PipelineModel model, java.io.PrintWriter out, String userID) throws Exception {
      boolean isMissingTasks = false;
      java.util.List tasks = 	model.getTasks();		
      HashMap unknownTaskNames = new HashMap();
      HashMap unknownTaskVersions = new HashMap();
      for(int ii = 0; ii < tasks.size(); ii++) {
         JobSubmission js = (JobSubmission) tasks.get(ii);
         TaskInfo formalTask = GenePatternAnalysisTask.getTaskInfo(js.getName(), userID);
         boolean unknownTask = !GenePatternAnalysisTask.taskExists(js.getLSID(), userID);
         boolean unknownTaskVersion = false;
         if(unknownTask) {
            isMissingTasks = true;
            // check for alternate version
            String taskLSIDstr = js.getLSID();
            LSID taskLSID = new LSID(taskLSIDstr);
            String taskLSIDstrNoVer = taskLSID.toStringNoVersion();
            unknownTaskVersion = GenePatternAnalysisTask.taskExists(taskLSIDstrNoVer, userID);
            if(unknownTaskVersion) {
               unknownTaskVersions.put(js.getName(), taskLSID);
            } else {
               unknownTaskNames.put(js.getName(), taskLSID);
            }
         }
      }

      if((unknownTaskNames.size() + unknownTaskVersions.size()) > 0) {
         out.println("<font color='red' size=\"+1\"><b>Warning:</b></font><br>The following task versions do not exist on this server. Before running this pipeline you will need to edit the pipeline to use the available version or import them.");
         out.println("<table width='100%'  border='1'>");
         out.println("<tr bgcolor='#efefff'><td> Name </td><td> Required Version</td><td> Available Version</td><td>LSID</td></tr>");

      }
      if((unknownTaskNames.size() + unknownTaskVersions.size()) > 0) {
         out.println("<form method=\"post\" action=\"taskCatalog.jsp\">");
      }

      if(unknownTaskNames.size() > 0) {

         for(Iterator iter = unknownTaskNames.keySet().iterator(); iter.hasNext(); ) {
            String name = (String) iter.next();
            LSID absentlsid = (LSID) unknownTaskNames.get(name);

            out.println("<input type=\"hidden\" name=\"LSID\" value=\"" + absentlsid + "\" /> ");

            out.println("<tr><td>" + name + "</td><td>" + absentlsid.getVersion() + "</td><td></td><td> " + absentlsid.toStringNoVersion() + "</td></tr>");
         }

      }
      if(unknownTaskVersions.size() > 0) {
         for(Iterator iter = unknownTaskVersions.keySet().iterator(); iter.hasNext(); ) {
            String name = (String) iter.next();
            LSID absentlsid = (LSID) unknownTaskVersions.get(name);

            out.println("<input type=\"hidden\" name=\"LSID\" value=\"" + absentlsid + "\" /> ");

            TaskInfo altVersionInfo = GenePatternAnalysisTask.getTaskInfo(absentlsid.toStringNoVersion(), userID);
            Map altVersionTia = altVersionInfo.getTaskInfoAttributes();

            LSID altVersionLSID = new LSID((String) (altVersionTia.get(GPConstants.LSID)));


            out.println("<tr><td>" + name + "</td><td> " + absentlsid.getVersion() + "</td><td>" + altVersionLSID.getVersion() + "</td><td>" + absentlsid.toStringNoVersion() + "</td></tr>");
         }
      }
      if((unknownTaskNames.size() + unknownTaskVersions.size()) > 0) {
         out.println("<tr bgcolor='#efefff'>");
         out.println("<td colspan='4' align='center' border = 'none'> <a href='addZip.jsp'>Import zip file </a>");
         out.println(" &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; ");

         out.println("<input type=\"submit\" value=\"install/update from catalog\"  ></td></form>");
         out.println("</tr>");

         out.println("</table>");
      }
      return isMissingTasks;
   }



    public static boolean deletePipelineDirAfterRun(String pipelineName){
        boolean deleteDirAfterRun = false;
        // determine if we want to delete the dir in tasklib after running
        // we do this if there is no pipeline saved by the same name.
        try {
            TaskInfo  savedTaskInfo = GenePatternAnalysisTask.getTaskInfo(pipelineName, null);
        } catch (Exception e){
            // exception means it isn't in the DB therefore not saved
            deleteDirAfterRun=true;
        }
        return deleteDirAfterRun;
    }
    
    public static boolean isSavedModel(TaskInfo taskInfo, String pipelineName, String userID){
        boolean savedPipeline = true;
        // determine if we want a link to the pipeline.  Check if one existis in the DB
        // by the same name and that the model matches what we have here
        try {
            TaskInfo  savedTaskInfo = GenePatternAnalysisTask.getTaskInfo(pipelineName, userID);
            Map sTia = savedTaskInfo.getTaskInfoAttributes();
            String savedSerializedModel = (String)sTia.get(GPConstants.SERIALIZED_MODEL);
            Map tia = taskInfo.getTaskInfoAttributes();
            String serializedModel = (String)tia.get(GPConstants.SERIALIZED_MODEL);
            if (!savedSerializedModel.equals(serializedModel)) savedPipeline = false;
            
        } catch (Exception e){
            // exception means it isn't in the DB therefore not saved
            savedPipeline = false;
            
        }
        return savedPipeline;
    }
    
    public static void stopPipeline(String jobID) throws Exception {
        Process p = null;
        for (int i = 0; i < 10; i++) {
                p = GenePatternAnalysisTask.terminatePipeline(jobID);
                if (p != null) break;
                Thread.currentThread().sleep(1000);
        }
        GenePatternAnalysisTask.updatePipelineStatus(Integer.parseInt(jobID), JobStatus.JOB_ERROR, null);
        if (p != null) p.destroy();

        return;

    }
    
    
      
    public static String[] generatePipelineCommandLine(String name, String jobID, String userID, String baseURL, TaskInfo taskInfo, HashMap commandLineParams, java.io.File tempDir, String decorator ) throws Exception {
        String JAVA_HOME = System.getProperty("java.home");
        boolean savedPipeline = isSavedModel(taskInfo, name, userID);
        // these jar files are required to execute
        // analysis.jar; 			gpclient.jar;
        // log4j-1.2.4.jar; 		xerces.jar;
        // activation.jar;		saaj.jar
        // axis.jar;			jaxrpc.jar;
        // commons-logging.jar;		commons-discovery.jar;
        
	String tomcatLibDir=System.getProperty("tomcatCommonLib") + "/";
	String webappLibDir=System.getProperty("webappDir") + "/" +"WEB-INF"+ "/" +"lib"+ "/";
	String resourcesDir=null;
	resourcesDir = new java.io.File(System.getProperty("resources")).getAbsolutePath() + "/";

        ArrayList cmdLine = new ArrayList();
        cmdLine.add(JAVA_HOME + java.io.File.separator + "bin" + java.io.File.separator + "java");
        cmdLine.add("-cp");
        StringBuffer classPath = new StringBuffer();
        
        classPath.append(tomcatLibDir + "activation.jar" +  java.io.File.pathSeparator);
        classPath.append(tomcatLibDir + "xerces.jar" +  java.io.File.pathSeparator);
        classPath.append(tomcatLibDir + "saaj.jar" +  java.io.File.pathSeparator);
        classPath.append(tomcatLibDir + "jaxrpc.jar" +  java.io.File.pathSeparator);
	String[] jars = new java.io.File(webappLibDir).list();
	for (int i = 0; i < jars.length; i++) {
	        classPath.append(webappLibDir + jars[i] +  java.io.File.pathSeparator);
	}
        
        cmdLine.add(classPath.toString());
        cmdLine.add("-Ddecorator="+decorator);
        cmdLine.add("-DjobID="+jobID);
        cmdLine.add("-Djobs="+System.getProperty("jobs") );
        cmdLine.add("-DsavedPipeline="+savedPipeline);
        cmdLine.add("-Domnigene.conf="+resourcesDir);
        cmdLine.add("-Dgenepattern.properties=" + resourcesDir);
        cmdLine.add("-DGenePatternURL="+System.getProperty("GenePatternURL") );
        cmdLine.add("-D" + GPConstants.LSID + "="+(String)taskInfo.getTaskInfoAttributes().get(GPConstants.LSID));
        cmdLine.add("edu.mit.genome.gp.ui.analysis.RunPipeline");
        
        
        //-------------------------------------------------------------
        //------Serialize the pipeline model for the java executor-----
        
        Map tia = taskInfo.getTaskInfoAttributes();
        
        String pipelineShortName = name;
        if (pipelineShortName != null) {
            int i = pipelineShortName.indexOf(" ");
            if (i != -1) pipelineShortName = pipelineShortName.substring(0, i);
        }
        
        String serializedModel = (String)tia.get(GPConstants.SERIALIZED_MODEL);
        java.io.File pipeFile = new java.io.File(tempDir, pipelineShortName+".xml");
        BufferedWriter writer = new BufferedWriter(new java.io.FileWriter(pipeFile));
        writer.write(serializedModel );
        writer.flush();
        writer.close();
        //-------------------------------------------------------------
        
        cmdLine.add(pipeFile.getName());
        cmdLine.add(userID);
        
        // add any command line parameters
        // first saving the files locally
        for (Iterator iter = commandLineParams.keySet().iterator(); iter.hasNext(); ){
            String key = (String)iter.next();
            String val = "";
            
            try {
                val = (String)commandLineParams.get(key);
            } catch (ClassCastException e){
                com.jspsmart.upload.File attachedFile = (com.jspsmart.upload.File)commandLineParams.get(key);
                java.io.File file = new java.io.File( tempDir, attachedFile.getFilePathName());
                attachedFile.saveAs(file.getAbsolutePath());
                try {
                    val = baseURL +"retrieveResults.jsp?job="+URLEncoder.encode(jobID, "utf-8")+"&filename="+URLEncoder.encode(file.getName(), "utf-8");
                } catch (UnsupportedEncodingException uee) {
                }
            }
            cmdLine.add(key + "=" + val);
        }
        return (String[]) cmdLine.toArray(new String[0]);
    }
    
    /**
      * Collect the command line params from the request and see if they are all present
     */
    public static boolean validateAllRequiredParametersPresent(TaskInfo taskInfo, HashMap commandLineParams){
        
        boolean paramsRequired = false;
        ParameterInfo[] parameterInfoArray = taskInfo.getParameterInfoArray();
        if (parameterInfoArray != null && parameterInfoArray.length > 0) {
            paramsRequired = true;
            int count = parameterInfoArray.length;
            
            for (int i=0; i < parameterInfoArray.length; i++){
                ParameterInfo param = parameterInfoArray[i];
                String key = param.getName();
                String val = (String)commandLineParams.get(key); // get non-file param
                if (val != null){
                    count--;
                } 
            }
            if (count == 0) paramsRequired = false;
        }    
        return paramsRequired;
    }
    
    
    
    
    public static int getJobID(){
        return jobID;
    }
    
    public static Process runPipeline(TaskInfo taskInfo, String name, String baseURL, String decorator, String userID, HashMap commandLineParams) throws Exception{
        
        JobInfo jobInfo = GenePatternAnalysisTask.createPipelineJob(userID, "", taskInfo.getName());
        jobID = jobInfo.getJobNumber();
        
        String pipelineShortName = taskInfo.getName();
        if (pipelineShortName != null) {
                int i = pipelineShortName.indexOf(" ");
                if (i != -1) pipelineShortName = pipelineShortName.substring(0, i);
        }
        java.io.File tempDir = java.io.File.createTempFile("pipe", pipelineShortName, new java.io.File(System.getProperty("jobs")));
        tempDir.delete();
        tempDir.mkdirs();

        if (decorator == null) decorator = "edu.mit.genome.gp.ui.analysis.RunPipelineHTMLDecorator";

        boolean deleteDirAfterRun = RunPipelineForJsp.deletePipelineDirAfterRun(taskInfo.getName());   

        String[] commandLine = RunPipelineForJsp.generatePipelineCommandLine(taskInfo.getName(), ""+jobID ,  userID,  baseURL, taskInfo, commandLineParams, tempDir, decorator);

        // spawn the command
        final Process process = Runtime.getRuntime().exec(commandLine, null, tempDir);

        GenePatternAnalysisTask.startPipeline(Integer.toString(jobID), process);

        WaitForPipelineCompletionThread waiter = new WaitForPipelineCompletionThread(process, jobID);
        waiter.start();
        
        if (deleteDirAfterRun){
            DeleteUnsavedTasklibDirThread delThread = new DeleteUnsavedTasklibDirThread(taskInfo, process);
            delThread.start();
        }
        
        return process;
   
    } 
}


class WaitForPipelineCompletionThread extends Thread {
    Process process = null;
    int jobID = -1;
    WaitForPipelineCompletionThread(Process aProcess, int aJobID){
        process = aProcess;
        jobID = aJobID;
    }
    
    public void run(){
        try {
            process.waitFor();
            GenePatternAnalysisTask.terminatePipeline(Integer.toString(jobID));  
        } catch (Exception e){
            try {
                GenePatternAnalysisTask.updatePipelineStatus(jobID, JobStatus.JOB_ERROR, null);
                GenePatternAnalysisTask.terminatePipeline(Integer.toString(jobID));
            } catch (Exception ee){
                //ignore
            }
       }
    }
}
 

class DeleteUnsavedTasklibDirThread extends Thread {
    TaskInfo taskInfo;
    Process process;
    
    DeleteUnsavedTasklibDirThread(TaskInfo taskInfo, Process process){
        this.taskInfo = taskInfo;
	this.process = process;
    }
    
    public void run(){
      
	    try {
		    process.waitFor();
	    } catch (InterruptedException ie)  {
		// ignore
	    }

	    try {
		java.io.File fDir = new java.io.File(GenePatternAnalysisTask.getTaskLibDir(taskInfo));
		// delete the temp files now
		if (fDir.exists()){
                    java.io.File[] children = fDir.listFiles();
                    for (int i=0; i < children.length; i++){
                            System.out.println("Deleting " + children[i].getCanonicalPath());
                            children[i].delete();
                    }
		    System.out.println("Deleting directory " + fDir.getCanonicalPath());
                    fDir.delete();
		}
            } catch (Exception e) {
		// ignore
	    }
        } // run
}








