<!-- /*
  The Broad Institute
  SOFTWARE COPYRIGHT NOTICE AGREEMENT
  This software and its documentation are copyright (2003-2006) by the
  Broad Institute/Massachusetts Institute of Technology. All rights are
  reserved.

  This software is supplied without any warranty or guaranteed support
  whatsoever. Neither the Broad Institute nor MIT can be responsible for its
  use, misuse, or functionality.
*/ -->


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
		 org.genepattern.server.genepattern.GenePatternAnalysisTask,
		 org.genepattern.server.webservice.server.local.LocalTaskIntegratorClient , 
		 org.genepattern.webservice.TaskInfo,
		 org.genepattern.webservice.TaskInfoAttributes,
		 org.genepattern.webservice.ParameterFormatConverter,
		 org.genepattern.webservice.ParameterInfo,
		 org.genepattern.server.util.AccessManager,
		 org.genepattern.util.LSID,
		 java.net.*, 
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
ParameterInfo[] formalParamInfos = new ParameterInfo[0];
try {
      job = analysisClient.getJob((new Integer(jobId)).intValue());

System.out.println("\n\nJob=" + job);
	TaskInfo task = GenePatternAnalysisTask.getTaskInfo(job.getTaskLSID(), userID);
	formalParamInfos = task.getParameterInfoArray();
	

} catch(Exception wse) {
	wse.printStackTrace();

}
%>
<head>
<link href="skin/stylesheet.css" rel="stylesheet" type="text/css">
</head>
<body class="bodyNoMargin">	
<table  id='jobtable' frame=border xwidth='100%' class="paleBackground"  valign='top' >

<tr><td align='left' class="wideheading" colspan=3><span class="heading">Job <%=jobId%> <%= job.getTaskName()%></span></td></tr><tr>

<%
SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd HH:mm:ss");
SimpleDateFormat shortDateFormat = new SimpleDateFormat("HH:mm:ss");
Calendar midnight = Calendar.getInstance();
midnight.set(Calendar.HOUR_OF_DAY, 0);
midnight.set(Calendar.MINUTE, 0);
midnight.set(Calendar.SECOND, 0);
midnight.set(Calendar.MILLISECOND, 0);
String serverURL = "http://"+ InetAddress.getLocalHost().getCanonicalHostName() + ":"+ System.getProperty("GENEPATTERN_PORT") +"/"+ request.getContextPath();



boolean hasLog = false;
int outFileCount=1;
HashMap formalParamMap = new HashMap();

   job = analysisClient.getJob(job.getJobNumber());
   hasLog = false;
   StringBuffer buff2 = new StringBuffer();
   StringBuffer buff = new StringBuffer();
			
   out.print("<tr>");
   ParameterInfo[] params = job.getParameterInfoArray();
   JobInfo[] children = analysisClient.getChildren(job.getJobNumber());
			
  
   Date completed = job.getDateCompleted();
   DateFormat formatter =  completed.after(midnight.getTime()) ? shortDateFormat : dateFormat;
   
   out.print("<td> completed: " + formatter.format(completed)+"</td>");
   out.println("<tr><td colspan=2><P><B>output Files</b></td></tr>");

if (formalParamInfos!=null){  
   for (int k=0; k < formalParamInfos.length; k++){
		ParameterInfo  formalParam = formalParamInfos[k];
		formalParamMap.put(formalParam.getName(), formalParam);
   }
}

      
   if(params!=null && params.length > 0) {
     
	for(int j = 0; j < params.length; j++) {
         ParameterInfo parameterInfo = params[j];
 	   ParameterInfo formalParam = (ParameterInfo)formalParamMap.get(parameterInfo.getName());
         if(parameterInfo.isOutputFile()) {

         String value = parameterInfo.getUIValue(formalParam);
           int index = value.lastIndexOf(File.separator);
	     String altSeperator = "/";
	     if (index == -1) index = value.lastIndexOf(altSeperator);

           String jobNumber = value.substring(0, index);
           String fileName = value.substring(index + 1, value.length());
                 
	     if (!GPConstants.TASKLOG.equals(fileName)){ 
           		out.println("<tr><td valign='top' colspan=\"3\">");
           		out.println("<a href=\"retrieveResults.jsp?job=" + jobNumber + "&filename=" + URLEncoder.encode(fileName, "utf-8") + "\" target=\"_blank\">" + fileName + "</a></td></tr>");
   	    
		}
           }
      }
   }

// JTL 10/2/06 add files from the pipelines children
	if(children.length > 0) {
		//boolean isPipeline = true;
		int childNum = 0;
		for(int k = 0; k < children.length; k++) {
			List paramsList = getOutputParameters(children[k]);
			if(paramsList.size() > 0) {
				out.println("<tr align='left' id=" + childNum + "><td colspan=\"2\">");
				childNum++;
				out.println((k+1) + ". " + children[k].getTaskName());
out.println("</td></tr>");
				writeParameters(paramsList,  "&nbsp;&nbsp;&nbsp;&nbsp;", serverURL, out);
			}
		
		}
	}		
		

   if(params!=null && params.length > 0) {  

	out.println("<tr><td colspan=2><P><B>Input Parameters</b></td></tr>");  
      for(int j = 0; j < params.length; j++) {
  	 	
         	ParameterInfo parameterInfo = params[j];
   		ParameterInfo formalParam = (ParameterInfo)formalParamMap.get(parameterInfo.getName());
	

	  	if (!parameterInfo.isOutputFile()){	
			out.print("<tr><td align=right>");
			out.println(parameterInfo.getName());
			out.println("</td><td>");
			if (parameterInfo.isInputFile()) {
				File f = new File(parameterInfo.getValue());
				String axisName = f.getName();

System.out.println("\tPI=" + parameterInfo.getName()+" "+f.getCanonicalPath());

				boolean fileExists = f.exists();
				boolean isURL=false;
				if (fileExists){
					out.println("<a href=\"getInputFile.jsp?file="+StringUtils.htmlEncode(axisName)+  "\" target=\"_blank\" > ");
				} else {
					try {// see if a URL was passed in
System.out.println("\tPI=" + parameterInfo.getName()+" "+parameterInfo.getValue());
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
					URL url = new URL(parameterInfo.getUIValue(formalParam));
					out.println("<a href=\""+ parameterInfo.getValue()+"\" > ");				
					isURL = true;
				} catch (Exception e){
					//e.printStackTrace();
				}

				out.println(StringUtils.htmlEncode(parameterInfo.getUIValue(formalParam)));
			
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
<script language="Javascript">
var item = document.getElementById(['jobtable']);
//alert("width=" + item.offsetWidth + ", height=" + item.offsetHeight);

iWidth = document.body.clientWidth;
iHeight =document.body.clientHeight;

iWidth = 20+item.offsetWidth - iWidth;
iHeight = 10+item.offsetHeight - iHeight;
window.resizeBy(iWidth, iHeight);
</script> 
</body>
<%! 

public void writeParameters(List params, String prefix, String serverURL, JspWriter out) throws 	java.io.IOException {
		
for(int i = 0; i < params.size(); i++) {
	ParameterInfo parameterInfo = (ParameterInfo) params.get(i);
	String value = parameterInfo.getValue();
 	int index = value.lastIndexOf(File.separator);
	String altSeperator = "/";

	if (index == -1) index = value.lastIndexOf(altSeperator);

    	String jobNumber = value.substring(0, index);
      String fileName = value.substring(index + 1, value.length());
    	      
      out.println("<tr><td colspan=\"3\">");

	String fileURL = "retrieveResults.jsp?job=" + jobNumber + "&filename=" + URLEncoder.encode(fileName, "utf-8");
      out.println(prefix + "<a href=\""+ fileURL+"\">" + fileName + "</a>");
 
	out.println("</td></tr>");

	}	
}
	
	public List getOutputParameters(JobInfo job)  {
		ParameterInfo[] params = job.getParameterInfoArray();
		List paramsList = new ArrayList();
		if(params!=null && params.length > 0) {
  
			for(int j = 0; j < params.length; j++) {
				ParameterInfo parameterInfo = params[j];

				if(parameterInfo.isOutputFile()) {
					paramsList.add(parameterInfo);
				}
			}
		}
		return paramsList;
	}
%>