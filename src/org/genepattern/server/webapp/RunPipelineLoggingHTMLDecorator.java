package org.genepattern.server.webapp;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.SimpleTimeZone;
import java.util.Vector;

import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;


/**
 * This is the decorator for output from running a pipeline from the web environment.
 * It should generate the html for the runPipeline.jsp page as it runs and also record a log file
 * that will allow users to see when this pipeline was run, execution times and output files
 */
public class RunPipelineLoggingHTMLDecorator extends RunPipelineHTMLDecorator {

protected PrintWriter logWriter = null;
protected Date lastJobStart = null;
protected Date pipelineStart = null;
protected static SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss dd-MM-yy");
protected static SimpleDateFormat elapsedDateFormat = new SimpleDateFormat( "HH' hrs' mm' mins' ss' secs'");
protected static SimpleDateFormat titleDateFormat = new SimpleDateFormat("h:mm a EEE MMM d, ''yy");


public void beforePipelineRuns(PipelineModel model){
		super.beforePipelineRuns(model);
		String jobID = System.getProperty("jobID");
		try {
			pipelineStart = new Date();
		
		
			File jobDir = new File(".."+File.separator + jobID);
			String fileName = model.getName() + "_execution_log.html";
			File logFile = new File(jobDir, fileName);
			logWriter = new PrintWriter(new FileWriter(logFile));

			
			
			
			URL url = new URL(URL + "stylesheet.css" );
			HttpURLConnection uconn = (HttpURLConnection)url.openConnection();
			uconn.setDoInput(true);    
			InputStream  in = uconn.getInputStream();

            // read reply
            StringBuffer b = new StringBuffer("<head><STYLE TYPE=\"text/css\"> <!--");
            BufferedReader r = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = r.readLine()) != null)
                b.append(line);
            b.append("--> </style>");
            logWriter.println(b.toString());
			
			
			//logWriter.println("<link href=\""+URL+"/stylesheet.css\" rel=\"stylesheet\" type=\"text/css\">");
			
			
			logWriter.println("<link rel=\"SHORTCUT ICON\" href=\""+URL+"/gp/favicon.ico\" >");
			logWriter.println("<title>"+ model.getName()+"</title></head><body><h2>" + model.getName() + " Execution Log "+titleDateFormat.format(pipelineStart)+"</h2>");
			logWriter.println("Running as user: <b>" + System.getProperty("userID") +"</b><p>");
			
			String displayName = model.getName();
			if(displayName.endsWith(".pipeline")) {
				displayName = displayName.substring(0, displayName.length()-".pipeline".length());
			}
			
			logWriter.flush();

			// register the execution log as an output file of the pipeline
			String updateUrl = URL + "updatePipelineStatus.jsp?jobID=" + System.getProperty("jobID") + "&" + GPConstants.NAME + "=";
			updateUrl += "&filename=" + jobDir.getName() + File.separator + logFile.getName();
			url = new URL(updateUrl );
			uconn = (HttpURLConnection)url.openConnection();
	      	int rc = uconn.getResponseCode();	

		} catch (Exception e){
			e.printStackTrace();
		}
		String fileName = model.getName() + "_execution_log.html";
		out.println("<p><input type=\"checkbox\" value=\"" + fileName +"=" + jobID +"/" + fileName+"\" name=\"dl\" checked>");
		out.println("<a target=\"_blank\" href=\""+  URL+GET_TASK_FILE+"job="+jobID+"&filename="+fileName  +"\">" + fileName + "</a><p>");
	
	}

public void recordTaskExecution(JobSubmission jobSubmission, int idx, int numSteps){
	super.recordTaskExecution(jobSubmission, idx, numSteps);

	logWriter.println("<font size='+1'>"+ idx +". <a href=\"addTask.jsp?view=1&name=" + jobSubmission.getLSID() + "\">" + jobSubmission.getName() + "</a></font> " + jobSubmission.getDescription());
	logWriter.print("<div id=\"lsid"+ idx + "\" style=\"display:block;\">");
	logWriter.print("<pre>    " + jobSubmission.getLSID() + "</pre>");
	logWriter.print("</div>");

	logWriter.println("<div id=\"id"+ idx + "\" style=\"display:block;\">"); //XXX
	logWriter.flush();
	if (!jobSubmission.isVisualizer()) {
		lastJobStart = new Date(); // skip execution time summary for visualizers
	} else {
		lastJobStart = null;
	}

	ParameterInfo[] params = jobSubmission.giveParameterInfoArray();
	Vector vparams = jobSubmission.getParameters();
	logWriter.println("<table width='100%' frame='box' cellspacing='0'>");
	boolean odd = false;
	for (int i=0; i < params.length; i++){
		ParameterInfo aParam = params[i];
		boolean isInputFile = aParam.isInputFile();
		HashMap hmAttributes = aParam.getAttributes();
		String paramType = null;
		if (hmAttributes != null) paramType = (String)hmAttributes.get(ParameterInfo.TYPE);
		if (!isInputFile && !aParam.isOutputFile() && paramType != null && paramType.equals(ParameterInfo.FILE_TYPE)) {
			isInputFile = true;
		}
		isInputFile = (aParam.getName().indexOf("filename") != -1);

		if (odd){
			logWriter.println("<tr>");
		} else {
			logWriter.println("<tr  bgcolor='#EFEFFF'>");
		}
		odd = !odd;
		logWriter.println("<td WIDTH='25%'>" + aParam.getName());
		logWriter.println("</td>");

		logWriter.println("<td>" );
		if (isInputFile){
			// convert from "localhost" to the actual host name so that 
			// it can be referenced from anywhere (eg. visualizer on non-local client)
			logWriter.print("<a href=\"");
			logWriter.print(aParam.getValue());
			logWriter.print("\">");

		}
		logWriter.print(htmlEncode(aParam.getValue()));
	
		if (isInputFile){
			out.println("</a>");
		}

		logWriter.println("</td>");
		logWriter.println("</tr>");
	}

	logWriter.println("</table>");
	logWriter.flush();
	
		
}


public void recordTaskCompletion(JobInfo jobInfo, String name){
	ParameterInfo[] jobParams = jobInfo.getParameterInfoArray();
	StringBuffer sbOut = new StringBuffer();
	boolean hasOutput = false;
	for(int j = 0; j < jobParams.length; j++){
       	if(jobParams[j].isOutputFile()){
       		hasOutput = true;
       		break;
       	}
	}
	
	if (hasOutput){
		logWriter.println("<table width='100%' >");
		logWriter.println("<tr><td colspan=2 align='left'><b>Output Files:</b></td></tr>");
	 	for(int j = 0; j < jobParams.length; j++){
		       	if(!jobParams[j].isOutputFile()){
				continue;
			}
			
			sbOut.setLength(0);
			String fileName = new File("../../" + jobParams[j].getValue()).getName();	
							
			sbOut.append("<tr><td width='25%'>&nbsp;</td><td><a target=\"_blank\" href=\"");
	
			String outFileUrl = null;
			try {
				outFileUrl = URL+GET_TASK_FILE+"job="+jobInfo.getJobNumber()+"&filename="+URLEncoder.encode(fileName, "utf-8");
			} catch (UnsupportedEncodingException uee) {
				outFileUrl = URL+GET_TASK_FILE+"job="+jobInfo.getJobNumber()+"&filename="+fileName;
			}
	                            
			sbOut.append(localizeURL(outFileUrl));
			try {
				fileName = URLDecoder.decode(fileName, "UTF-8");
			} catch (UnsupportedEncodingException uee) {
				// ignore
			}
			sbOut.append("\">"+htmlEncode(fileName)+"</a></td></tr>");
			logWriter.println(sbOut.toString());
		}
	 	logWriter.println("</table>");
	} // END OF OUTOPUT FILE WRITING	
	
	// ============================================================================
	if (lastJobStart != null){
		Date endTime = 	new Date();	
		String formattedElapsedTime = getElapsedTime(lastJobStart, endTime);
			
		logWriter.println("<table>");
	
		logWriter.println("<tr colspan='2'><td><b>Execution Times:</b></td></tr>");
		logWriter.println("<tr><td width='25%'>Submitted: </td><td>" + dateFormat.format(lastJobStart));
		logWriter.println("</td></tr>");
		logWriter.println("<tr><td width='25%'>Completed: </td><td>" + dateFormat.format(endTime ));
		logWriter.println("</td></tr>");
		logWriter.println("<tr><td width='25%'>Elapsed: </td><td>" + formattedElapsedTime);
		logWriter.println("</td></tr>");
		logWriter.println("</table><p>");
	}
	logWriter.flush();
		super.recordTaskCompletion(jobInfo, name);
	


	}


public void afterPipelineRan(PipelineModel model){
		Date endTime = 	new Date();	
	
		String jobID = System.getProperty("jobID");
	
		String formattedElapsedTime = getElapsedTime(pipelineStart, endTime);
		
		logWriter.println("<table>");

		logWriter.println("<tr colspan='2'><td><h2>Pipeline Execution Times:</h2></td></tr>");
		logWriter.println("<tr><td width='25%'>Submitted: </td><td>" + dateFormat.format(pipelineStart));
		logWriter.println("</td></tr>");
		logWriter.println("<tr><td width='25%'>Completed: </td><td>" + dateFormat.format(endTime ));
		logWriter.println("</td></tr>");
		logWriter.println("<tr><td width='25%'>Elapsed: </td><td>" + formattedElapsedTime);
		logWriter.println("</td></tr>");
		logWriter.println("</table><p>");
		
		
		// add the link to the execution log
		
		super.afterPipelineRan(model);		
		logWriter.flush();
		logWriter.close();
	}

	public String getElapsedTime(Date startTime, Date endTime){
		long deltaMillis = endTime.getTime() - pipelineStart.getTime();
		Calendar cal = new GregorianCalendar();
		cal.setTimeInMillis( deltaMillis );
		cal.setTimeZone(new SimpleTimeZone(0,""));
		elapsedDateFormat.setTimeZone(new SimpleTimeZone(0,""));// set to GMT for the calculation
		return elapsedDateFormat.format( cal.getTime() );
	
	}

}