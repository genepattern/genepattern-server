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
		 org.apache.commons.fileupload.DiskFileUpload,
		 org.apache.commons.fileupload.FileItem,
		 org.apache.commons.fileupload.FileUpload,
		 org.genepattern.server.webservice.server.local.*"
	session="false" contentType="text/html" language="Java" buffer="1kb" %>
<%
System.out.println("-A-");
	response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
	response.setHeader("Pragma", "no-cache");		 // HTTP 1.0 cache control
	response.setDateHeader("Expires", 0);
%>
<%
System.out.println("-B-");
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
if (username == null || username.length() == 0) return; // come back after login
// TODO: get values for access_id from task_access table in database

LocalTaskIntegratorClient taskIntegratorClient = new LocalTaskIntegratorClient(username, out);
DiskFileUpload fub = new DiskFileUpload();
isEncodedPost = FileUpload.isMultipartContent(request);
List params = fub.parseRequest(request);

System.out.println("-C-");

for (Iterator iter = params.iterator(); iter.hasNext();){
	FileItem fi = (FileItem) iter.next();
System.out.println("-CC-" + fi.getFieldName());
	if (fi.isFormField()){
		requestParameters.put(fi.getFieldName(), fi.getString());
	} else {
		// it is the file
		fileCount++;
		System.out.println(fi.getFieldName() + "=" + fi.getName());
		String name = fi.getName();
		File zipFile = new File(System.getProperty("java.io.tmpdir"),name);
		fi.write(zipFile);
		requestParameters.put(fi.getFieldName(), zipFile);

	}
System.out.println("-CC- file written");

}
System.out.println("-DC-");

url = (String)requestParameters.get("url");
if (url != null && (url.equals("http://") || url.length() == 0)) url = null;

System.out.println("-D-");

if (fileCount == 0 && url == null) { %>
	Please select a zip file for upload containing GenePattern manifest and optional support files<br>
	<a href="addZip.jsp">back</a><br>
	<jsp:include page="footer.jsp"></jsp:include>
	</body>
	</html>
<%
	return;
}
System.out.println("-E-");
if (isEncodedPost) {
System.out.println("-F-");
	attachedFile = (File)requestParameters.get("zipfile");
	System.out.println("attachedFile="+ attachedFile);
	attachmentName = attachedFile.getName();
		
		fullName = attachedFile.toString();
		try {
			taskName = attachmentName;
			String privacy = (String)requestParameters.get(GPConstants.PRIVACY);
			int access_id = (privacy != null ? GPConstants.ACCESS_PRIVATE : GPConstants.ACCESS_PUBLIC);
						
			try {
				String fileURL = attachedFile.toURI().toURL().toString();
				boolean askedRecursive = ((String)requestParameters.get("askedRecursive") != null);
				boolean doRecursive = ((String)requestParameters.get("inclDependents") != null);
				if (!askedRecursive && taskIntegratorClient.isZipOfZips(fileURL)) {
 					// see if user wants to install recursively or just the first entry
					Vector vTaskInfos = GenePatternAnalysisTask.getZipOfZipsTaskInfos(attachedFile);
					Enumeration vTasks = vTaskInfos.elements();
%>
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
<%					} %>
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
		attachedFile.delete();

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
