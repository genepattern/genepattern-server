<%--
  The Broad Institute
  SOFTWARE COPYRIGHT NOTICE AGREEMENT
  This software and its documentation are copyright (2003-2006) by the
  Broad Institute/Massachusetts Institute of Technology. All rights are
  reserved.

  This software is supplied without any warranty or guaranteed support
  whatsoever. Neither the Broad Institute nor MIT can be responsible for its
  use, misuse, or functionality.
--%><%@ page import="java.util.Collection,
		 java.util.Iterator,
		 org.genepattern.server.process.CatalogGenerator,
		 org.genepattern.util.GPConstants,
		 org.genepattern.util.LSID,
 		 org.genepattern.server.webservice.server.local.*"
	session="false" contentType="text/plain" language="Java" %><%

	// output a set of name value pairs: taskID/task name, taskID/LSID, and taskID/LSID-without-version-number

	String userID = request.getParameter(GPConstants.USERID);

	CatalogGenerator cg = new CatalogGenerator(userID);

	out.print(cg.generateModuleCatalog());
	
 %>