<%@ page import="
                 java.util.ArrayList,
                 java.util.List,
                 java.util.Map,
                 org.genepattern.server.webapp.BatchSubmit,
                 org.genepattern.server.user.UserDAO,
                 org.genepattern.util.GPConstants,
                 org.apache.commons.fileupload.FileUploadException,
                 org.genepattern.server.domain.BatchJob"
         session="true" contentType="text/html" language="Java" %>
<%
	FileUploadException fileUploadException = null;
	List missingReqParams = null;
	BatchSubmit batchSubmit = null;	
	try{
		batchSubmit = new BatchSubmit(request);
		batchSubmit.submitJobs();
	
		missingReqParams = batchSubmit.getMissingParameters();
	}catch (FileUploadException e) {
	        fileUploadException = e;
    }catch (Exception e){ %>    	
		<jsp:include page="internalError.jsp"/>
	<%}
	if (fileUploadException != null || missingReqParams.size() > 0 || !batchSubmit.listSizesMatch() || !batchSubmit.matchedFiles()) {
	        request.setAttribute("missingReqParams", missingReqParams);
	%>		
		<head>
		    <link href="skin/stylesheet.css" rel="stylesheet" type="text/css">
		    <link href="skin/favicon.ico" rel="shortcut icon">
		    <link href="css/style.css" rel="stylesheet" type="text/css">
		    <link href="css/style-jobresults.css" rel="stylesheet" type="text/css">
		    <title>GenePattern - Run Module Results</title>
		    <script language="Javascript" src="js/prototype.js"></script>
		    <script language="Javascript" src="js/commons-validator-1.3.0.js"></script>
		    <script language="Javascript" src="js/genepattern.js"></script>
		    <jsp:include page="navbarHead.jsp"/>
		</head>
		<body>
		
		<jsp:include page="navbar.jsp"/>
		<% if (missingReqParams.size() > 0) { %>
		<jsp:include page="runTaskMissingParams.jsp"/>
		<% } else if (!batchSubmit.listSizesMatch() || !batchSubmit.matchedFiles() ) { %>
	       <font size='+1' color='red'><b> Warning </b></font><br>
	       You have uploaded multiple files for multiple parameters but the lists of files do not match.
		   <br>
		   For a batch process you must either <ul>
		   	<li> Upload multiple files for a single parameter only.  Or </li>
		   	<li> Each parameter must have the same list of files, whose names differ only in their extension (e.g.  Parameter 1 has a.gcs;b.gcs;c.gcs and parameter 2 has a.res;b.res;c.res).</li>
		   </ul>		   	
	       <p>
	       <p>
	       Hit the back button to resubmit the job.
		<% } else { %>
		       <font size='+1' color='red'><b> Warning </b></font><br>
		       <% out.println(fileUploadException.getLocalizedMessage()); %>
		       <p>
		       <p>
		       Hit the back button to resubmit the job.
		<% } %>
		<jsp:include page="footer.jsp"/>
		</body>
		</html>
		<%
		return;
    }	
	%>
	<jsp:useBean id="jobResultsFilterBean" scope="session" class="org.genepattern.server.webapp.jsf.JobResultsFilterBean"/>
	<jsp:setProperty property="selectedGroup" name="jobResultsFilterBean" value="<%= BatchJob.BATCH_KEY + batchSubmit.getId() %>"/>
	<%
    if (batchSubmit.isBatch() ) {
		response.sendRedirect("jobResults?openVisualizers=true");		
	} else{
		response.sendRedirect("jobResults/"+batchSubmit.getId() + "?openVisualizers=true");		 
	}
%>