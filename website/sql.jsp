<%--
  ~ Copyright 2012 The Broad Institute, Inc.
  ~ SOFTWARE COPYRIGHT NOTICE
  ~ This software and its documentation are the copyright of the Broad Institute, Inc. All rights are reserved.
  ~
  ~ This software is supplied without any warranty or guaranteed support whatsoever. The Broad Institute is not responsible for its use, misuse, or functionality.
  --%>


<%@ page language="java" info="SQL test" %>
<%@ page import="org.genepattern.server.database.SqlJsp" %>

<%
    response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
    response.setHeader("Pragma", "no-cache");         // HTTP 1.0 cache control
    response.setDateHeader("Expires", 0);

    String query = request.getParameter("query");
    if (query == null) query = "";
    String message = request.getParameter("message");
%>
<html>
    <head>
        <script type="text/javascript">
            function jobsSql() {
                sql.query.value = 'select * from analysis_job order by job_no desc';
                return false;
            }
        </script>
        <link href="skin/stylesheet.css" rel="stylesheet" type="text/css">
        <link href="skin/favicon.ico" rel="shortcut icon">
        <title>SQL - <%= query %>
        </title>
        <style>
            TD.little {
                font-size: 9pt
            }
        </style>
        <jsp:include page="navbarHead.jsp" />
    </head>
    <body onLoad="javascript:sql.query.focus();">
        <jsp:include page="navbar.jsp" />
        <h1>SQL</h1>
        <%
            if (message != null) { %>
        <font size="+1" color="red">
            <%= message %>
        </font>
        <% } %>
        <form method="post" name="sql">
            <table>
                <tr>
                    <td valign="top">
                        S<u>Q</u>L:
                    </td>
                    <td valign="top">
                        <textarea rows="<%= Math.max(query.length()/80+3, 8) %>" cols="80" name="query" wrap accesskey="Q"><%= query %>
                        </textarea>
                    </td>
                </tr>
                <tr>
                    <td colspan="2" valign="top" align="center">
                        <input type="submit" value="submit" name="submit" accesskey="S" class="little">
                        <input type="reset" value="reset" class="little">
                    </td>
                </tr>
                <tr>
                    <td><input type="button" value="Get Jobs SQL" onClick="javascript:jobsSql();">
                    </td>
                </tr>
            </table>
        </form>
        <%
            try {
                query=SqlJsp.trimQuery(query);
                if (query.length() > 0 && request.getParameter("submit") != null) {
                        SqlJsp.formatQuery(query, out);
                } 
            } catch (Throwable t) {
        %>
        <font color="red" size="-1" face="Courier New">
            <%= t.getMessage() %>
        </font>
        <br>
        <%
            }
        %>
        <jsp:include page="footer.jsp" />
    </body>
</html>
