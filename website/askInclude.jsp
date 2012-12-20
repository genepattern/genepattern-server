<%--
  ~ Copyright 2012 The Broad Institute, Inc.
  ~ SOFTWARE COPYRIGHT NOTICE
  ~ This software and its documentation are the copyright of the Broad Institute, Inc. All rights are reserved.
  ~
  ~ This software is supplied without any warranty or guaranteed support whatsoever. The Broad Institute is not responsible for its use, misuse, or functionality.
  --%>


<%@ page session="true" contentType="text/html" language="Java" %>
<%
    response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
    response.setHeader("Pragma", "no-cache");         // HTTP 1.0 cache control
    response.setDateHeader("Expires", 0);

    String title = request.getParameter("title");
%>
<html>
    <head>
        <link href="skin/stylesheet.css" rel="stylesheet" type="text/css">
        <link rel="SHORTCUT ICON" href="favicon.ico">
        <title>Include all modules used by <%= title %>
        </title>
        <script language="Javascript">
            // submit and close
            function doSubmit(btn) {
                window.opener.location = "makeZip.jsp?name=<%= request.getParameter("name") %>" +
                        (btn.name == "IncludeDependents" ? "&includeDependents=1" : "") +
                        "&close=1";
                window.close();
                return false;
            }
        </script>
    </head>
    <body>
        <form name="ask">

            Press 'Include modules' to include all modules used by <%= title %> in the exported zip file. <br><br>
            Press 'Pipeline only' to include only the <%= title %> pipeline definition itself<br>

            <br>
            <center>
                <input type="button" name="IncludeDependents" value="Include modules" onclick="doSubmit(this)" class="little">
                <input type="button" name="DontIncludeDependents" value="Pipeline only" onclick="doSubmit(this)" class="little">
                <br>
                <input type="button" name="Cancel" value="Cancel export" onclick="javascript:window.close()" class="little">
            </center>
            <input type="hidden" name="name" value="<%= request.getParameter("name") %>">
        </form>
        <script language="Javascript">
            document.forms['ask'].target = window.opener;
        </script>
    </body>
</html>
