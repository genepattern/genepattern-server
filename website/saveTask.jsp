<%--
  ~ Copyright 2012 The Broad Institute, Inc.
  ~ SOFTWARE COPYRIGHT NOTICE
  ~ This software and its documentation are the copyright of the Broad Institute, Inc. All rights are reserved.
  ~
  ~ This software is supplied without any warranty or guaranteed support whatsoever. The Broad Institute is not responsible for its use, misuse, or functionality.
  --%>


<%@ page import="java.io.File,
                 java.io.FileInputStream,
                 java.io.FilenameFilter,
                 java.io.FileOutputStream,
                 java.io.FileWriter,
                 java.io.IOException,
                 java.lang.reflect.Constructor,
                 java.net.URLEncoder,
                 java.text.SimpleDateFormat,
                 java.util.Arrays,
                 java.util.Comparator,
                 java.util.Date,
                 java.util.HashMap,
                 java.util.TreeMap,
                 java.util.Vector,
                 java.util.Properties,
                 java.nio.channels.FileChannel,
                 java.util.List,
                 java.util.Iterator,
                 java.util.Enumeration,
                 org.apache.commons.fileupload.DiskFileUpload,
                 org.apache.commons.fileupload.FileItem,
                 org.apache.commons.fileupload.FileUpload,
                 org.genepattern.webservice.TaskInfo,
                 org.genepattern.webservice.TaskInfoAttributes,
                 org.genepattern.webservice.ParameterInfo,
                 org.genepattern.util.StringUtils,
                 org.genepattern.server.util.AccessManager,
                 org.genepattern.server.genepattern.GenePatternAnalysisTask,
                 org.genepattern.server.webservice.server.local.*,
                 org.genepattern.server.genepattern.TaskInstallationException,
                 org.genepattern.util.GPConstants,
                 org.genepattern.webservice.OmnigeneException,
                 org.genepattern.webservice.WebServiceException,
                 org.genepattern.webservice.WebServiceErrorMessageException,
                 org.genepattern.server.webservice.*,
                 org.genepattern.server.webapp.*,
                 org.genepattern.data.pipeline.PipelineModel,
                 org.genepattern.server.webservice.server.DirectoryManager,
                 org.genepattern.util.LSID"
         session="false" contentType="text/html" language="Java" %>
<%


    String userID = (String) request.getAttribute("userID"); // will force login if necessary

    LocalTaskIntegratorClient taskIntegratorClient = new LocalTaskIntegratorClient(userID, out);
%>
<%
    int i;
    StringBuffer log = null;

    String logFilename = System.getProperty("taskLog", null);
//com.jspsmart.upload.Request requestParameters = null;
    Properties requestParameters = new Properties();
    HashMap requestMultiValueParameters = new HashMap();
    HashMap requestFiles = new HashMap();


    StringBuffer sbAttachments = new StringBuffer();
    TaskInfo previousTask = null;
    String taskName = null;
    String forward = null;
    DiskFileUpload fub = new DiskFileUpload();
    boolean isEncodedPost = FileUpload.isMultipartContent(request);
    int fileCount = 0;

    if (isEncodedPost) {
        List rParams = fub.parseRequest(request);
        for (Iterator iter = rParams.iterator(); iter.hasNext(); ) {
            FileItem fi = (FileItem) iter.next();

            if (fi.isFormField()) {
                // check for multiple values and append if true
                String val = requestParameters.getProperty(fi.getFieldName());
                if (val != null) {
                    val = val + GPConstants.PARAM_INFO_CHOICE_DELIMITER + fi.getString();
                    requestParameters.put(fi.getFieldName(), val);

                } else {
                    requestParameters.put(fi.getFieldName(), fi.getString());
                }

            } else {
                // it is the file
                fileCount++;
                String name = fi.getName();
                // strip out paths on IE -- BUG 1819
                int idx = name.lastIndexOf('/');
                if (idx >= 0) {
                    name = name.substring(idx + 1);
                }
                idx = name.lastIndexOf('\\');
                if (idx >= 0) {
                    name = name.substring(idx + 1);
                }

                if (name == null || name.equals("")) {
                    continue;
                }
                File aFile = new File(System.getProperty("java.io.tmpdir"), name);
                requestFiles.put(fi.getFieldName(), aFile);
                fi.write(aFile);
            }
        }
    } else {
        for (Enumeration en = request.getParameterNames(); en.hasMoreElements(); ) {
            String pname = (String) en.nextElement();
            String val = request.getParameter(pname);

            requestParameters.put(pname, val);

        }
    }

    taskName = requestParameters.getProperty(GPConstants.NAME);
    if (taskName == null) taskName = requestParameters.getProperty(GPConstants.NAME);

// save the file attachments, if any, before installing the task, so that it is immediately ready to run
    String lsid = requestParameters.getProperty(GPConstants.LSID);
    if (lsid == null) lsid = requestParameters.getProperty(GPConstants.LSID);
    String attachmentDir = null;
    File dir = null;

// delete task
    if (requestParameters.getProperty("delete") != null || requestParameters.getProperty("delete") != null) {
        try {
            taskIntegratorClient.deleteTask(lsid);
%>
<html>
    <head>
        <title>Saved GenePattern module</title>
    </head>
    <body>
        <jsp:include page="navbarHead.jsp" />
        <jsp:include page="navbar.jsp" />
            <%= taskName %> has been deleted. Any running jobs using that module have been stopped.<br>
            <%
	} catch (Throwable t) { 
%>
        <html>
            <head>
                <title>Saved GenePattern module</title>
            </head>
            <body>
                <jsp:include page="navbarHead.jsp" />
                <jsp:include page="navbar.jsp" />
                <%
                    out.println(t + " while attempting to delete " + taskName);
                } finally {
                %>
                <jsp:include page="footer.jsp" />
            </body>
        </html>
            <%
	}
	return;
}

// delete support file

if ((requestParameters.getProperty("deleteFiles") != null || requestParameters.getProperty("deleteFiles") != null) ||
    (requestParameters.getProperty("deleteSupportFiles") != null || requestParameters.getProperty("deleteSupportFiles") != null)) {


	if ((requestParameters.getProperty("deleteSupportFiles") != null && requestParameters.getProperty("deleteSupportFiles").length() > 0) ||
	    (requestParameters.getProperty("deleteSupportFiles") != null && requestParameters.getProperty("deleteSupportFiles").length() > 0)) {

		String filename = requestParameters.getProperty("deleteFiles");
		if (filename == null) filename = requestParameters.getProperty("deleteFiles");

		forward = requestParameters.getProperty("forward");
		if (forward== null) forward = "addTask.jsp";


		if (filename != null && !filename.equals("")) {
			try {
				StringBuffer sbURL = request.getRequestURL();
				String queryString = request.getQueryString();
				if (queryString != null) {
					sbURL.append("?");
					sbURL.append(queryString);
				}
				lsid = taskIntegratorClient.deleteFiles(lsid, new String[] { filename });
				if (lsid != null) { 
					response.sendRedirect(forward + "?" + GPConstants.NAME + "=" + lsid);
					return;
				} else { %>
        <html>
            <head>
                <title>Saved GenePattern module</title>
            </head>
            <body>
                <jsp:include page="navbarHead.jsp" />
                <jsp:include page="navbar.jsp" />
                Unable to delete <%= filename %> from <%= taskName %> support files.<br>
                <jsp:include page="footer.jsp" />
            </body>
        </html>
            <%
					return;
				}
			} catch (Throwable t) { 
%>
        <html>
            <head>
                <title>Saved GenePattern module</title>
            </head>
            <body>
                <jsp:include page="navbarHead.jsp" />
                <jsp:include page="navbar.jsp" />
                <%= t %> while attempting to delete <%= filename %>
                <br>
                <jsp:include page="footer.jsp" />
            </body>
        </html>
            <%
				return;
			}
		}
	} // end of deleteSupportFiles
}

// clone task
if (requestParameters.getProperty("clone") != null) {
	String cloneName = requestParameters.getProperty("cloneName");
    try {
        StringBuffer sbURL = request.getRequestURL();
        String queryString = request.getQueryString();
        if (queryString != null) {
            sbURL.append("?");
            sbURL.append(queryString);
        }
        lsid = taskIntegratorClient.cloneTask(lsid, cloneName);
    }
    catch (Throwable t) {
        Vector vProblems = new Vector();
        vProblems.add( t.getLocalizedMessage() );
        if (t instanceof WebServiceErrorMessageException) {
            WebServiceErrorMessageException wseme = (WebServiceErrorMessageException) t;
            vProblems = wseme.getErrors();
        }
        if(vProblems != null && vProblems.size() > 0) {
%>
        <html>
            <head>
                <title>Saved GenePattern module</title>
            </head>
            <body>
                <jsp:include page="navbarHead.jsp" />
                <jsp:include page="navbar.jsp" />
                There are some problems with the <%= cloneName %> module that need to be fixed:<br>
                <ul>
                    <%
                        for (Enumeration eProblems = vProblems.elements(); eProblems.hasMoreElements(); ) {
                    %>
                    <li><%= StringUtils.htmlEncode((String) eProblems.nextElement()) %>
                    </li>
                    <%
                        }
                    %>
                </ul>
                <a href="javascript:history.back()">back</a>
                    <%
                return;
        }
    }
%>
                <html>
                    <head>
                        <title>Saved GenePattern module</title>
                    </head>
                    <body>
                        <jsp:include page="navbarHead.jsp" />
                        <jsp:include page="navbar.jsp" />
                        Cloned <%= taskName %> as <%= cloneName %>.<br>
                        <a href="addTask.jsp?<%= GPConstants.NAME%>=<%=lsid %>">edit <%= cloneName %>
                        </a><br>
                        <script language="javascript">
                            <% if("1".equals(requestParameters.getProperty("pipeline"))) {
               %>
                            window.location = "viewPipeline.jsp?<%= GPConstants.NAME %>=<%= lsid %>";
                            <%
           } else { %>
                            window.location = "addTask.jsp?view=1&<%= GPConstants.NAME %>=<%= lsid %>";
                            <%}
           %>

                        </script>
                        <jsp:include page="footer.jsp" />
                    </body>
                </html>
                    <%
	return;
} // end if requestParameters.getProperty("clone")

int access_id = requestParameters.getProperty(GPConstants.PRIVACY).equals(GPConstants.PUBLIC) ? GPConstants.ACCESS_PUBLIC : GPConstants.ACCESS_PRIVATE;

String formerName = requestParameters.getProperty(GPConstants.FORMER_NAME);
if (formerName != null && formerName.length() > 0 && !formerName.equals(taskName)) {
	try {
		previousTask = GenePatternAnalysisTask.getTaskInfo(formerName, userID);
	} catch (OmnigeneException oe) {
	}
	if (previousTask != null && !formerName.equalsIgnoreCase(taskName)) {
		// TODO: handle overwrite-by-renaming of some other task
	}

	attachmentDir = DirectoryManager.getTaskLibDir(taskName, lsid, userID);
	dir = new File(attachmentDir);
	dir.delete(); // delete the just created directory

	// renaming task, need to rename taskLib directory for this task
	formerName = requestParameters.getProperty(GPConstants.FORMER_NAME);
	File oldDir = new File(DirectoryManager.getTaskLibDir(formerName, lsid, userID));
	oldDir.renameTo(dir);

	// TODO: check whether this task is involved in any pipelines and if so, alert/offer to rename
	// the task in each pipeline
} else if (formerName != null && formerName.equals(taskName)) {
	try {
		previousTask = GenePatternAnalysisTask.getTaskInfo(formerName, userID);
	} catch (OmnigeneException oe) {
	}
}



// count parameters
int numParameterInfos;
String key = null;
String value = null;
String[] values = null;
for (numParameterInfos = 0; ; numParameterInfos++) {
	key = "p" + numParameterInfos + "_name";
	value = requestParameters.getProperty(key);
	if (value == null || value.length() == 0) break;
}

ParameterInfo[] params = new ParameterInfo[numParameterInfos];
for (i = 0; i < numParameterInfos; i++) {
	ParameterInfo pi = new ParameterInfo(requestParameters.getProperty("p" + i + "_name"), 
					     requestParameters.getProperty("p" + i + "_value"), 
					     requestParameters.getProperty("p" + i + "_description"));
	if (GPConstants.PARAM_INFO_ATTRIBUTES.length > 0) {
		HashMap attributes = new HashMap();
		for (int attributeNum = 0; attributeNum < GPConstants.PARAM_INFO_ATTRIBUTES.length; attributeNum++) {
			String attributeName = (String)GPConstants.PARAM_INFO_ATTRIBUTES[attributeNum][GPConstants.PARAM_INFO_NAME_OFFSET];

			value = requestParameters.getProperty("p" + i + "_" + attributeName);
	
			attributes.put(attributeName, value);
		}
		pi.setAttributes(attributes);
	}
	params[i] = pi;
}
for(ParameterInfo p: params) {
	if(p.getAttributes()!=null && p.getAttributes().get("type").equals("java.io.File")) {
	    p.setAsInputFile();
	    p.getAttributes().put(GPConstants.PARAM_INFO_TYPE[GPConstants.PARAM_INFO_TYPE_NAME_OFFSET], GPConstants.PARAM_INFO_TYPE_INPUT_FILE);
	}
}


TaskInfoAttributes tia = new TaskInfoAttributes();
for (i = 0; i < GPConstants.TASK_INFO_ATTRIBUTES.length; i++) {
	key = GPConstants.TASK_INFO_ATTRIBUTES[i];
	value = requestParameters.getProperty(key);
	tia.put(key, value);
}

File attachment = null;
String attachmentName = null;
File attachedFile = null;

// TODO: get values for access_id from task_access table in database
//
// check if this task already exists (byName) and use updateTask instead of installNewTask if it does
//

access_id = requestParameters.getProperty(GPConstants.PRIVACY).equals(GPConstants.PUBLIC) ? GPConstants.ACCESS_PUBLIC : GPConstants.ACCESS_PRIVATE;

Vector vProblems = null;
try {
	// merge old support files with newly submitted ones!

	javax.activation.DataHandler[] supportFiles = null;
	String[] supportFileNames = null;
	try {
	      supportFiles = taskIntegratorClient.getSupportFiles(lsid);
	      supportFileNames = taskIntegratorClient.getSupportFileNames(lsid);
	} catch(WebServiceException wse) {
	}
   
	lsid = (String)tia.get(GPConstants.LSID);
	lsid = taskIntegratorClient.modifyTask(access_id, 
				requestParameters.getProperty(GPConstants.NAME), 
				requestParameters.getProperty(GPConstants.DESCRIPTION), 
				params,
				tia,
				supportFiles, 
				supportFileNames);

	// make $GenePatternHome/taskLib/<taskName> to store DLLs, etc.


	attachmentDir = DirectoryManager.getTaskLibDir(requestParameters.getProperty(GPConstants.NAME), lsid, userID);
	dir = new File(attachmentDir);
	
	for (Iterator iter = requestFiles.keySet().iterator(); iter.hasNext(); ){
		key = (String)iter.next();
			
		attachedFile = (File)requestFiles.get(key);
		if (!attachedFile.exists()) continue;

		try {
			attachmentName = attachedFile.getName();
			if (attachmentName.trim().length() == 0) continue;
			attachment = new File(dir, attachmentName);
			if (attachment.exists()) {
				File oldVersion = new File(dir, attachment.getName() + ".old");
				if(oldVersion.exists()) {
					oldVersion.delete(); // delete the previous .old file
				}
				if(!attachment.renameTo(oldVersion)) {
					Vector v = new Vector();
					v.add("failed to rename " + oldVersion.getName() +".");
					throw new WebServiceErrorMessageException(v);
				}
			}
			
			FileChannel inChannel = null, outChannel = null;
			try	{
				inChannel = new FileInputStream(attachedFile).getChannel();
				outChannel = new FileOutputStream(attachment).getChannel();
				outChannel.transferFrom(inChannel, 0, inChannel.size());
			} finally {
				if (inChannel != null) 	inChannel.close();
				if (outChannel != null)	outChannel.close();
			}

			if (sbAttachments.length() > 0) sbAttachments.append(",");
			sbAttachments.append(attachmentName);
		} catch (Exception sue) {
		    	throw new Exception("error saving " + dir.getPath() + File.separator + attachmentName + " in " + dir.getPath() +":<br>" + sue.getMessage());
		}
	}
} catch (WebServiceErrorMessageException wseme) {
	vProblems = wseme.getErrors();
}


if(vProblems != null && vProblems.size() > 0) {
	if (formerName != null && formerName.length() > 0 && !formerName.equals(taskName)) {
		// renaming task, need to rename taskLib directory for this task
		dir.renameTo(new File(DirectoryManager.getTaskLibDir(formerName, lsid, userID)));
	}

	tia.put(GPConstants.LSID, requestParameters.getProperty("LSID"));
	TaskInfo taskInfo = new TaskInfo(-1,	requestParameters.getProperty(GPConstants.NAME), requestParameters.getProperty(GPConstants.DESCRIPTION), "", tia, userID, access_id);
	taskInfo.setParameterInfoArray(params);
	request.setAttribute("errors", vProblems);
	request.setAttribute("taskInfo", taskInfo);
	request.setAttribute("taskName", requestParameters.getProperty("name"));
	request.getRequestDispatcher("addTask.jsp").forward(request, response);
}



/*********************************
      begin logging of changes
 *********************************/
if (logFilename != null) {
/* 
log file format:

timeMS dateTime loginId moduleType moduleName  manifest supportFilesChanges URLToEditPage URLToZipDownload
76576574 2003-11-03_17:34:11 liefeld "Filter" SelectFeaturesColumns "manifest" "foo.jar,bar.jar" http://elm:8080/gp/addTask.jsp?name=SelectFeaturesColumns http://elm:8080/gp/makeZip.jsp?name=SelectFeaturesColumns
*/

	log = new StringBuffer();
	SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-M-d_H:mm");
	Date d = new Date();


	log.append(d.getTime());
	log.append(" ");
	log.append(dateFormat.format(d));
	log.append(" ");
	log.append((String)request.getAttribute("userID"));
	log.append(" ");
	String taskType = tia.get(GPConstants.TASK_TYPE);
	if (taskType == null || taskType.length() == 0) taskType = "[unclassified]";
	log.append("\"");
	log.append(taskType);
	log.append("\" \"");
	log.append(taskName);
	log.append("\"");
	log.append(" \"");

	TaskInfo thisTask = null;
	try {
		thisTask = GenePatternAnalysisTask.getTaskInfo(taskName, userID);
	} catch (OmnigeneException oe) {
	}
	// if manifest changed, list the name of the manifest file
	boolean manifestChange = (previousTask == null || !(thisTask.equals(previousTask)));
	if (manifestChange) {
		log.append(GPConstants.MANIFEST_FILENAME);
	}

	// list all newly added/replaced attachments
	log.append("\" \"");
	log.append(sbAttachments);
	log.append("\" ");

    String portStr = "";
    int port = request.getServerPort();
    if (port > 0) {
        portStr = ":"+port;
    }
	String baseURL = request.getScheme()+"://" + request.getServerName() + portStr + request.getRequestURI();
	baseURL = baseURL.substring(0, baseURL.lastIndexOf("/")+1);
	log.append(baseURL + "addTask.jsp?" + GPConstants.NAME + "=" + URLEncoder.encode(lsid, "UTF-8"));
	log.append(" ");
	log.append(baseURL + "makeZip.jsp?" + GPConstants.NAME + "=" + URLEncoder.encode(lsid, "UTF-8"));
	log.append("\n");

	if (manifestChange || sbAttachments.length() > 0) {
		FileWriter fwLog = new FileWriter(logFilename, true);
		fwLog.write(log.toString());
		fwLog.close();
	}
}
/*********************************
      end logging of changes
 *********************************/

	if (forward != null) {
		response.sendRedirect(forward);
		return;
	}
%>
                <html>
                    <head>
                        <title>Saved GenePattern module</title>
                    </head>
                    <body>
                        <jsp:include page="navbarHead.jsp" />
                        <jsp:include page="navbar.jsp" />
                        Installation of your <a href="addTask.jsp?<%= GPConstants.NAME %>=<%= URLEncoder.encode(lsid, "UTF-8") %>"><%= taskName %>
                    </a> module (version <%= new LSID(lsid).getVersion() %>) is complete.<br><br>


                        <hr>
                        <a href='<%= request.getContextPath() %>/pages/index.jsf?lsid=<%= URLEncoder.encode(lsid, "UTF-8") %>'>Run <%= taskName %>
                        </a>
                        </h4>


                        <br>

                        <jsp:include page="footer.jsp" />
                    </body>
                </html>

