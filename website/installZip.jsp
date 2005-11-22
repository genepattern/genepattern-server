<%@ page import="java.io.File,
		 java.util.*,
		 java.io.IOException,
		 java.util.Properties,
		 java.util.Vector,
		 java.util.Enumeration,
		 org.genepattern.server.util.AccessManager,
		 org.genepattern.server.genepattern.GenePatternAnalysisTask,
		 org.genepattern.util.GPConstants,
		 org.genepattern.server.webservice.server.*,
 		 org.genepattern.util.StringUtils,
		 org.genepattern.server.*,
		 org.genepattern.webservice.*,
		 org.genepattern.server.handler.*,
		 org.genepattern.server.webservice.*,
		 org.genepattern.util.LSID,
		 org.genepattern.server.TaskUtil,
		 org.genepattern.util.LSIDUtil,
		 org.genepattern.server.util.AuthorizationManager,
		 org.genepattern.server.util.IAuthorizationManager,
		 org.apache.commons.fileupload.DiskFileUpload,
		 org.apache.commons.fileupload.FileItem,
		 org.apache.commons.fileupload.FileUpload,
		 org.genepattern.server.webservice.server.local.*"
	session="false" contentType="text/html" language="Java" buffer="1kb" %>
<%
	response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
	response.setHeader("Pragma", "no-cache");		 // HTTP 1.0 cache control
	response.setDateHeader("Expires", 0);
%>

<html>
<head>
<link href="skin/stylesheet.css" rel="stylesheet" type="text/css">
<link rel="SHORTCUT ICON" href="favicon.ico" >
<title>install GenePattern task from zip file</title>
</head>
<body>	
<jsp:include page="navbar.jsp"></jsp:include>
<%
try {

//out.flush();

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
HashMap requestParameters = new HashMap();
boolean isEncodedPost = true;
int fileCount = 0;
File attachedFile = null;
String username = (String)request.getAttribute("userID");
AuthorizationManager authManager = new AuthorizationManager();

boolean taskInstallAllowed = authManager.checkPermission("createTask", username);
boolean pipelineInstallAllowed = authManager.checkPermission("createPipeline", username);
boolean suiteInstallAllowed = authManager.checkPermission("createSuite", username);

String agent = request.getHeader("USER-AGENT");
boolean isIE = false;
if (agent.indexOf("MSIE") >= 0) {
	isIE = true;
} 


if (username == null || username.length() == 0) return; // come back after login
// TODO: get values for access_id from task_access table in database

LocalTaskIntegratorClient taskIntegratorClient = new LocalTaskIntegratorClient(username, out);
DiskFileUpload fub = new DiskFileUpload();
isEncodedPost = FileUpload.isMultipartContent(request);
List params = fub.parseRequest(request);


for (Iterator iter = params.iterator(); iter.hasNext();){
	FileItem fi = (FileItem) iter.next();
	if (fi.isFormField()){
		requestParameters.put(fi.getFieldName(), fi.getString());
	} else {
		// it is the file
		fileCount++;
		String name = fi.getName();
		
		if (isIE){
			int idx1 = name.lastIndexOf("/");
			int idx2 = name.lastIndexOf("\\");
			int idx = Math.max(idx1, idx2);
			name = name.substring(idx);
		}		
		if(name==null || name.equals("")) {
			continue;
		}
		File zipFile = new File(System.getProperty("java.io.tmpdir"),name);
		fi.write(zipFile);
		requestParameters.put(fi.getFieldName(), zipFile);

	}
}

url = (String)requestParameters.get("url");
if (url != null && (url.equals("http://") || (url.length() == 0) )) url = null;


if (requestParameters.get("file1")==null && url == null) { %>
	Please select a zip file for upload containing GenePattern manifest and optional support files<br>
	<a href="addSuiteZip.jsp">back</a><br>
	<jsp:include page="footer.jsp"></jsp:include>
	</body>
	</html>
<%
	return;
}

int access_id = (request.getParameter(GPConstants.PRIVACY) != null ? GPConstants.ACCESS_PRIVATE : GPConstants.ACCESS_PUBLIC);
boolean askedRecursive = (requestParameters.get("askedRecursive") != null);
boolean doRecursive = (requestParameters.get("inclDependents") != null);
String fileURL = null;

if (isEncodedPost) {
	attachedFile = (File)requestParameters.get("file1");
	if (attachedFile == null) {
		url = (String)requestParameters.get("url");
		filename = GenePatternAnalysisTask.downloadTask(url);
		attachedFile =  new File(filename);
		taskName = attachmentName;			
		fileURL = url;

	} else {
		attachmentName = attachedFile.getName();
		taskName = attachmentName;			
		fileURL = attachedFile.toURI().toURL().toString();
	}				
} else {
 	if (url != null) {
		filename = GenePatternAnalysisTask.downloadTask(url);

		attachedFile =  new File(filename);
		attachmentName = url.substring(url.lastIndexOf("/")+1);			
		taskName = url;
		fileURL = new File(filename).toURI().toURL().toString();
	}

}

try {
	fullName = attachedFile.toString();
	try {
		if (!askedRecursive ) {


			if (taskInstallAllowed &&  taskIntegratorClient.isZipOfZips(fileURL)){
			// query user to see if they want just the first thing or all contents and then come back in

			Vector vTaskInfos = GenePatternAnalysisTask.getZipOfZipsTaskInfos(attachedFile);
			Enumeration vTasks = vTaskInfos.elements();
%>
                	            <form method="post" ENCTYPE="multipart/form-data">
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
<%					} %>
					</td></tr></table>
	                             </form>
        	                       	<jsp:include page="footer.jsp"></jsp:include>
  		              	 </body>
                	        	</html>
<%
					return;

			}
		}

		boolean isZipOfZips = taskIntegratorClient.isZipOfZips(fileURL);
		boolean isSuiteZip = taskIntegratorClient.isSuiteZip(fileURL);
		boolean isPipelineZip = taskIntegratorClient.isPipelineZip(fileURL);

		/*
		* if zip of zips, its either a pipeline or a suite but you need to
		* allow tasks if doRecursive is true
		*
		*/
		String fileNameForError = attachmentName;
		if (fileNameForError == null) fileNameForError = fileURL;

		if (isZipOfZips) {
			if (doRecursive) {
				if (!taskInstallAllowed) {
					// install pipeline but not tasks
					if (pipelineInstallAllowed) {
						doRecursive = false;
					} else {
						throw new WebServiceException
					("You do not have permission to install tasks on this server: " + fileNameForError);	
					}
				} else {
					// tasks are to be installed and we are allowed
					// so just jo on and do it
				} 
			}
			lsid = taskIntegratorClient.importZipFromURL(fileURL, access_id, doRecursive);		

		} else if (isSuiteZip){
			if (!suiteInstallAllowed) {
				throw new WebServiceException
					("You do not have permission to install suites on this server: " + fileNameForError);	
			} 
			// do the real installation
			lsid = taskIntegratorClient.importZipFromURL(fileURL, access_id, doRecursive);
		} else if (isPipelineZip) {
			if (!pipelineInstallAllowed) {
				throw new WebServiceException
					("You do not have permission to install pipelines on this server: " + fileNameForError);
			} 
			lsid = taskIntegratorClient.importZipFromURL(fileURL, access_id, doRecursive);
		} else { // must be a task
			if (!taskInstallAllowed){
				throw new WebServiceException
					("You do not have permission to install tasks on this server: " + fileNameForError);
			}
			lsid = taskIntegratorClient.importZipFromURL(fileURL, access_id, doRecursive);			
		}
		


 	} catch (WebServiceErrorMessageException wse){
		vProblems = wse.getErrors();
	}

} catch (Exception ioe) {
	taskName = "[unknown task name] in " + fullName;
	if (vProblems == null) vProblems = new Vector();
	vProblems.add("Unable to install " + fullName + ": " + ioe.getMessage());
} finally {
	attachedFile.delete(); 
}


if(vProblems != null && vProblems.size() > 0) {
%>
	There are some problems with the task description for <%= taskName %> 
	<%= (url != null) ? ("in <a href=\"" + url + "\">" + url + "</a>") : "" %> that need to be fixed:<br>
	<ul>
<%	
    	for (Enumeration eProblems = vProblems.elements(); eProblems.hasMoreElements(); ) {
%>
		<li><%= StringUtils.htmlEncode((String)eProblems.nextElement()) %></li>
<%
	}
%>
	</ul>
	<a href="addZip.jsp">back</a><br>
<%
} else {
	boolean isSuite = LSIDUtil.isSuiteLSID(lsid);

	if (! isSuite) {

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
	} else {
		// its a suite we just installed
		LocalAdminClient adminClient = new LocalAdminClient(username);
		SuiteInfo suite = adminClient.getSuite(lsid);
	%>
    		Installation of the <%= suite.getName() %> suite is complete.  
	<% 
	

	}

	

} // end if successfully installed
} catch (Exception e){
e.printStackTrace();
}

%>
<a href="addZip.jsp">install another zip file</a><br>
<jsp:include page="footer.jsp"></jsp:include>
</body>
</html>
