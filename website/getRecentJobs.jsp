<%@ page import="java.io.IOException,
		java.util.Enumeration,
		java.util.HashMap,
		java.io.*,
		java.util.*,
 		java.text.*,
 		java.net.*,
		org.genepattern.webservice.JobInfo,
		org.genepattern.webservice.JobStatus,
		org.genepattern.webservice.ParameterInfo,
		org.genepattern.webservice.WebServiceException,
       	org.genepattern.server.webservice.server.local.*,
		org.genepattern.server.webservice.server.local.LocalTaskIntegratorClient , 
		org.genepattern.server.util.AuthorizationManagerFactoryImpl,
		org.genepattern.server.util.IAuthorizationManager,
		org.genepattern.webservice.TaskInfo,
		org.genepattern.webservice.TaskInfoAttributes,
		org.genepattern.webservice.ParameterFormatConverter,
		org.genepattern.webservice.ParameterInfo,
		org.genepattern.server.util.AccessManager,
		org.genepattern.util.LSID,
		org.genepattern.util.StringUtils,
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

</head><body  >
	
<script language="JavaScript">

function showJob(job) {
	window.open('showJob.jsp?jobId=' + job, 'Job ' + job,'toolbar=no, location=no, status=no, resizable=yes, scrollbars=yes, menubar=no, width=550, height=240')

}



function createPipeline(filename) {
	
	var proceed = window.confirm("Create a pipeline that describes how this file was created?");
	if (proceed == null || proceed == false) {
		return false;
	}
		
	var pipeName = window.prompt("Name of pipeline", "");
		
	// user cancelled?
	if (pipeName == null || pipeName.length == 0) {
		return false;
	}

	window.open("provenanceFinder.jsp?pipelinename="+pipeName+"&filename="+filename);
	return false;
	
}


</script>

<table   margin=0  width=100% height='100%' class="paleBackground"  valign='top'>

<tr><td class="heading" colspan=3><span class="heading">Recent Jobs</span></td></tr><tr>

<%
String userID = (String)request.getAttribute("userID"); // get userID but don't force login if not defined
IAuthorizationManager authorizationManager = (new AuthorizationManagerFactoryImpl()).getAuthorizationManager();


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
      jobs = analysisClient.getJobs(userID, -1, Integer.MAX_VALUE, true);
} catch(WebServiceException wse) {
	wse.printStackTrace();
}
String serverURL = "http://"+ InetAddress.getLocalHost().getCanonicalHostName() + ":"+ System.getProperty("GENEPATTERN_PORT") +"/"+ request.getContextPath();

int numJobsToDisplay = 15; 
int jobsDisplayed = 0; // increment for each <tr> in this table

boolean[] hasLog = new boolean[jobs.length];

//// GET THE EXECUTION LOG FOR WRITING TO THE TEXTAREA
for(int i = 0; i < jobs.length; i++) {
   JobInfo job = jobs[i];
   job = analysisClient.getJob(job.getJobNumber());

   if(!job.getStatus().equals(JobStatus.FINISHED) ) continue;
   StringBuffer buff2 = new StringBuffer();
   StringBuffer buff = new StringBuffer();
			
   out.print("<tr><td align=\"right\" >" + job.getJobNumber() + "");
   jobsDisplayed++;
   ParameterInfo[] params = job.getParameterInfoArray();
   
   out.print("<td valign='center'><span name='"+job.getJobNumber()+"'onClick='showJob("+job.getJobNumber()+")'><nobr>" + job.getTaskName());

   out.print("&nbsp;");
   
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

			String fileUrl = "retrieveResults.jsp?job=" + jobNumber + "&filename=" + URLEncoder.encode(fileName, "utf-8");

           		out.println("<a href=\""+ fileUrl+ "\" target=\"_blank\">" + fileName + "</a>");
   	     		//jobsDisplayed++;

			if (authorizationManager.checkPermission("createPipeline", userID)){

 				out.print("<span  onClick=\"createPipeline(\'"+URLEncoder.encode(serverURL + fileUrl, "utf-8")+"\')\"><nobr>" );
   				out.print("&nbsp;");
    				out.print("<img src='skin/pipe_obj.jpeg'>");
    				out.print( "  </nobr></span>");
			}
		}
           }
      }
   }

   if (jobsDisplayed >= numJobsToDisplay) break;
}

if (jobsDisplayed == 0){
 out.print("<tr><td colspan=3 align=\"center\" >No completed jobs available to display</td></tr>");
}
int numRows = jobsDisplayed;
while (numRows < (numJobsToDisplay)){
	 out.print("<tr><td colspan=3 align=\"right\" >&nbsp;</td></tr>");
	numRows++;

}

out.println("</td></tr>");
out.println("</table>");

%>
</body>