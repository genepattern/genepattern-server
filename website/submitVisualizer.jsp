<%--
  The Broad Institute
  SOFTWARE COPYRIGHT NOTICE AGREEMENT
  This software and its documentation are copyright (2003-2008) by the
  Broad Institute/Massachusetts Institute of Technology. All rights are
  reserved.

  This software is supplied without any warranty or guaranteed support
  whatsoever. Neither the Broad Institute nor MIT can be responsible for its
  use, misuse, or functionality.
--%>


<%@ page
    import="java.net.URLEncoder, 
         java.util.ArrayList,
         java.util.List,
         java.util.Map,
         org.genepattern.util.StringUtils,
         org.genepattern.webservice.ParameterFormatConverter,
         org.genepattern.webservice.ParameterInfo,
         org.genepattern.server.genepattern.GenePatternAnalysisTask,
         org.genepattern.util.GPConstants,
         org.genepattern.webservice.TaskInfo,
         org.genepattern.webservice.JobInfo,
         org.genepattern.server.webapp.RunTaskHelper,
         org.apache.commons.fileupload.FileUploadException"
    session="false" contentType="text/html" language="Java"%>
<%
response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
response.setHeader("Pragma", "no-cache");        // HTTP 1.0 cache control
response.setDateHeader("Expires", 0);
%>

<html>
<head>
<link href="skin/stylesheet.css" rel="stylesheet" type="text/css">
<link href="skin/favicon.ico" rel="shortcut icon">
<title>Running Module</title>
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

String userID = (String) request.getAttribute(GPConstants.USERID);
try {
    String RUN = "run";
    String CLONE = "clone";
    
    
    // set up the call to the runVisualizer.jsp by putting the params into the request
    // and then forwarding through a requestDispatcher
    
    RunTaskHelper runTaskHelper = null;
    TaskInfo task = null;
    String lsid = null;
    List<ParameterInfo> missingReqParams = new ArrayList<ParameterInfo>();
    FileUploadException fileUploadException = null;
    try {
        runTaskHelper = new RunTaskHelper(userID, request);
        task = runTaskHelper.getTaskInfo();
        lsid = runTaskHelper.getTaskLsid();
        request.setAttribute("name", lsid);
        missingReqParams = runTaskHelper.getMissingParameters();
    }
    catch (FileUploadException e) {
        fileUploadException = e;
    }
    if (fileUploadException != null) { %>
        <font size='+1' color='red'><b> Warning </b></font><br>
        <% out.println(fileUploadException.getLocalizedMessage()); %>
        <p>
        <p>
        Hit the back button to resubmit the job.
<jsp:include page="footer.jsp"/>
</body>
</html>
<%
        return;
    }
    if (missingReqParams.size() > 0) {
        System.out.println(""+missingReqParams);
        request.setAttribute("missingReqParams", missingReqParams);
        %>
        <jsp:include page="runTaskMissingParams.jsp"/>
<jsp:include page="footer.jsp"/>
</body>
</html>
<%
        return;
    }

    String taskName = runTaskHelper.getTaskName();
    String tmpDirName = runTaskHelper.getTempDirectory().getName();
    Map<String, String> requestParameters = runTaskHelper.getRequestParameters();
    ParameterInfo[] parmInfos = task.getParameterInfoArray();
    for (int i=0; i < parmInfos.length; i++){
        ParameterInfo pinfo = parmInfos[i];
        String value = pinfo.getValue();    
        request.setAttribute(pinfo.getName(), value);
    }
    
    RequestDispatcher rd = request.getRequestDispatcher("runVisualizer.jsp");
    rd.include(request, response);
    JobInfo job = GenePatternAnalysisTask.createVisualizerJob(userID, ParameterFormatConverter.getJaxbString(parmInfos) , taskName, lsid);
%>

<table width='100%' cellpadding='10'>
    <tr>
        <td>Running <%=requestParameters.get("taskName")%> on <%=java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.LONG, java.text.DateFormat.LONG).format(job.getDateSubmitted())%>.</td>
    </tr>
    <tr>
        <td><%=requestParameters.get("taskName")%> ( <%
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


<table with="100%">
<tr><td><br>
    <a href="pages/index.jsf">Return to Modules & Pipelines Start</a>
</td></tr>
</table>

<jsp:include page="footer.jsp"/>
