<%--
  ~ Copyright 2012 The Broad Institute, Inc.
  ~ SOFTWARE COPYRIGHT NOTICE
  ~ This software and its documentation are the copyright of the Broad Institute, Inc. All rights are reserved.
  ~
  ~ This software is supplied without any warranty or guaranteed support whatsoever. The Broad Institute is not responsible for its use, misuse, or functionality.
  --%>

<% /*
  The Broad Institute
  SOFTWARE COPYRIGHT NOTICE AGREEMENT
  This software and its documentation are copyright (2003-2012) by the
  Broad Institute/Massachusetts Institute of Technology. All rights are
  reserved.

  This software is supplied without any warranty or guaranteed support
  whatsoever. Neither the Broad Institute nor MIT can be responsible for its
  use, misuse, or functionality.
*/ %>
<%@ page session="false" language="Java" %>
<%
    String message = request.getParameter("e");
%>
<html>
    <head>
        <title>GenePattern module documentation: error</title>
        <link href="skin/stylesheet.css" rel="stylesheet" type="text/css">
        <link href="skin/favicon.ico" rel="shortcut icon">
        <meta http-equiv="content-type" content="text/html; charset=ISO-8859-1">
    </head>
    <body>
        <jsp:include page="navbar.jsp" />
        <br />
        <%= message %>
        <br />
        <jsp:include page="footer.jsp" />
    </body>
</html>
