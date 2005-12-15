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
