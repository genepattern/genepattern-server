<%@ page import="org.genepattern.server.webapp.*,
		 org.genepattern.server.process.*,
		 org.genepattern.server.genepattern.TaskInstallationException,
		 org.genepattern.server.genepattern.LSIDManager,
		 org.genepattern.server.webservice.server.local.LocalTaskIntegratorClient,
		 org.genepattern.util.LSIDUtil,
		 org.genepattern.util.GPConstants,
 		 org.genepattern.util.StringUtils,
		 org.genepattern.util.LSID,
		 org.genepattern.server.webservice.server.TaskIntegrator,
		 org.genepattern.server.webservice.server.local.LocalTaskIntegratorClient,
		 java.io.File,
		 java.net.MalformedURLException,
		 java.text.DateFormat,
		 java.text.NumberFormat,
		 java.text.ParseException,
		 java.util.Arrays,
		 java.util.Comparator,
		 java.util.Enumeration,
		 java.util.HashMap,
		 java.util.Iterator,
		 java.util.Map,
		 java.util.StringTokenizer,
		 java.util.TreeSet,
		 java.util.List,
		 java.util.ArrayList,
		 org.apache.commons.fileupload.DiskFileUpload,
		 org.apache.commons.fileupload.FileItem,
		 org.apache.commons.fileupload.FileUpload,
		 java.util.Vector"
   session="false" language="Java" %>
<jsp:useBean id="messages" class="org.genepattern.server.util.MessageUtils" scope="page"/>

<%
	response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
	response.setHeader("Pragma", "no-cache");		 // HTTP 1.0 cache control
	response.setDateHeader("Expires", 0);

	HashMap requestParameters = new HashMap();
	int fileCount = 0;
	DiskFileUpload fub = new DiskFileUpload();
	boolean isEncodedPost = FileUpload.isMultipartContent(request);
	List params = fub.parseRequest(request);


	for (Iterator iter = params.iterator(); iter.hasNext();){
		FileItem fi = (FileItem) iter.next();
		if (fi.isFormField()){
			requestParameters.put(fi.getFieldName(), fi.getString());
		} else {
			// it is the file
			fileCount++;
			String name = fi.getName();
			if(name==null || name.equals("")) {
				continue;
			}
			File zipFile = new File(System.getProperty("java.io.tmpdir"),name);
			fi.write(zipFile);
			requestParameters.put(fi.getFieldName(), zipFile);

		}
	}

%>

<html>
	<head>
	<link href="skin/stylesheet.css" rel="stylesheet" type="text/css">
	<link href="skin/favicon.ico" rel="shortcut icon">
	<title>Saving Suite - </title>
</head>
<body>

<jsp:include page="navbar.jsp"></jsp:include>


Saved Suite - 
...done.<br>
<br>
<a href="editSuite.jsp">create another suite</a><br>

		<jsp:include page="footer.jsp"></jsp:include>
		</body>
		</html>

	