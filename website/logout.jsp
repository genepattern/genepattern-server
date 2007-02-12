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

<%@ page
	session="true" contentType="text/html" language="Java" %>
<%
	org.genepattern.server.webapp.jsf.UIBeanHelper.logout(request, response, session);
%>
<jsp:forward page="/pages/login.jsf" />

