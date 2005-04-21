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
		 org.genepattern.server.genepattern.GenePatternAnalysisTask,
		 org.genepattern.util.LSID,
		 org.genepattern.util.GPConstants,
		 org.genepattern.webservice.OmnigeneException, 
		 org.genepattern.data.pipeline.PipelineModel"
	session="false" contentType="text/html" language="Java" %><%

response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
response.setHeader("Pragma", "no-cache");		 // HTTP 1.0 cache control
response.setDateHeader("Expires", 0);

String taskName = request.getParameter(GPConstants.NAME);
if (taskName == null || taskName.length() == 0) {
%>
	<html>
	<head>
	<link href="stylesheet.css" rel="stylesheet" type="text/css">
	<link rel="SHORTCUT ICON" href="favicon.ico" >
	<title>run GenePattern task</title>
	</head>
	<body>	
	<jsp:include page="navbar.jsp"></jsp:include>
	Must specify task name.<br>
	<jsp:include page="footer.jsp"></jsp:include>
	</body>
	</html>
<%
	return;
}
String username = request.getParameter(GPConstants.USERID);
if (username == null || username.length() == 0) {
	username = GenePatternAnalysisTask.getUserID(request, response);
}
boolean bNoEnvelope = (request.getParameter("noEnvelope") != null);

TaskInfo taskInfo = null;
try { 
	
taskInfo = GenePatternAnalysisTask.getTaskInfo(taskName, username); } catch (OmnigeneException oe) {}
TaskInfoAttributes tia = taskInfo.giveTaskInfoAttributes();
ParameterInfo[] parameterInfoArray = null;
try {
        parameterInfoArray = new ParameterFormatConverter().getParameterInfoArray(taskInfo.getParameterInfo());
	if (parameterInfoArray == null) parameterInfoArray = new ParameterInfo[0];
} catch (OmnigeneException oe) {
}


String taskType = tia.get("taskType");
boolean isVisualizer = "visualizer".equalsIgnoreCase(taskType);
boolean isPipeline = "pipeline".equalsIgnoreCase(taskType);

String formAction = "runTaskPipeline.jsp";
if (isVisualizer){
	formAction = "preRunVisualizer.jsp";
} else if (isPipeline){
	formAction = "runPromptingPipeline.jsp";
	int numParams = parameterInfoArray.length;
	if (numParams == 0){
try{
		RequestDispatcher rd = request.getRequestDispatcher("runPipeline.jsp");
		rd.forward(request, response);
		return;
} catch (Exception e){
	e.printStackTrace();
}

	}
	
}


if (!bNoEnvelope) { %>
	<html>
	<head>
	<link href="stylesheet.css" rel="stylesheet" type="text/css">
	<link rel="SHORTCUT ICON" href="favicon.ico" >
	<title>run <%= taskInfo != null ? taskInfo.getName() : "GenePattern task" %></title>
<style>
.heading { font-family: Arial,Helvetica,sans-serif; background-color: #0E0166; color: white; font-size: 12pt; font-weight: 800; text-align: center; }
.majorCell { border-width: 2; font-size: 10pt; }
.button  { width: 50; }
.wideButton  { width: 100; }
.wideBoldButton  { width: 100; font-weight: bold; color: red }
td { padding-left: 5; }
</style>
	</head>
	<body>	
	<jsp:include page="navbar.jsp"></jsp:include>
<% } %>

<%
if (taskInfo == null) {
%>
	<script language="javascript">
	alert('No such task <%= taskName %>');
	</script>
	No such task <%= taskName %><br>
	<% if (!bNoEnvelope) { %>
		<jsp:include page="footer.jsp"></jsp:include>
		</body>
		</html>
	<% } %>
<%
	return;
}
taskName = taskInfo.getName();


//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX

%>
<script language="JavaScript">

var logFileContents = new Array(); 

function showJob(job) {
	execLogArea = document.execLogForm.execLogArea;	
	execLogArea.value = logFileContents[job];
}


</script>


<table   width='80%' cols=2  cellpadding=20 >
<col align="left" valign='top' width="45%"><col align="left" width="*">

<tr><td  valign='top' height='100%'>

<table class="majorCell"  frame=border width='100%' height='100%' bgcolor='#EFEFFF'  valign='top'>

<tr><td class="heading" colspan=3><span class="heading">Recent Jobs</span></td></tr><tr>

<%
String userID = GenePatternAnalysisTask.getUserID(request, null); // get userID but don't force login if not defined

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

int numRowsToDisplay = 15; 
int rowsDisplayed = 0; // increment for each <tr> in this table

//// GET THE EXECUTION LOG FOR WRITING TO THE TEXTAREA
for(int i = 0; i < jobs.length; i++) {
   JobInfo job = jobs[i];
  
   if(!job.getStatus().equals(JobStatus.FINISHED) ) continue;
  
   out.print("<tr><td align=\"right\" >" + job.getJobNumber() + "");
   rowsDisplayed++;
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
			StringBuffer buff = new StringBuffer();
			System.out.println("VAL=" + value);
			File logFile = new File("temp/"+value);
			BufferedReader reader = new BufferedReader(new FileReader(logFile));
			String line = null;
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
		}
			
	   }
	}
   }
  // END OF GETTING THE EXECUTION LOG
  out.println("<script language='javascript'>");

  out.println("logFileContents["+job.getJobNumber()+"]='" + log+ "';");

  out.println("</script>");


   out.print("<td valign='center'><span name='"+job.getJobNumber()+"'onmouseover='showJob("+job.getJobNumber()+")'><nobr>" + job.getTaskName()+"&nbsp;<img src='info_obj.gif'></nobr></span>");
   
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
           		out.println("<a href=\"retrieveResults.jsp?job=" + jobNumber + "&filename=" + URLEncoder.encode(fileName, "utf-8") + "\">" + fileName + "</a>");
   	     		rowsDisplayed++;
		}
           }
      }
   }

// System.out
   if (rowsDisplayed >= numRowsToDisplay) break;
}
out.println("</td></tr><tr><td colspan=3><form name='execLogForm'><TEXTAREA name='execLogArea' style=\"font-size:9px;font-family: arial, helvetica, sans-serif;width: 100%;\" rows='5'  readonly wrap='soft' bgcolor='#EFEFFF'></textarea></form></td></tr>");


out.println("</table>");




%>
</td>
<td valign='top' align='left'>
<%
	int veridx = ((String)tia.get(GPConstants.LSID)).lastIndexOf(":");
	String taskLsidVersion = ((String)tia.get(GPConstants.LSID)).substring(veridx+1);

%>

<table width="100%" cols="2" >
<tr><td><b><font size="+1"><%= taskName %></font></b> version <%= taskLsidVersion%></td>
<%
if (taskName != null) {
	LocalTaskIntegratorClient taskIntegratorClient = new LocalTaskIntegratorClient(username);

	File[] docFiles = taskIntegratorClient.getDocFiles(taskInfo);
	boolean hasDoc = docFiles != null && docFiles.length > 0;
	if (hasDoc) {
		%><td align="right"><b>Documentation:</b><%
 		for (int i = 0; i < docFiles.length; i++) { %>
			<a href="getTaskDoc.jsp?<%= GPConstants.NAME %>=<%= GenePatternAnalysisTask.htmlEncode(request.getParameter(GPConstants.NAME)) %>&file=<%= URLEncoder.encode(docFiles[i].getName()) %>" target="new"><%= GenePatternAnalysisTask.htmlEncode(docFiles[i].getName()) %></a><% 
 		} 
		%></td></tr><%
 	}
}

// XXXXXXXXXXXXXXXXXXXXXXXXX

%>
</table>


	<form name="pipeline" action="<%=formAction%>" method="post" ENCTYPE="multipart/form-data">
	<input type="hidden" name="taskName" value="<%= taskName %>">
	<input type="hidden" name="taskLSID" value="<%= tia.get(GPConstants.LSID) %>">
	<input type="hidden" name="<%= GPConstants.USERID %>" value="<%= username %>">
	<input type="hidden" name="taskName" value="<%= taskName %>">

	<table  valign="top"  >
	

<%	
	int numParams = parameterInfoArray.length;

	if (numParams > 0) { %>
		<tr><td align='left' colspan='2'><b>&nbsp;&nbsp;</b></td></tr>
<%	} else { %>
		<tr><td align='left' colspan='2'><i>has no input parameters</i></td></tr>
<%	} 
 	

	String prefix = "";
	for (int param = 0; param < parameterInfoArray.length; param++) {
		out.flush();
		ParameterInfo pi = parameterInfoArray[param];
		HashMap pia = pi.getAttributes();
		String[] choices = null;
		String[] stChoices = pi.getChoices(GPConstants.PARAM_INFO_CHOICE_DELIMITER);
		String val = pi.getValue();
		boolean isOptional = ((String)pia.get(GPConstants.PARAM_INFO_OPTIONAL[GPConstants.PARAM_INFO_NAME_OFFSET])).length() > 0;
		String defaultValue = (request.getParameter(pi.getName()) != null ? request.getParameter(pi.getName()) : (String)pia.get(GPConstants.PARAM_INFO_DEFAULT_VALUE[0]));
		if (defaultValue != null) defaultValue = defaultValue.trim();
		String description = pi.getDescription();
%>

		<tr>
     		<td align="right"  valign="top"><nobr><%= !isOptional ? "<b>" : "" %><%= pi.getName().replace('.',' ') %>:<%= !isOptional ? "<span style=\"font-size: medium;\"> *</span></b>" : "" %></nobr></td>
		<td valign="top" align='left'>

<% 		if (pi.isInputFile()) { %>

			<input	type="file" 
				name="<%= pi.getName() %>" 
				size="60" 
				onchange="this.form.shadow<%= param %>.value=this.value;" 
				onblur="javascript:if (this.value.length > 0) { this.form.shadow<%= param %>.value=this.value; }" 
				ondrop="this.form.shadow<%= param %>.value=this.value;" 
				class="little">
			<br>
			<input name="shadow<%= param %>" 
			 	   type="text" 
				   value="<%= defaultValue == null ? "" : defaultValue %>"
				   readonly 
				   size="90" 
				   tabindex="-1" 
				   class="shadow" 
				   style="{ border-style: none; font-style: italic; font-size=9pt; background-color: transparent }">

<%
			if (description.length() > 0 && !description.equals(pi.getName().replace('.',' '))) {
				out.println("<br>" + GenePatternAnalysisTask.htmlEncode(description));
			}
		} else if (pi.isOutputFile()) {
		} else if (stChoices.length < 2) { %>
			<table align="left"><tr><td valign="top">
			<input name="<%= pi.getName() %>" value="<%=  defaultValue %>">
			</td><%
			if (description.length() > 0) { %>
				<td valign="top"><%= GenePatternAnalysisTask.htmlEncode(description) %></td>
			<% } %>
			</tr></table>
<%		} else { %>
			<table align="left"><tr><td valign="top">
			<select name="<%= pi.getName() %>">
<%
			String display = null;
			String option = null;
			String choice;
			for (int iChoice = 0; iChoice < stChoices.length; iChoice++) {
				choice = stChoices[iChoice];
				int c = choice.indexOf(GPConstants.PARAM_INFO_TYPE_SEPARATOR);
				if (c == -1) {
					display = choice;
					option = choice;
				} else {
					option = choice.substring(0, c);
					display = choice.substring(c+1);
				}
				display = display.trim();
				option = option.trim();
%>			<option value="<%= option %>"<%= defaultValue.equals(option) || defaultValue.equals(display) ? " selected" : "" %>><%= display %></option>
<%			} %>
			</select>
			</td>
			<td valign="top"><%= GenePatternAnalysisTask.htmlEncode(description) %></td>
			</tr></table>
<%
		}
		out.println("</td></tr>");
	}
%>
	<tr>	
		<td></td>
		<td><input type="submit" name="cmd" value="run"> <input type="button" value="help" onclick="window.open('getTaskDoc.jsp?<%= GPConstants.NAME %>=<%= taskName %>', '_new')"></td>
	</tr>

	</table>
	</form>

	

<% if (!bNoEnvelope) { %>
	</td></tr></table>
      <jsp:include page="footer.jsp"></jsp:include>
	</body>
	</html>
<% } %>