<%@ page import="org.genepattern.server.genepattern.GenePatternAnalysisTask"
	session="false" language="Java" %><% 

response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
response.setHeader("Pragma", "no-cache");		 // HTTP 1.0 cache control
response.setDateHeader("Expires", 0);

int jobID = Integer.parseInt(request.getParameter("jobID"));
String js = request.getParameter("jobStatus");
int jobStatus = -1;
try {
	jobStatus = Integer.parseInt(js);
} catch (NumberFormatException nfe) {
} catch (NullPointerException npe) {
}
String name = request.getParameter("name");
String filename = request.getParameter("filename");

GenePatternAnalysisTask.updatePipelineStatus(jobID, jobStatus, name, filename);
if (true) return;
%>
