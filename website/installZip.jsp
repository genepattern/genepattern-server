<%@ page import="java.io.File,
		 java.io.IOException,
		 java.util.Properties,
		 java.util.Vector,
		 java.util.Enumeration,
		 org.genepattern.server.analysis.genepattern.GenePatternAnalysisTask,
		 org.genepattern.util.GPConstants,
		 org.genepattern.server.analysis.webservice.server.*,
		 org.genepattern.server.analysis.*,
		 org.genepattern.analysis.*,
		 org.genepattern.server.analysis.handler.*,
		 org.genepattern.server.webservice.*,
		 com.jspsmart.upload.*,
		 org.genepattern.util.LSID,
		 org.genepattern.server.analysis.webservice.server.local.*"
	session="false" contentType="text/html" language="Java" %>
<%
	response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
	response.setHeader("Pragma", "no-cache");		 // HTTP 1.0 cache control
	response.setDateHeader("Expires", 0);
%>
<jsp:useBean id="mySmartUpload" scope="page" class="com.jspsmart.upload.SmartUpload" />
<html>
<head>
<link href="stylesheet.css" rel="stylesheet" type="text/css">
<link rel="SHORTCUT ICON" href="favicon.ico" >
<title>install GenePattern task from zip file</title>
</head>
<body>	
<%
int i;
String taskName = null;
File attachment = null;
String attachmentName = null;
String fullName = null;
Vector vProblems = null;
String url = null;
String attachmentDir;
String filename = null;
String lsid = null;
com.jspsmart.upload.Request requestParameters = null;
boolean isEncodedPost = true;
int fileCount = 0;
com.jspsmart.upload.File attachedFile = null;
String username = GenePatternAnalysisTask.getUserID(request, response);
if (username == null || username.length() == 0) return; // come back after login
// TODO: get values for access_id from task_access table in database

// mySmartUpload is from http://www.jspsmart.com/
LocalTaskIntegratorClient taskIntegratorClient = new LocalTaskIntegratorClient(username);

mySmartUpload.initialize(pageContext);
try {
	mySmartUpload.upload();
	requestParameters = mySmartUpload.getRequest();
	url = requestParameters.getParameter("url");
	isEncodedPost = (url == null);
} catch (NegativeArraySizeException nase) {
	// just means that this wasn't a form-encoded post
	isEncodedPost = false;
	url = request.getParameter("url"); // if this was not a form-encoded post
} catch (Exception e) {
	System.err.println(e + " during mySmartUpload.upload call in installZip.jsp");
}

if (url != null && (url.equals("http://") || url.length() == 0)) url = null;
if (isEncodedPost) {
	for (i=0;i<mySmartUpload.getFiles().getCount();i++) {
		attachedFile = mySmartUpload.getFiles().getFile(i);
		if (!attachedFile.isMissing()) fileCount++;
	}
}

if (fileCount == 0 && url == null) { %>
	<jsp:include page="navbar.jsp"></jsp:include>
	Please select a zip file for upload containing GenePattern manifest and optional support files<br>
	<a href="addZip.jsp">back</a><br>
	<jsp:include page="footer.jsp"></jsp:include>
	</body>
	</html>
<%
	return;
}
if (isEncodedPost) {
	for (i=0;i<mySmartUpload.getFiles().getCount();i++) {
		attachedFile = mySmartUpload.getFiles().getFile(i);
		if (attachedFile.isMissing()) continue;

		try {
			attachmentName = attachedFile.getFileName();
			if (attachmentName.trim().length() == 0) continue;

			attachment = new File(System.getProperty("java.io.tmpdir"), attachmentName);
			fullName = attachment.toString();
			attachedFile.saveAs(fullName);
			try {
				taskName = attachmentName;
				String privacy = requestParameters.getParameter(GPConstants.PRIVACY);
				int access_id = (requestParameters.getParameter(GPConstants.PRIVACY) != null ? GPConstants.ACCESS_PRIVATE : GPConstants.ACCESS_PUBLIC);
						
				try {
					String fileURL = attachment.toURI().toURL().toString();
					boolean askedRecursive = (requestParameters.getParameter("askedRecursive") != null);
					boolean doRecursive = (requestParameters.getParameter("inclDependents") != null);
					if (!askedRecursive &&
		 			    taskIntegratorClient.isZipOfZips(fileURL)) {
 						// see if user wants to install recursively or just the first entry
						Vector vTaskInfos = GenePatternAnalysisTask.getZipOfZipsTaskInfos(attachment);
						Enumeration vTasks = vTaskInfos.elements();
%>
		                                <jsp:include page="navbar.jsp"></jsp:include>
                	                	<form>
	                        	        <input type="checkbox" name="inclDependents" checked> include all tasks used within <%= attachmentName %>?
        	                        	<input type="hidden" name="url" value="<%= fileURL %>">
						<input type="hidden" name="askedRecursive" value="true">
                	                	<input type="submit" name="submit" value="install">
						<br><br>
						<table cols="2">
						<tr><td valign="top" align="right"><%= attachmentName %> contents:</td>
						<td valign="top" align="left">
<%
						// skip the first one, it is the pipeline task itself
						vTasks.nextElement();
					    	for (i = 0; vTasks.hasMoreElements(); i++) {
							TaskInfo ti = ((TaskInfo)vTasks.nextElement());
							TaskInfoAttributes tia = ti.giveTaskInfoAttributes();
							LSID l = new LSID((String)tia.get(GPConstants.LSID));
%>
							<%= ti.getName() %> version <%= l.getVersion() %><br>
<%						} %>
						</td></tr></table>
	                        	        </form>
        	                        	<jsp:include page="footer.jsp"></jsp:include>
  		              	                </body>
                	        	        </html>
<%
						return;
					}
					lsid = taskIntegratorClient.importZipFromURL(fileURL, access_id, doRecursive); 

				} catch (WebServiceErrorMessageException wse){
					vProblems = wse.getErrors();
				}

			} catch (Exception ioe) {
				taskName = "[unknown task name] in " + fullName;
				vProblems = new Vector();
				vProblems.add("Unable to install " + fullName + ": " + ioe.getMessage());
			}
			attachment.delete();
		} catch (SmartUploadException sue) {
		    	throw new Exception("error saving " + fullName + ":<br>" + sue.getMessage());
		}
	} // end loop for each file in upload

} else { // end if not isEncodedPost

    if (url != null) {
	taskName = "[unknown task name]";
	try {
		filename = GenePatternAnalysisTask.downloadTask(url);
		taskName = url;
		int access_id = (request.getParameter(GPConstants.PRIVACY) != null ? GPConstants.ACCESS_PRIVATE : GPConstants.ACCESS_PUBLIC);
		try {
			boolean askedRecursive = (request.getParameter("askedRecursive") != null);
			boolean doRecursive = (request.getParameter("inclDependents") != null);
			String fileURL = new File(filename).toURI().toURL().toString();
			if (!askedRecursive &&
			    taskIntegratorClient.isZipOfZips(fileURL)) {
				// see if user wants to install recursively or just the first entry
				attachmentName = url.substring(url.lastIndexOf("/")+1);
				Vector vTaskInfos = GenePatternAnalysisTask.getZipOfZipsTaskInfos(new File(filename));
				Enumeration vTasks = vTaskInfos.elements();
%>
                                <jsp:include page="navbar.jsp"></jsp:include>
                                <form>
				<br>contents: 
<%
				// skip the first one, it is the pipeline task itself
				vTasks.nextElement();
			    	for (i = 0; vTasks.hasMoreElements(); i++) {
					if (i > 0) out.println(",");
					TaskInfo ti = ((TaskInfo)vTasks.nextElement());
					TaskInfoAttributes tia = ti.giveTaskInfoAttributes();
					LSID l = new LSID((String)tia.get(GPConstants.LSID));
%>
					<%= ti.getName() %> version <%= l.getVersion() %>
<%				} %><br><br>
                                <input type="checkbox" name="inclDependents" checked> include all tasks used within <a href="<%= url %>"><%= attachmentName %></a>?
                                <input type="hidden" name="url" value="<%= fileURL %>">
				<input type="hidden" name="askedRecursive" value="true">
                                <input type="submit" name="submit" value="install">
				<br>
				<table cols="2">
				<tr><td valign="top" align="right">contents:</td>
				<td valign="top" align="left">
<%
				// skip the first one, it is the pipeline task itself
				vTasks.nextElement();
			    	for (i = 0; vTasks.hasMoreElements(); i++) {
					if (i > 0) out.println(",");
					TaskInfo ti = ((TaskInfo)vTasks.nextElement());
					TaskInfoAttributes tia = ti.giveTaskInfoAttributes();
					LSID l = new LSID((String)tia.get(GPConstants.LSID));
%>
					<%= ti.getName() %> version <%= l.getVersion() %>
<%						} %>
				</td></tr></table>
                                </form>
                                <jsp:include page="footer.jsp"></jsp:include>
                                </body>
                                </html>
<%
				return;
			}
			lsid = taskIntegratorClient.importZipFromURL(fileURL, access_id, doRecursive); 
		} catch (WebServiceErrorMessageException wse){
			vProblems = wse.getErrors();
		}

	} catch (Exception e) {
		vProblems = new Vector();
		vProblems.add("Unable to load " + url + ": " + e.getMessage());
	} finally {
		if (filename != null) { new File(filename).delete(); }
	}
    }

} // end if not isEncodedPost

%>
<jsp:include page="navbar.jsp"></jsp:include>
<%
if(vProblems != null && vProblems.size() > 0) {
%>
	There are some problems with the task description for <%= taskName %> 
	<%= (url != null) ? ("in <a href=\"" + url + "\">" + url + "</a>") : "" %> that need to be fixed:<br>
	<ul>
<%	
    	for (Enumeration eProblems = vProblems.elements(); eProblems.hasMoreElements(); ) {
%>
		<li><%= GenePatternAnalysisTask.htmlEncode((String)eProblems.nextElement()) %></li>
<%
	}
%>
	</ul>
	<a href="addZip.jsp">back</a><br>
<%
} else {
	TaskInfo ti = GenePatternAnalysisTask.getTaskInfo(lsid, username);
%>
    Installation of the <%= ti.getName() %> task is complete.  
<% 

	String taskType = ti.giveTaskInfoAttributes().get(GPConstants.TASK_TYPE);
	if (!taskType.equals(GPConstants.TASK_TYPE_PIPELINE)) {
	
%>
		Try it out!<br><br>
		<jsp:include page="runTask.jsp" flush="true">
			<jsp:param name="name" value="<%= lsid %>"/>
			<jsp:param name="noEnvelope" value="1"/>
		</jsp:include>
<% 
	} else {
%>
		<a href="runPipeline.jsp?<%= GPConstants.NAME %>=<%= lsid %>&cmd=run">Try it out</a><br><br>
<%
	}
} // end if successfully installed
%>
<a href="addZip.jsp">install another zip file</a><br>
<jsp:include page="footer.jsp"></jsp:include>
</body>
</html>
