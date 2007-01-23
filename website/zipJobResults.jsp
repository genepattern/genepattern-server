<%-- /*
  The Broad Institute
  SOFTWARE COPYRIGHT NOTICE AGREEMENT
  This software and its documentation are copyright (2003-2006) by the
  Broad Institute/Massachusetts Institute of Technology. All rights are
  reserved.

  This software is supplied without any warranty or guaranteed support
  whatsoever. Neither the Broad Institute nor MIT can be responsible for its
  use, misuse, or functionality.
*/ --%><%@ page import="java.io.File,
		 java.util.zip.*,
		 java.io.*,
	       java.text.*,
	       java.util.*,
	       java.net.*,
	 	 org.genepattern.server.util.AuthorizationManagerFactoryImpl,
		 org.genepattern.server.util.IAuthorizationManager,
		 org.genepattern.util.GPConstants,
		 org.genepattern.webservice.JobInfo,
 		 org.genepattern.util.StringUtils,
		 org.genepattern.webservice.JobStatus,
		 org.genepattern.webservice.ParameterInfo,
		 org.genepattern.webservice.WebServiceException,
	       org.genepattern.server.webservice.server.local.*,
	       org.genepattern.server.util.AccessManager,
		 org.genepattern.server.genepattern.GenePatternAnalysisTask"
	session="false" contentType="text/html" language="Java" %><% 

	/*********************************************************
	 NOTE THAT THIS PAGE CANNOT BE MARKED NON-CACHEABLE.
	 Bug 555 attempted to do this, and bug 600 was the result.
	 *********************************************************/


String STOP = "stop";
String SHOW_ALL = "showAll";
   
String jobID = request.getParameter("jobID"); // for deleting or downloading 
String[] attachmentNames = request.getParameterValues("dl"); // for deleting or downloading

String SHOW_LOGS = "showLogs";
boolean showLogs = (request.getParameter(SHOW_LOGS) != null);

boolean showAll = (request.getParameter(SHOW_ALL) != null);
boolean isDelete = (request.getParameter("delete") != null);
boolean isDownload = (request.getParameter("download") != null);
String[] deleteJob = request.getParameterValues("deleteJob");
String stopTaskID = request.getParameter(STOP);
String userID = (String)request.getAttribute("userID"); // get userID but don't force login if not defined
String serverURL = "http://"+ InetAddress.getLocalHost().getCanonicalHostName() + ":"+ System.getProperty("GENEPATTERN_PORT") +"/"+ request.getContextPath();

if(isDownload) {
	ZipOutputStream zos = null;
   File zipFile = new File(System.getProperty("java.io.tmpdir"), jobID + ".zip");
   zipFile.delete();
   try {
   	zos = new ZipOutputStream(new FileOutputStream(zipFile));
      byte[] buf = new byte[100000];
      String jobDir = System.getProperty("jobs");

	  if ((attachmentNames == null) && (jobID != null)) {
			// if no filenames passed in, we get all files from the job
 			LocalAnalysisClient client = new LocalAnalysisClient(userID);
            JobInfo jobInfo = client.checkStatus(Integer.parseInt(jobID));
			JobInfo[] children = client.getChildren(jobInfo.getJobNumber());

			ParameterInfo[] params = jobInfo.getParameterInfoArray();
			ArrayList<String> fileList = new ArrayList<String>();
			if (params != null) {
				for (int p = 0; p < params.length; p++) {
					if (params[p].isOutputFile()) {
						fileList.add((String)params[p].getValue());
					}
				}
			}
			for (int ji = 0; ji < children.length; ji++){
				JobInfo childJob = children[ji];
				params = childJob.getParameterInfoArray();
				if (params != null) {
					for (int p = 0; p < params.length; p++) {
						if (params[p].isOutputFile()) {
							fileList.add((String)params[p].getValue());
						}
					}
				}
			}

			attachmentNames = fileList.toArray(new String[fileList.size()]);
		}
	  


		for(int i = 0; i < attachmentNames.length; i++) {
			String value = attachmentNames[i];
         int index = value.lastIndexOf("=");
	 		value = value.substring(index+1);
         index = value.lastIndexOf("/");
         if (index == -1) index = value.lastIndexOf("\\");

         String jobNumber = value.substring(0, index);
         String fileName = value.substring(index + 1, value.length());
         
         try {
            fileName = URLDecoder.decode(fileName, "UTF-8");
         } catch(UnsupportedEncodingException uee) {
            // ignore
         }
         File attachment = new File(jobDir + File.separator + value);
         ZipEntry zipEntry = new ZipEntry((jobNumber.equals(jobID) ? "" : (jobNumber + "/")) + fileName);
         zipEntry.setTime(attachment.lastModified());
         zipEntry.setSize(attachment.length());
         zos.putNextEntry(zipEntry);
         FileInputStream is = null;
         try {
            is = new FileInputStream(attachment);
            int n;
            while((n = is.read(buf, 0, buf.length)) > 0) {
               zos.write(buf, 0, n);
            }
         } finally {
            if(is!=null) {
               is.close();
            }
         }
         zos.closeEntry();          
       }
     } catch(IOException ioe) {
		ioe.printStackTrace();
     } finally {
         if(zos!=null) {
            try {
               zos.finish();
               zos.close();

	String contentType = "application/x-zip-compressed" + "; name=\"" + zipFile.getName()+ "\";";
	response.addHeader("Content-Disposition", "attachment; filename=\"" + zipFile.getName() + "\";");
	response.setContentType(contentType);
	response.setHeader("Content-Length", "" + zipFile.length());
        BufferedInputStream ins = new BufferedInputStream(new java.io.FileInputStream(zipFile));
	int c = 0;
  	while ((c = ins.read()) != -1) {
   		out.write(c);
  	}
	ins.close();
	ins = null;




               zipFile.delete();
            } catch(IOException x){}
         }
     }
     return;
} // isDownload

if (isDelete || !isDownload) {
   response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
   response.setHeader("Pragma", "no-cache");		 // HTTP 1.0 cache control
   response.setDateHeader("Expires", 0);
%>
   <html>
   <head>
   <link href="skin/stylesheet.css" rel="stylesheet" type="text/css">
   <link href="skin/favicon.ico" rel="shortcut icon">
   <meta http-equiv="content-type" content="text/html; charset=ISO-8859-1">
   <script language="javascript">


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


	function checkAll(bChecked) {
		for (f = 0; f < document.forms.length; f++) {
			var frm = document.forms[f];
			for (i = 0; i < frm.elements.length; i++) {
				if (frm.elements[i].type != "checkbox") continue;
				if (frm.elements[i].name != "deleteJobID") continue;
				frm.elements[i].checked = bChecked;
			}
		}
	}

	function deleteJobs() {
		// iterate through all forms in document
		// for each form, iterate over all controls
		// if controlName==deleteJobID && checked
		// add to list of jobs to delete

		var deleteIDs = "";
		var url = "";
		var numDeleted = 0;
		for (f = 0; f < document.forms.length; f++) {
			var frm = document.forms[f];
			for (i = 0; i < frm.elements.length; i++) {
				if (frm.elements[i].type != "checkbox") continue;
				if (frm.elements[i].name != "deleteJobID") continue;
				if (frm.elements[i].checked) {
					if (numDeleted > 0) {
						url = url + "&";
						deleteIDs = deleteIDs + ", ";
					}
					url = url + "deleteJob=" + frm.elements[i].value;
					deleteIDs = deleteIDs + frm.elements[i].value;
					numDeleted++;
				}
			}
		}

		// confirm deletion
		if (numDeleted > 0 && window.confirm('Really delete job' + (numDeleted > 1 ? 's' : '') + ' ' + deleteIDs + '?')) {
			// call self with URL to delete all selected jobs
			window.location = "zipJobResults.jsp?" + url;
		}
	}

	function toggleLogs() {
		var visible = document.form1.showLogs.checked;
			
		var elements = document.getElementsByTagName("div")
		for (i=0; i < elements.length; i++){
			var objId = elements[i].id
			if (objId == 'log'){
				if (visible) {
		  			elements[i].style.display = "block";
				} else {
					elements[i].style.display = "none";
				}
			}
		}
	}

   </script>
<%
}

if(deleteJob!=null && deleteJob.length > 0) {
    for (int j = 0; j < deleteJob.length; j++) {
	    try {
	      LocalAnalysisClient analysisClient = new LocalAnalysisClient(userID);
	      analysisClient.deleteJob(Integer.parseInt(deleteJob[j]));
	    } catch(Exception e){}
    }
}

if (isDelete) {
   LocalAnalysisClient analysisClient = new LocalAnalysisClient(userID);
   if (attachmentNames != null) {
      int _jobId = Integer.parseInt(jobID);

      for (int j = 0; j < attachmentNames.length; j++) {
         String value = attachmentNames[j];
         try {
            analysisClient.deleteJobResultFile(_jobId, value);
          } catch(WebServiceException wse) {
wse.printStackTrace();

		}
      }
   }
   response.sendRedirect("pages/index.jsf");
}

LocalAnalysisClient analysisClient = new LocalAnalysisClient(userID);
if (stopTaskID != null) {
   try {
      analysisClient.terminateJob(Integer.parseInt(stopTaskID));
   } catch(WebServiceException wse) {
	wse.printStackTrace();
   }
}
 

SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd HH:mm:ss");
SimpleDateFormat shortDateFormat = new SimpleDateFormat("HH:mm:ss");
Calendar midnight = Calendar.getInstance();
midnight.set(Calendar.HOUR_OF_DAY, 0);
midnight.set(Calendar.MINUTE, 0);
midnight.set(Calendar.SECOND, 0);
midnight.set(Calendar.MILLISECOND, 0);

JobInfo[] jobs = null;
try {
   if(showAll) {
      jobs = analysisClient.getJobs(null, -1, Integer.MAX_VALUE, false);
   } else {
      jobs = analysisClient.getJobs(userID, -1, Integer.MAX_VALUE, false);
   }
} catch(WebServiceException wse) {
	wse.printStackTrace();
}

DecimalFormat df = new DecimalFormat();

SimpleDateFormat fmt = null;
Date d = null;
Hashtable htColors = new Hashtable();
htColors.put(JobStatus.NOT_STARTED, "red");
htColors.put(JobStatus.PROCESSING, "blue");
htColors.put(JobStatus.FINISHED, "green");
htColors.put(JobStatus.ERROR, "red");



if (!isDelete) {
%>
   <title>job results</title>
   <script language="javascript">
   function toggle(start, end) {
		

		for(i = start; i < end; i++) {
			el = document.getElementById(i);
			if(el.style.display=="none") {
				el.style.display = '';
			} else {
				el.style.display = "none";
			}
		}
	}
	
	</script>
    <jsp:include page="navbarHead.jsp"/>
   </head>
   <body>
   <jsp:include page="navbar.jsp"/>
<% } %>


<form  name='form1'>
<input type="checkbox" name="<%= SHOW_ALL %>" <%= showAll ? "checked" : "" %> value="<%= SHOW_ALL %>"
onclick="javascript:window.location='zipJobResults.jsp<%= showAll ? "" : ("?" + SHOW_ALL + "=1") %>'">show everyone's jobs
<input type="checkbox" name="<%= SHOW_LOGS %>" <%= showLogs ? "checked" : "" %> value="<%= SHOW_LOGS %>"
onclick="toggleLogs()">show execution logs


</form>

<table cellspacing="4">
<tr>
   <th valign="bottom" align="right"><b><u>job &nabla;</u></b></th>
   <th valign="bottom"><b><u>task</u></b></th>
   <th valign="bottom"><b><u>submitted</u></b></th>
   <th valign="bottom"><b><u>completed</u></b></th>
   <th valign="bottom"><b><u>status</u></b></th>
   <th valign="bottom">
	<form>
		<a href="#" onclick="checkAll(true)">check all</a> &nbsp;&nbsp;
		<a href="#" onclick="checkAll(false)">uncheck all</a><br>
		<input type="button" value="delete checked jobs" onclick="deleteJobs()">
	</form>
   </th>
</tr>
<%

if(jobs.length==0) {
   out.println("<p>No jobs to display");
}
int id = 0;
for(int i = 0; i < jobs.length; i++) {
   JobInfo job = jobs[i];
   boolean myJob = userID.equals(job.getUserId());

   out.print("<tr><td align=\"right\"><a href=\"getJobResults.jsp?jobID=" + job.getJobNumber() + "\">" + job.getJobNumber() + "</a>");
     
   out.println("<td><span onClick='showJob(" + job.getJobNumber() + ")'>" + job.getTaskName() + "&nbsp;<img src='skin/info_obj.gif'>");
   
   Date submitted = job.getDateSubmitted();
   DateFormat formatter = submitted.after(midnight.getTime()) ? shortDateFormat : dateFormat;
   
   out.print("<td>" + formatter.format(submitted));
   Date completed = job.getDateCompleted();
   String status = job.getStatus();
   
   if(completed!=null && !status.equals(JobStatus.PROCESSING)) {
   	formatter = completed.after(midnight.getTime()) ? shortDateFormat : 	dateFormat;
   
   	out.print("<td>" + formatter.format(completed));
   } else {
   	out.print("<td></td>");
   }
   
   
   if(status.equals(JobStatus.PROCESSING)) {
	out.print("<td><font color=" + htColors.get(status)  +">" + status + 
		"</font> - <a href=\"zipJobResults.jsp?" + STOP + "=" + job.getJobNumber() + 
		(showAll ? ("&" + SHOW_ALL + "=1") : "") + "\">stop</a></td>" +
		"<td><form><input type=\"checkbox\" name=\"deleteJobID\" value=\"" + job.getJobNumber() + "\"></form>");
   } else if(!status.equals(JobStatus.NOT_STARTED)) {
	//out.print("<td><font color=" + htColors.get(status)  +">" + status + "</font></td><td><form><input type=\"checkbox\" name=\"deleteJobID\" value=\"" + job.getJobNumber() + "\"></form>");

out.print("<td><font color=" + htColors.get(status)  +">" + status + "</font></td>");
		if (myJob){ 
			out.print("<td><form><input type=\"checkbox\" name=\"deleteJobID\" value=\"" + job.getJobNumber() + "\"></form>");
		} else {
			out.print("<td>&nbsp;");
		}

   } else {
		out.print("<td><font color=" + htColors.get(status)  +">" + status);
		if (status.equals(JobStatus.FINISHED)) {
			out.print("</td><td><form><input type=\"checkbox\" name=\"deleteJobID\" value=\"" + job.getJobNumber() + "\"></form>");	}
  	 	}
		ParameterInfo[] params = job.getParameterInfoArray();
		JobInfo[] children = analysisClient.getChildren(job.getJobNumber());
		out.println("<form method=\"post\">");
		out.println("<input type=\"hidden\" name=\"jobID\" value=\"" + job.getJobNumber() + "\"/>");
		boolean hasOutputFiles = false;
		boolean isPipeline = false;
		if (showAll) {
			out.println("<input type=\"hidden\" name=\"" + SHOW_ALL + "\" value=\"1\"/>");
		}
		int startId = id;
		if(children.length > 0) {
			isPipeline = true;
			
			for(int k = 0; k < children.length; k++) {
				List paramsList = getOutputParameters(children[k]);
				if(paramsList.size() > 0) {
					hasOutputFiles = true;
					String userId = StringUtils.htmlEncode(" " + job.getUserId());
					out.println("<tr id=" + id + "><td></td><td colspan=\"4\">");
					id++;
					out.println((k+1) + ". " + children[k].getTaskName());
					writeParameters(userId, paramsList, showAll, "&nbsp;&nbsp;&nbsp;&nbsp;", serverURL, out);
				}
				
			}
			
		} else {
			List paramsList = getOutputParameters(job);
			if(paramsList.size() > 0) {
				hasOutputFiles = true;
				String userId = StringUtils.htmlEncode(" " + job.getUserId());
				writeParameters(userId, paramsList, showAll, "", serverURL, out);
			}
		}   	
   	if(hasOutputFiles) {
			out.println("<tr><td></td><td colspan=\"4\">");
			out.println("<input type=\"submit\" name=\"delete\" value=\"delete\" class=\"little\"");
			if (!myJob) {
				out.println("disabled=\"true\" ");
			}
			out.println("><input type=\"submit\" name=\"download\" value=\"download\" class=\"little\">");
			if(isPipeline) {
			//	out.print("<input type=\"checkbox\" onClick=toggle('" + job.getJobNumber() + "');>");
					out.print("<input type=\"checkbox\" checked onClick=\"toggle(" + startId +"," + id + ");\">");
				out.println("Show Pipeline Steps</input>");
			}
		}
		out.println("</form>");	
	
}

out.println("</table>");
out.println("<br>");

%>
 <jsp:include page="footer.jsp"/>
</body>
</html>
<%! 

	public void writeParameters(String encodedUserId, List params, boolean showAll, String prefix, String serverURL, JspWriter out) throws java.io.IOException {

		IAuthorizationManager authorizationManager = (new AuthorizationManagerFactoryImpl()).getAuthorizationManager();


		for(int i = 0; i < params.size(); i++) {
			ParameterInfo parameterInfo = (ParameterInfo) params.get(i);
			String value = parameterInfo.getValue();
           	int index = value.lastIndexOf(File.separator);
	     	String altSeperator = "/";
	        	if (index == -1) index = value.lastIndexOf(altSeperator);

           	String jobNumber = value.substring(0, index);
           	String fileName = value.substring(index + 1, value.length());
            boolean isLog = ((fileName.indexOf(GPConstants.TASKLOG)) >= 0);
		      
           	out.println("<tr><td></td><td colspan=\"4\">");
		if (isLog){
			out.println("<div id=\"log\" style=\"display:none;\">");
      	} 

		String fileURL = "jobResults/" + jobNumber + "/" + URLEncoder.encode(fileName, "utf-8");
           	out.println(prefix + "<input type=\"checkbox\" name=\"dl\" value=\"" + value + "\" checked><a href=\""+ fileURL+"\">" + fileName + "</a>");
   		
		if (authorizationManager.checkPermission("createPipeline", encodedUserId)){

			out.print("<span  onClick=\"createPipeline(\'"+URLEncoder.encode(serverURL + fileURL, "utf-8")+"\')\" \" ><nobr>" );
   			out.print("&nbsp;");
    			out.print("<img src=skin/pipe_obj.jpeg>");
    			out.print( "  </nobr></span>");
		}


		if(showAll) {
			out.println(encodedUserId); 
			}

		if (isLog){
			out.println("</div>");
      	} 

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