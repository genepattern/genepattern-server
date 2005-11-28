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
		 javax.activation.*,
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
		 java.util.Date,
		 java.text.SimpleDateFormat,
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
	ArrayList docFiles = new ArrayList();

	String userID= (String)request.getAttribute("userID");
	
	for (Iterator iter = params.iterator(); iter.hasNext();){
		FileItem fi = (FileItem) iter.next();
		if (fi.isFormField()){
			String name = fi.getFieldName();
			String value = fi.getString();

			if ("LSID".equals(name)) {
				ArrayList lsids = (ArrayList)requestParameters.get("LSID");
				if (lsids == null){
					lsids = new ArrayList();
					requestParameters.put("LSID", lsids);
				}
				lsids.add(value);	
			} else {
				requestParameters.put(name, value);
			}
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
			docFiles.add(zipFile);

		}
	}

	System.out.println("DELETE ==" + requestParameters.get("deleteSupportFiles"));
	System.out.println("DELETE 2 ==" + requestParameters.get("deleteFiles"));


	LocalTaskIntegratorClient  taskInt = new LocalTaskIntegratorClient(userID, out);

	String accessIdStr = (String)requestParameters.get("privacy");
	int accessId = GPConstants.ACCESS_PRIVATE;
	if ("public".equalsIgnoreCase(accessIdStr)) accessId = GPConstants.ACCESS_PUBLIC;
	
	String lsid = (String)requestParameters.get("suiteLSID");
	String name = (String)requestParameters.get("suiteName");
	String description = (String)requestParameters.get("suiteDescription");
	String author = (String)requestParameters.get("suiteAuthor");
	String owner = (String)requestParameters.get("suiteOwner");

	if (name == null) {
		Date d = new Date();
		SimpleDateFormat df = new SimpleDateFormat("EEE_MMM_d_yyyy");
		String dd = df.format(d);
		name = userID + "_suite_" + dd;
	} else if (name.trim().length() == 0){
		Date d = new Date();
		SimpleDateFormat df = new SimpleDateFormat("EEE_MMM_d_yyyy");
		String dd = df.format(d);
		name = userID + "_suite_" + dd;
	}



	ArrayList LSIDs = (ArrayList)requestParameters.get("LSID");
	if (LSIDs == null) LSIDs = new ArrayList();

	ArrayList lsidsWithVersions = new ArrayList();
	// loop through the lsids and get the right version
	for (Iterator iter = LSIDs.iterator(); iter.hasNext(); ){
		String lsidStr = (String)iter.next();
		LSID anlsid = new LSID(lsidStr);
		String ver = (String)requestParameters.get(anlsid.toStringNoVersion());
		if (ver != null) {		
			anlsid.setVersion(ver);
		}
		lsidsWithVersions.add(anlsid.toString());
	}

	try {
System.out.println("suite name is " + name + " IAD=" + accessId);
		taskInt.modifySuite(accessId, lsid, name, description, author, owner, lsidsWithVersions, 	docFiles);
	} catch (Throwable t){
		t.printStackTrace();
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
<a href="suiteCatalog.jsp">Manage suites</a>&nbsp;&nbsp;&nbsp;
<a href="editSuite.jsp">Create another suite</a><br>

		<jsp:include page="footer.jsp"></jsp:include>
		</body>
		</html>

	