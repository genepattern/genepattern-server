<%--
  ~ Copyright 2012 The Broad Institute, Inc.
  ~ SOFTWARE COPYRIGHT NOTICE
  ~ This software and its documentation are the copyright of the Broad Institute, Inc. All rights are reserved.
  ~
  ~ This software is supplied without any warranty or guaranteed support whatsoever. The Broad Institute is not responsible for its use, misuse, or functionality.
  --%><%@ page import="java.util.Collection,
                     java.util.Iterator,
                     org.genepattern.server.process.CatalogGenerator,
                     org.genepattern.util.GPConstants,
                     org.genepattern.util.LSID,
                     org.genepattern.server.webservice.server.local.*"
	session="false" contentType="text/plain" language="Java" %><%

	// output a set of name value pairs: taskID/task name, taskID/LSID, and taskID/LSID-without-version-number

	String userID = request.getParameter(GPConstants.USERID);
	String catalogType = request.getParameter("catalogtype");

	CatalogGenerator cg = new CatalogGenerator(userID);
	if ("suite".equalsIgnoreCase(catalogType)){
		out.print(cg.generateSuiteCatalog());
	} else {
		out.print(cg.generateModuleCatalog());
	}
 %>
