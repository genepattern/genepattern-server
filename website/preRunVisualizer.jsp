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


<%@ page
	import="java.io.IOException,
		 java.util.StringTokenizer,
		 java.util.Enumeration,
		 java.util.HashMap,
		 java.io.File,
		 java.io.FileInputStream,
		 java.io.FileOutputStream,
		 java.util.Date,
		 java.io.UnsupportedEncodingException,
		 java.net.InetAddress,
 		 java.net.URLEncoder, 
		 java.net.URLDecoder,
 		 java.text.SimpleDateFormat,
		 java.util.Date,
		 java.util.ArrayList,
		 java.util.Enumeration, 
		 java.util.GregorianCalendar,
		 java.text.ParseException,
		 java.text.DateFormat,
		 java.util.Properties,
		 java.nio.channels.FileChannel,
		 java.util.List,
		 java.util.Iterator,		
		 java.util.Enumeration,
		 org.apache.commons.fileupload.DiskFileUpload,
             org.apache.commons.fileupload.FileItem,
             org.apache.commons.fileupload.FileUpload,
		
 		 org.genepattern.util.StringUtils,
		
		 org.genepattern.webservice.TaskInfo,
		 org.genepattern.webservice.TaskInfoAttributes,
		 org.genepattern.webservice.ParameterFormatConverter,
		 org.genepattern.webservice.ParameterInfo,
		 org.genepattern.server.genepattern.GenePatternAnalysisTask,
		 org.genepattern.util.GPConstants,
		 org.genepattern.util.LSID,
		 org.genepattern.webservice.OmnigeneException,
		 org.genepattern.webservice.AnalysisWebServiceProxy,
		 org.genepattern.webservice.TaskInfo,
		 org.genepattern.webservice.JobInfo,
		 org.genepattern.data.pipeline.PipelineModel"
	session="false" contentType="text/html" language="Java"%>
<%
response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
response.setHeader("Pragma", "no-cache");		 // HTTP 1.0 cache control
response.setDateHeader("Expires", 0);
%>

<html>
<head>
<link href="skin/stylesheet.css" rel="stylesheet" type="text/css">
<link href="skin/favicon.ico" rel="shortcut icon">
<title>Running Task</title>
<jsp:include page="navbarHead.jsp"/>
</head>
<body>

<jsp:include page="navbar.jsp"/>


<%

/**
 * To run a visualizer, we first upload the files here to get them a URL and then call
 * runVisualizer.jsp to actually launch it after we move the params out of the file upload into
 * a normal request
 */


Properties requestParameters = new Properties();
HashMap requestFiles = new HashMap();

String userID = (String) request.getAttribute(GPConstants.USERID);



try {


DiskFileUpload fub = new DiskFileUpload();
boolean isEncodedPost = FileUpload.isMultipartContent(request);
List rParams = fub.parseRequest(request);
int fileCount = 0;

for (Iterator iter = rParams.iterator(); iter.hasNext();) {
    FileItem fi = (FileItem) iter.next();

    if (fi.isFormField()) {
		// check for multiple values and append if true
		String val = (String)requestParameters.get(fi.getFieldName());
		if ( val != null) {
 			val = val + GPConstants.PARAM_INFO_CHOICE_DELIMITER + fi.getString();
	   		requestParameters.put(fi.getFieldName(), val);

		}else {
	   		requestParameters.put(fi.getFieldName(), fi.getString());
		}

    } else {
        // it is the file
        fileCount++;
        String name = fi.getName();
        
        if (name == null || name.equals("")) {
            continue;
        }
        File aFile = new File(System.getProperty("java.io.tmpdir"), name);
        requestFiles.put(fi.getFieldName(), aFile);
        fi.write(aFile);
    }
 }


	String RUN = "run";
	String CLONE = "clone";
	HashMap htFilenames = new HashMap();

	String lsid = URLDecoder.decode(requestParameters.getProperty("taskLSID"), "UTF-8");
	String taskName = URLDecoder.decode(requestParameters.getProperty("taskName"), "UTF-8");

	
	boolean DEBUG = false; // (requestParameters.getProperty("debug") != null);

	if (DEBUG) {
		System.out.println("\n\nPRERUN VISUALIZER Request parameters:<br>");
		for (Iterator eNames = requestParameters.keySet().iterator(); eNames.hasNext(); ) {
			String n = (String)eNames.next();
                        if (!("code".equals(n)))
			System.out.println(n + "='" + StringUtils.htmlEncode(requestParameters.getProperty(n)) + "'");
		}
	}
	String tmpDirName = null;
	if (fileCount > 0) {

		String attachmentDir = null;
		File dir = null;
		String attachmentName = null;

		File attachedFile = null;

		for (Iterator iter = requestFiles.keySet().iterator(); iter.hasNext(); ){
			String key = (String)iter.next();
			
			attachedFile = (File)requestFiles.get(key);
			if (!attachedFile.exists()) continue;

			try {
				attachmentName = attachedFile.getName();
				if (attachmentName.trim().length() == 0) continue;
				String fieldName = key;
				String fullName = attachedFile.getCanonicalPath();

				if (DEBUG) System.out.println("makePipeline: " + fieldName + " -> " + fullName);
				if (fullName.startsWith("http:") || fullName.startsWith("https:") || fullName.startsWith("ftp:") || fullName.startsWith("file:")) {
					// don't bother trying to save a file that is a URL, retrieve it at execution time instead
					htFilenames.put(fieldName, fullName); // map between form field name and filesystem name
					continue;
				}
					
				dir = new File(System.getProperty("java.io.tmpdir"));
				// create a bogus dir under this for the input files
				tmpDirName = taskName + "_" + userID + "_" + System.currentTimeMillis();			
				dir = new File(dir, tmpDirName );
				dir.mkdir();
				attachmentName = dir.getPath() + File.separator + attachmentName;
				File attachment = new File(attachmentName);
				if (attachment.exists()) {
					attachment.delete();
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

				String encodedName = URLEncoder.encode(attachment.getName(), "utf-8");
				htFilenames.put(fieldName, tmpDirName + "/" + encodedName  ); // map between form field name and filesystem name
				
				if (DEBUG) System.out.println(fieldName + "=" + fullName + " (? bytes) in " + htFilenames.get(fieldName) + "<br>");
			} catch (Exception sue) {
			    	throw new Exception("error saving " + attachmentName  + ": " + sue.getMessage());
			}
		}
		
	} // loop over files

	
	// set up the call to the runVisualizer.jsp by putting the params into the request
	// and then forwarding through a requestDispatcher
	TaskInfo task = GenePatternAnalysisTask.getTaskInfo(lsid, userID);
	if(task==null) {
	    out.println("Task not found");
		return;
	}
	ParameterInfo[] parmInfos = task.getParameterInfoArray();
    if(parmInfos==null) {
        parmInfos = new ParameterInfo[0];
    }
	request.setAttribute("name", lsid);
	String server = request.getScheme() + "://"+ InetAddress.getLocalHost().getCanonicalHostName() + ":"
					+ System.getProperty("GENEPATTERN_PORT");

	ArrayList missingReqParams = new ArrayList();
	
	for (int i=0; i < parmInfos.length; i++){
		ParameterInfo pinfo = parmInfos[i];
		String value;	
		if (pinfo.isInputFile()){
			value = (String)htFilenames.get(pinfo.getName());
			if (value == null) {
				//System.err.println("preRunVisualizer.jsp: no input file specified for " + task.getName() + "'s " + pinfo.getName());
				HashMap attrs = pinfo.getAttributes();
				attrs.put(pinfo.MODE , pinfo.URL_INPUT_MODE);
				attrs.remove(pinfo.TYPE);
			} else
			if (value.startsWith("http:") || value.startsWith("https:") || value.startsWith("ftp:") || value.startsWith("file:")) {
				HashMap attrs = pinfo.getAttributes();
				attrs.put(pinfo.MODE , pinfo.URL_INPUT_MODE);
				attrs.remove(pinfo.TYPE);
			} else {
				value = server + "/"+request.getContextPath()+"/getFile.jsp?task=&file="+ value;
			}
		} else {
			value = requestParameters.getProperty(pinfo.getName());
		}
		request.setAttribute(pinfo.getName(), value);

		// look for missing required params
		if ((value == null) || (value.trim().length() == 0)){
			HashMap pia = pinfo.getAttributes();
			boolean isOptional = ((String)pia.get(GPConstants.PARAM_INFO_OPTIONAL[GPConstants.PARAM_INFO_NAME_OFFSET])).length() > 0;
		
			if (!isOptional){
				missingReqParams.add(pinfo);
			}
		}

		pinfo.setValue(value);

	}
	if (missingReqParams.size() > 0){
		System.out.println(""+missingReqParams);

		request.setAttribute("missingReqParams", missingReqParams);
		(request.getRequestDispatcher("runTaskMissingParams.jsp")).include(request, response);
%>
<jsp:include page="footer.jsp"/>
</body>
</html>
<%
		return;
	}
System.out.println("PRV="+userID);

	RequestDispatcher rd = request.getRequestDispatcher("runVisualizer.jsp");
	rd.include(request, response);
	GenePatternAnalysisTask.createVisualizerJob(userID, ParameterFormatConverter.getJaxbString(parmInfos) , taskName, lsid);
%>

<table width='100%' cellpadding='10'>
	<tr>
		<td>Running <a href="addTask.jsp?view=1&name=<%= lsid %>"><%=requestParameters.getProperty("taskName")%></a>
		version <%= new LSID(lsid).getVersion() %> on <%=new Date()%></td>
	</tr>
	<tr>
		<td><%=requestParameters.getProperty("taskName")%> ( <%
for (int i=0; i < parmInfos.length; i++){
		ParameterInfo pinfo = parmInfos[i];
		String value = pinfo.getValue();	
		out.println(pinfo.getName());
		out.println("=");
		if (pinfo.isInputFile()) {
			String htmlValue = StringUtils.htmlEncode(pinfo.getValue());		
			if (value.startsWith("http:") || value.startsWith("https:") || value.startsWith("ftp:") || value.startsWith("file:")) {
				out.println("<a href='"+ htmlValue + "'>"+htmlValue +"</a>");
			} else {
				out.println("<a href='getFile.jsp?task=&file="+ URLEncoder.encode(tmpDirName +"/" + value)+"'>"+htmlValue +"</a>");
	
			}
		} else {
			out.println(StringUtils.htmlEncode(pinfo.getValue()));
		}
		if (i != (parmInfos.length -1))out.println(", ");
	}

} catch (Exception e){
	e.printStackTrace();
}

%> )<br>
		</td>
	</tr>
</table>




<jsp:include page="footer.jsp"/>
