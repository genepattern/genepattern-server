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


<%@ page import="org.genepattern.server.webapp.*,
		 org.genepattern.data.pipeline.*,
             org.genepattern.server.webapp.RunPipelineForJsp,
		  org.genepattern.server.util.AccessManager,
		 org.genepattern.server.genepattern.GenePatternAnalysisTask,
		 java.io.Writer,
		 java.io.PrintWriter,
		 java.util.Map,
		 java.util.HashMap,
		 java.util.*,
		 java.util.Collection,
		 java.util.Iterator"
	session="true" contentType="text/html" language="Java" %>
<%
Writer outWriter = (Writer)request.getAttribute("outputWriter");
PrintWriter outstr = outstr = new java.io.PrintWriter(out);
if (outWriter != null) {
	outstr = new java.io.PrintWriter(outWriter);
}
System.out.println("Writing to: " + outstr);

String userID= (String)request.getAttribute("userID"); // will force login if necessary
if (userID == null) return; // come back after login
String pipelineName = request.getParameter("name");
boolean showParams = request.getParameter("showParams") == null ? false : true;
boolean showLSID = request.getParameter("showLSID") == null ? false : true;
boolean hideButtons = request.getParameter("hideButtons") == null ? false : true;
PipelineModel model = (PipelineModel )request.getAttribute("pipelineModel");

RunPipelineForJsp.writePipelineBody(outstr, pipelineName, model, userID, showParams, showLSID, hideButtons);



%>

