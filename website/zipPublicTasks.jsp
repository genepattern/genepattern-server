<%@ page import="org.genepattern.analysis.TaskInfo,
		 org.genepattern.analysis.TaskInfoAttributes,
		 org.genepattern.server.analysis.ParameterInfo,
		 org.genepattern.server.analysis.ParameterFormatConverter,
		 java.io.File,
		 java.io.FilenameFilter,
		 java.io.FileInputStream,
		 java.io.FileOutputStream,
		 java.io.PrintWriter,
		 java.util.Collection,
		 java.util.Enumeration,
		 java.util.Iterator,
		 java.util.HashMap,
		 java.util.Vector,
		 java.util.zip.*,
		 org.genepattern.server.util.OmnigeneException,
		 org.genepattern.util.GPConstants,
		 org.genepattern.server.analysis.genepattern.GenePatternAnalysisTask,
		 org.genepattern.server.analysis.webservice.server.local.*"
	session="false" contentType="text/html" language="Java" %>
<%
	response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
	response.setHeader("Pragma", "no-cache");		 // HTTP 1.0 cache control
	response.setDateHeader("Expires", 0);
%>
<html>
<head>
<link href="stylesheet.css" rel="stylesheet" type="text/css">
<link href="favicon.ico" rel="shortcut icon">
<title>Zip public GenePattern tasks</title>
</head>
<body>
<jsp:include page="navbar.jsp"></jsp:include>
<% 
try {
	String dirName = request.getParameter("dir");
	if (dirName == null) dirName = System.getProperty("java.io.tmpdir") + File.separator + "GPTasks";
	File dir = new File(dirName);

	dir.mkdirs(); // create if it doesn't exist already
	// empty the directory
	int i;
	File[] files = dir.listFiles();
	for (i = 0; files != null && i < files.length; i++) {
		out.println("Deleting " + files[i] + "<br>");
		files[i].delete();
	}

	Collection tmTasks = new LocalAdminClient(null).getTaskCatalog();
	boolean isPublic;
	TaskInfo taskInfo;
	TaskInfoAttributes tia;
	StringBuffer manifestData = new StringBuffer(1000);
	ParameterInfo[] parameterInfo;
	String key;
	String value;
	byte[] buf = new byte[100000];
	out.flush();

	for (Iterator itTasks = tmTasks.iterator(); itTasks.hasNext(); ) {
		taskInfo = (TaskInfo)itTasks.next();
		tia = taskInfo.giveTaskInfoAttributes();
		if (tia == null) continue;
		isPublic = tia.get(GPConstants.PRIVACY).equals(GPConstants.PUBLIC);
		if (!isPublic) continue;
	        parameterInfo = new ParameterFormatConverter().getParameterInfoArray(taskInfo.getParameterInfo());
		manifestData.setLength(0);
		manifestData.append(GPConstants.NAME + "=" + taskInfo.getName() + "\n");
		manifestData.append(GPConstants.DESCRIPTION + "=" + taskInfo.getDescription() + "\n");

		if (parameterInfo != null) {
			int i2;
			for (i = 0; i < parameterInfo.length; i++) {
				i2 = i+1;
				manifestData.append("p" + i2 + "_name=" + parameterInfo[i].getName() + "\n");
				if (parameterInfo[i].getDescription() != null) manifestData.append("p" + i2 + "_description=" + parameterInfo[i].getDescription() + "\n");
				if (parameterInfo[i].getValue() != null) manifestData.append("p" + i2 + "_value=" + parameterInfo[i].getValue() + "\n");
				HashMap attributes = parameterInfo[i].getAttributes();
				if (attributes != null) {
				    	for (Iterator eAttributes = attributes.keySet().iterator(); eAttributes.hasNext(); ) {
						key = (String)eAttributes.next();
						value = (String)attributes.get(key);
						if (value == null) value = "";
						manifestData.append("p" + i2 + "_" + key + "=" + value + "\n");
				        }
				}
			}
		}
		for (i = 0; i < GPConstants.TASK_INFO_ATTRIBUTES.length; i++) {
			key = GPConstants.TASK_INFO_ATTRIBUTES[i];
			value = tia.get(key);
			if (value == null) value = "";
			manifestData.append(key + "=" + value + "\n");
		}

		// create zip file
		File zipFile = new File(dirName, taskInfo.getName() + ".zip");
		FileInputStream is = null;
		ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile));
		
		// insert manifest
		ZipEntry zipEntry = new ZipEntry(GPConstants.MANIFEST_FILENAME);
		zos.putNextEntry(zipEntry);
		zos.write(manifestData.toString().getBytes());
		zos.closeEntry();

		// insert attachments
		// find $OMNIGENE_ANALYSIS_ENGINE/taskLib/<taskName> to locate DLLs, other support files
		dir = new File(GenePatternAnalysisTask.getTaskLibDir(taskInfo.getName(), (String)taskInfo.getTaskInfoAttributes().get(GPConstants.LSID), null));
		File attachment = null;
		String attachmentName = null;
		File[] fileList = dir.listFiles(new FilenameFilter() {
						public boolean accept(File dir, String name) {
							return !name.endsWith(".old");
						}
								  });
		for (i = 0; i < fileList.length; i++){
			attachment = fileList[i];
			// TODO: handle subdirectory
			value = attachment.getPath();
			value = value.substring(dir.getPath().length() + File.separator.length());
			zipEntry = new ZipEntry(value);
			zipEntry.setTime(attachment.lastModified());
			zipEntry.setSize(attachment.length());
			zos.putNextEntry(zipEntry);
			long fileLength = attachment.length();
			long numRead = 0;
			is = new FileInputStream(attachment);
			int n;
			while ((n = is.read(buf, 0, buf.length)) > 0) {
				zos.write(buf, 0, n);
				numRead += n;
			}
			is.close();
			if (numRead != fileLength) throw new Exception("only read " + numRead + " of " + fileLength + " bytes in " + attachment.getPath());
			zos.closeEntry();
		}
		zos.finish();
		zos.close();
		out.println("Created " + zipFile + "<br>");
		out.flush();
	}
	return;
} catch (Exception e) {
	out.println("<pre>" + e.getMessage() + " in makeZip.jsp\n");
	e.printStackTrace(new PrintWriter(out));
	out.println("</pre>");
} finally { %>

done<br>
<jsp:include page="footer.jsp"></jsp:include>
</body>
</html>

<% } %>