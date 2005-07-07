<%@ page import="java.io.IOException,
		 java.util.Enumeration,
		 java.util.HashMap,
		 java.io.*,
		 java.util.*,
 		 java.text.*,
 		 java.net.URLEncoder,
		org.genepattern.webservice.JobInfo,
		 org.genepattern.webservice.JobStatus,
		 org.genepattern.webservice.ParameterInfo,
		 org.genepattern.webservice.WebServiceException,
       	org.genepattern.server.webservice.server.local.*,
		 org.genepattern.server.webservice.server.local.LocalTaskIntegratorClient , 
		 org.genepattern.webservice.TaskInfo,
		 org.genepattern.webservice.TaskInfoAttributes,
		 org.genepattern.webservice.ParameterFormatConverter,
		 org.genepattern.webservice.ParameterInfo,
		 org.genepattern.server.util.AccessManager,
		 org.genepattern.util.LSID,
		 org.genepattern.util.GPConstants,
		 org.genepattern.webservice.OmnigeneException, 
		 org.genepattern.data.pipeline.PipelineModel"
	session="false" contentType="text/html" language="Java" %><%

response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
response.setHeader("Pragma", "no-cache");		 // HTTP 1.0 cache control
response.setDateHeader("Expires", 0);
%>
<head>
<link href="skin/stylesheet.css" rel="stylesheet" type="text/css">
</head>	
<script language="JavaScript">

var logFileContents = new Array(); 

function showJob(job) {
	execLogArea = parent.document.execLogForm.execLogArea;	
	execLogArea.value = logFileContents[job];
}


</script>

<table   frame=border xwidth='100%' height='100%' class="paleBackground"  valign='top'>

<tr><td class="heading" colspan=3><span class="heading">Recent Jobs</span></td></tr><tr>

<%
String userID = (String)request.getAttribute("userID"); // get userID but don't force login if not defined

SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd HH:mm:ss");
SimpleDateFormat shortDateFormat = new SimpleDateFormat("HH:mm:ss");
Calendar midnight = Calendar.getInstance();
midnight.set(Calendar.HOUR_OF_DAY, 0);
midnight.set(Calendar.MINUTE, 0);
midnight.set(Calendar.SECOND, 0);
midnight.set(Calendar.MILLISECOND, 0);

JobInfo[] jobs = null;
LocalAnalysisClient analysisClient = new LocalAnalysisClient(userID);
try {
      jobs = analysisClient.getJobs(userID, -1, Integer.MAX_VALUE, false);
} catch(WebServiceException wse) {
	wse.printStackTrace();
}

int numJobsToDisplay = 15; 
int jobsDisplayed = 0; // increment for each <tr> in this table

boolean[] hasLog = new boolean[jobs.length];

//// GET THE EXECUTION LOG FOR WRITING TO THE TEXTAREA
for(int i = 0; i < jobs.length; i++) {
   JobInfo job = jobs[i];
   hasLog[i] = false;
   if(!job.getStatus().equals(JobStatus.FINISHED) ) continue;
  
   out.print("<tr><td align=\"right\" >" + job.getJobNumber() + "");
   jobsDisplayed++;
   ParameterInfo[] params = job.getParameterInfoArray();
      String log = "execution log unavailable for job " + job.getJobNumber();

   if(params!=null && params.length > 0) {    
      for(int j = 0; j < params.length; j++) {
         ParameterInfo parameterInfo = params[j];
         if(parameterInfo.isOutputFile()) {
		String value = parameterInfo.getValue();
           	int index = value.lastIndexOf(File.separator);
	     	String altSeperator = "/";
	     	if (index == -1) index = value.lastIndexOf(altSeperator);
		String jobNumber = value.substring(0, index);
		String fileName = value.substring(index + 1, value.length());
           
		boolean upToParams = false;      
	     	if (GPConstants.TASKLOG.equals(fileName)){
			File logFile = new File("temp/"+value);
			if (!logFile.exists()) continue;
			BufferedReader reader = new BufferedReader(new FileReader(logFile));
			String line = null;
			StringBuffer buff = new StringBuffer();
			while ((line = reader.readLine()) != null){
				if (!upToParams){
					int idx = line.indexOf("# Parameters");
					if (idx >= 0) upToParams = true;
					continue;
				} 
				String trimline = line.substring(1).trim(); // remove hash and spaces
				

				buff.append(trimline);
				buff.append("\\n");
			}	
			log = buff.toString();
			hasLog[i] = true;
		}
			
	   }
	}
   }
  // END OF GETTING THE EXECUTION LOG
  out.println("<script language='javascript'>");

  out.println("logFileContents["+job.getJobNumber()+"]='" + log+ "';");

  out.println("</script>");


   out.print("<td valign='center'><span name='"+job.getJobNumber()+"'onmouseover='showJob("+job.getJobNumber()+")'><nobr>" + job.getTaskName());

   out.print("&nbsp;");
   if (hasLog[i])
	out.print("<img src='skin/info_obj.gif'>");
    out.print( "  </nobr></span>");


   Date completed = job.getDateCompleted();
   DateFormat formatter =  completed.after(midnight.getTime()) ? shortDateFormat : dateFormat;
   
   out.print("<td>" + formatter.format(completed)+"</td>");
   
   if(params!=null && params.length > 0) {
    
      boolean firstOutputFile = true;  
      boolean hasOutputFiles = false;
      for(int j = 0; j < params.length; j++) {
         ParameterInfo parameterInfo = params[j];
 
         if(parameterInfo.isOutputFile()) {

            if(firstOutputFile) {
               firstOutputFile = false;
               hasOutputFiles = true;
            }
           String value = parameterInfo.getValue();
           int index = value.lastIndexOf(File.separator);
	     String altSeperator = "/";
	     if (index == -1) index = value.lastIndexOf(altSeperator);

           String jobNumber = value.substring(0, index);
           String fileName = value.substring(index + 1, value.length());
                 
	     if (!GPConstants.TASKLOG.equals(fileName)){ 
           		out.println("<tr><td></td><td valign='top' colspan=\"3\">");
           		out.println("<a href=\"retrieveResults.jsp?job=" + jobNumber + "&filename=" + URLEncoder.encode(fileName, "utf-8") + "\" target=\"_blank\">" + fileName + "</a>");
   	     		//jobsDisplayed++;
		}
           }
      }
   }

// System.out
   if (jobsDisplayed >= numJobsToDisplay) break;
}

if (jobsDisplayed == 0){
 out.print("<tr><td colspan=3 align=\"right\" >No completed jobs available to display</td></tr>");
}
int numRows = jobsDisplayed;
while (numRows < (numJobsToDisplay)){
	 out.print("<tr><td colspan=3 align=\"right\" >&nbsp;</td></tr>");
	numRows++;

}

out.println("</td></tr>");
//out.println("<tr><td colspan=3><form name='execLogForm'><TEXTAREA name='execLogArea' style=\"font-size:9px;font-family: arial, helvetica, sans-serif;width: 100%;\" rows='5'  readonly wrap='soft' bgcolor='#EFEFFF'></textarea></form></td></tr>");
out.println("</table>");

%>