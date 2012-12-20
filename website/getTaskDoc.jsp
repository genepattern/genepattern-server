<%--
  ~ Copyright 2012 The Broad Institute, Inc.
  ~ SOFTWARE COPYRIGHT NOTICE
  ~ This software and its documentation are the copyright of the Broad Institute, Inc. All rights are reserved.
  ~
  ~ This software is supplied without any warranty or guaranteed support whatsoever. The Broad Institute is not responsible for its use, misuse, or functionality.
  --%>

<%
    final String old = "/getTaskDoc.jsp";
    String newPage = "/module/doc/get";

    String name = request.getParameter("name");
    if (name == null) {
        newPage = "/getTaskDocCatalog.jsp";
    } else {
        //this naming convention is important. See ModuleDocServlet and its configuration in web.xml for more information.
        newPage = "/module/doc/" + name;

        // optionally specify a filename
        String file = request.getParameter("file");
        if (file != null && file.length() > 0) {
            newPage += "/" + file;
        }
    }

    StringBuffer url = request.getRequestURL();
    int idx = url.indexOf(old);
    url.replace(idx, idx + "/getTaskDoc.jsp".length(), newPage);

    response.sendRedirect(url.toString());
%>
