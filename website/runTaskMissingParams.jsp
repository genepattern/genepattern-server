<%--
  ~ Copyright 2012 The Broad Institute, Inc.
  ~ SOFTWARE COPYRIGHT NOTICE
  ~ This software and its documentation are the copyright of the Broad Institute, Inc. All rights are reserved.
  ~
  ~ This software is supplied without any warranty or guaranteed support whatsoever. The Broad Institute is not responsible for its use, misuse, or functionality.
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

