<%@ page import="org.genepattern.server.webapp.*,
		 org.genepattern.data.pipeline.*,
		 org.genepattern.server.genepattern.GenePatternAnalysisTask,
		 org.genepattern.server.webservice.server.local.*,
		 org.genepattern.webservice.TaskInfo,
		 org.genepattern.webservice.ParameterInfo,
		 org.genepattern.webservice.TaskInfoAttributes,
		 org.genepattern.server.genepattern.LSIDManager,
		 org.genepattern.data.pipeline.*,
		 org.genepattern.server.*,
         org.genepattern.server.webapp.RunPipelineForJsp,
		 org.genepattern.util.GPConstants,
		 org.genepattern.util.LSID,
		 org.genepattern.util.LSIDUtil,
		 java.io.Writer,
		 java.io.PrintWriter,
		java.util.Map,
		 java.util.HashMap,
		 java.util.*,
		 java.util.Collection,
		 java.util.Iterator"
	session="true" contentType="text/html" language="Java" %>
<%
String userID = GenePatternAnalysisTask.getUserID(request, response); // will force login if necessary
if (userID == null) return; // come back after login

String pipelineName = request.getParameter("name");

if (pipelineName == null) {
%>	Must specify a name parameter
<%
	return;
}
PipelineModel model = null;
TaskInfo task = new org.genepattern.server.webservice.server.local.LocalAdminClient(userID).getTask(pipelineName);
if (task != null) {
	TaskInfoAttributes tia = task.giveTaskInfoAttributes();
	if (tia != null) {
		 String serializedModel = (String)tia.get(GenePatternAnalysisTask.SERIALIZED_MODEL);
		 if (serializedModel != null && serializedModel.length() > 0) {
			 try {
			 	 model = PipelineModel.toPipelineModel(serializedModel);
				request.setAttribute("pipelineModel", model);
			} catch (Throwable x) {
				x.printStackTrace(System.out);
			}
		}
	}
}
%>
<html>
<head>
	
<link href="stylesheet.css" rel="stylesheet" type="text/css">
<link rel="SHORTCUT ICON" href="favicon.ico" >
<title><%=model.getName()%></title>

</head>
<body>
<jsp:include page="navbar.jsp"></jsp:include>
<%
Writer outWriter = (Writer)request.getAttribute("outputWriter");
PrintWriter outstr = outstr = new java.io.PrintWriter(out);
if (outWriter != null) {
	outstr = new java.io.PrintWriter(outWriter);
}
System.out.println("Writing to: " + outstr);

if (userID == null) return; // come back after login
boolean showParams = request.getParameter("showParams") == null ? false : true;
boolean showLSID = request.getParameter("showLSID") == null ? false : true;
boolean hideButtons = request.getParameter("hideButtons") == null ? false : true;
RunPipelineForJsp.writePipelineBody(outstr, pipelineName, model, userID, showParams, showLSID, hideButtons);

%>

<jsp:include page="footer.jsp"></jsp:include>
</body>
</html>
