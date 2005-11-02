<%@ page import="java.io.IOException,
		 java.util.Enumeration,
		 java.util.HashMap,
		 java.io.*,
		 java.net.URL,
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
		 org.genepattern.util.StringUtils,
		 org.genepattern.util.GPConstants,
		 org.genepattern.webservice.OmnigeneException, 
		 org.genepattern.data.pipeline.PipelineModel"
	session="false" contentType="text/html" language="Java" %><%

response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
response.setHeader("Pragma", "no-cache");		 // HTTP 1.0 cache control
response.setDateHeader("Expires", 0);
String jobId = request.getParameter("jobId");
String userID = (String)request.getAttribute("userID"); // get userID but don't force login if not defined

JobInfo job = null;
LocalAnalysisClient analysisClient = new LocalAnalysisClient(userID);
try {
      job = analysisClient.getJob((new Integer(jobId)).intValue());
} catch(WebServiceException wse) {
	wse.printStackTrace();
}

%>
<head>
<link href="skin/stylesheet.css" rel="stylesheet" type="text/css">
</head>	
<script language="JavaScript">


</script>

<table   frame=border xwidth='100%' class="paleBackground"  valign='top'>

<tr><td align='left' class="heading" colspan=3><span class="heading">Job <%=jobId%> <%= job.getTaskName()%></span></td></tr><tr>

<%
SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd HH:mm:ss");
SimpleDateFormat shortDateFormat = new SimpleDateFormat("HH:mm:ss");
Calendar midnight = Calendar.getInstance();
midnight.set(Calendar.HOUR_OF_DAY, 0);
midnight.set(Calendar.MINUTE, 0);
midnight.set(Calendar.SECOND, 0);
midnight.set(Calendar.MILLISECOND, 0);


boolean hasLog = false;
int outFileCount=1;

//// GET THE EXECUTION LOG FOR WRITING TO THE TEXTAREA
   job = analysisClient.getJob(job.getJobNumber());
   hasLog = false;
   StringBuffer buff2 = new StringBuffer();
   StringBuffer buff = new StringBuffer();
			
   out.print("<tr>");
   ParameterInfo[] params = job.getParameterInfoArray();

  
   Date completed = job.getDateCompleted();
   DateFormat formatter =  completed.after(midnight.getTime()) ? shortDateFormat : dateFormat;
   
   out.print("<td> completed: " + formatter.format(completed)+"</td>");
   out.println("<tr><td colspan=2><P><B>output Files</b></td></tr>");  
      
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
                 
	     if (!GPConstants.TASKLOG.equals(fileName)){ 
           		out.println("<tr><td align=right >"+outFileCount++ +".</td><td valign='top' colspan=\"3\">");
           		out.println("<a href=\"retrieveResults.jsp?job=" + jobNumber + "&filename=" + URLEncoder.encode(fileName, "utf-8") + "\" target=\"_blank\">" + fileName + "</a>");
   	    
		}
           }
      }
   }

   if(params!=null && params.length > 0) {  

	out.println("<tr><td colspan=2><P><B>Input Parameters</b></td></tr>");  
      for(int j = 0; j < params.length; j++) {

         ParameterInfo parameterInfo = params[j];
System.out.println("Val=" + parameterInfo.getValue());

	  	if (!parameterInfo.isOutputFile()){	
			out.print("<tr><td align=right>");
			out.println(parameterInfo.getName());
			out.println("</td><td>");
			if (parameterInfo.isInputFile()) {
				File f = new File(parameterInfo.getValue());
				String axisName = f.getName();
				boolean fileExists = f.exists();
				boolean isURL=false;
System.out.println("Val=" + parameterInfo.getValue());
				if (fileExists){
					out.println("<a href=\"getInputFile.jsp?file="+StringUtils.htmlEncode(axisName)+  "\" target=\"_blank\" > ");
				} else {
					try {// see if a URL was passed in
						URL url = new URL(parameterInfo.getValue());
						out.println("<a href=\""+ parameterInfo.getValue()+"\" > ");				
						isURL = true;
					} catch (Exception e){
						e.printStackTrace();
					}

				}
				String name = axisName;						
				int idx = axisName.lastIndexOf("axis_");
				if (idx > 0) {
					name = axisName.substring(idx+5);
				}

				out.println(StringUtils.htmlEncode(name));
				if (fileExists || isURL)
					out.println("</a>");
			} else {
 				boolean isURL=false;

				try {// see if a URL was passed in
					URL url = new URL(parameterInfo.getValue());
					out.println("<a href=\""+ parameterInfo.getValue()+"\" > ");				
					isURL = true;
				} catch (Exception e){
					//e.printStackTrace();
				}

				out.println(StringUtils.htmlEncode(parameterInfo.getValue()));
			
				if (isURL)out.println("</a>");

			}
			out.println("  ");
			out.print("</td></tr>");
			
		}
	   }		
   }


out.println("<p>");
out.println("</table>");

%>