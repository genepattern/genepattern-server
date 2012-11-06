<%--
  The Broad Institute
  SOFTWARE COPYRIGHT NOTICE AGREEMENT
  This software and its documentation are copyright (2003-2012) by the
  Broad Institute/Massachusetts Institute of Technology. All rights are
  reserved.

  This software is supplied without any warranty or guaranteed support
  whatsoever. Neither the Broad Institute nor MIT can be responsible for its
  use, misuse, or functionality.
--%>


<%@ page import="java.util.ArrayList, 
 		 org.genepattern.util.StringUtils,
		 org.genepattern.webservice.ParameterInfo"
	session="false" contentType="text/html" language="Java" %>

<font size='+1' color='red'><b> Warning </b></font><br>
The module could not be run. The following required parameters need to have values provided;
<p>

<%
	ArrayList missingParams = (ArrayList)request.getAttribute("missingReqParams");
	
	for (int i=0; i < missingParams.size(); i++){
		ParameterInfo pinfo = (ParameterInfo)missingParams.get(i);
		out.println("&nbsp;&nbsp;&nbsp;&nbsp;<b>" + StringUtils.replaceAll(pinfo.getName(),"."," ") );
		out.println("</b><br>");
	
	}
%>
<p>
Hit the back button to fill them in and resubmit the job.

