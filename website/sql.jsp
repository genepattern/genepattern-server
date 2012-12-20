<%--
  ~ Copyright 2012 The Broad Institute, Inc.
  ~ SOFTWARE COPYRIGHT NOTICE
  ~ This software and its documentation are the copyright of the Broad Institute, Inc. All rights are reserved.
  ~
  ~ This software is supplied without any warranty or guaranteed support whatsoever. The Broad Institute is not responsible for its use, misuse, or functionality.
  --%>


<%@ page language="java" info="SQL test" %>
<%@ page import="java.text.*" %>
<%@ page import="java.util.*" %>
<%@ page import="java.net.*" %>
<%@ page import="java.sql.*" %>
<%@ page import="org.genepattern.server.webservice.server.dao.AdminDAO,
                 org.genepattern.util.StringUtils" %>
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
// trim off trailing CR and LF's, since they will be recorded in the log!
                query = query.trim();
                while (query.length() > 0 && Character.isWhitespace(query.charAt(query.length() - 1)))
                    query = query.substring(0, query.length() - 1);

                if (query.length() > 0 && request.getParameter("submit") != null) {
                    try {
                        AdminDAO ds = new AdminDAO();
                        java.util.Date startTime = new java.util.Date();
                        if (query.toLowerCase().startsWith("select")) {
                            ResultSet rs = ds.executeSQL(query);
                            long elapsedMilliseconds = new java.util.Date().getTime() -
                                    startTime.getTime();
                            out.println("<font size=\"-1\">" + elapsedMilliseconds +
                                    " ms.</font><br><br>");
                            out.println(StringUtils.htmlEncode(query) + "<br><br>");
                            if (rs.next()) {
                                if (rs.isLast()) {
                                    NumberFormat moneyFormat = NumberFormat.getCurrencyInstance();
                                    ResultSetMetaData rsmd = rs.getMetaData();
                                    int columnCount = rsmd.getColumnCount();
                                    StringBuffer ret = new StringBuffer();
                                    ret.append("<table border=\"1\" cellpadding=\"0\" cellspacing=\"0\" bordercolor=\"lightgrey\" cols=\"2\">\n");
                                    for (int i = 1; i <= columnCount; i++) {
                                        ret.append("<tr>\n");
                                        ret.append("<td valign=\"top\" align=\"right\" class=\"little\">");
                                        ret.append(rsmd.getColumnName(i).toLowerCase());
                                        ret.append(": </td>\n");
                                        boolean isMoney = rsmd.getColumnType(i) == Types.DECIMAL;
                                        boolean isEmail = rsmd.getColumnName(i).startsWith("EMAIL");
                                        String s = rs.getString(i);
                                        boolean isXML = s != null && s.startsWith("<?xml");
                                        if (isMoney) s = moneyFormat.format(rs.getDouble(i));
                                        else if (isEmail) s = "<a href=\"mailto:" + rs.getString(i) + "\">" + s + "</a>";
                                        else {
                                            s = StringUtils.htmlEncode(s);
                                            s = StringUtils.replaceAll(s, "\n", "<br>\n");
                                        }
                                        ret.append("<td valign=\"top\" class=\"little\">");
                                        ret.append(s != null ? s : "&nbsp;");
                                        ret.append("</td></tr>");
                                    }
                                    ret.append("</table>");
                                    out.println(ret.toString());
                                } else {
                                    rs.previous();
                                    out.println(createTable(rs, "No rows returned by this query.<br>", null));
                                }
                                rs.close();
                                rs = null;
                            }
                        } else {
                            int rowsChanged = ds.executeUpdate(query);
                            out.println(rowsChanged + " rows changed.<br>");
                        }
                    } catch (SQLException ex) {
                        out.println(ex.getMessage() + "<br>");
                    }
                } // if query
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
<%! public static String createTable(ResultSet rs, String noRowsMessage, String url) throws SQLException {
    /** convert a resultset into an HTML table */
    boolean bHasQuestion = url != null && (url.indexOf("?") != -1);
    if (rs != null && rs.first()) {
        ResultSetMetaData rsmd = rs.getMetaData();
        int columnCount = rsmd.getColumnCount();
        StringBuffer ret = new StringBuffer();
        ret.append("<table border=\"1\" cellpadding=\"0\" cellspacing=\"0\" bordercolor=\"lightgrey\" class=\"little\">\n");
        ret.append("<tr>\n");
        for (int i = 1; i <= columnCount; i++) {
            ret.append("<td valign=\"top\" align=\"left\"><b>");
            if (url != null) {
                ret.append("<a href=\"");
                ret.append(url);
                ret.append(bHasQuestion ? "&" : "?");
                ret.append("order=");
                ret.append(rsmd.getColumnName(i));
                ret.append("\">");
            }
            ret.append(rsmd.getColumnName(i));
            if (url != null) ret.append("</a>");
            ret.append("</b></td>\n");
        }
        ret.append("</tr>\n");
        NumberFormat moneyFormat = NumberFormat.getCurrencyInstance();
        while (true) {
            ret.append("<tr>\n");
            for (int i = 1; i <= columnCount; i++) {
                boolean isMoney = rsmd.getColumnType(i) == Types.DECIMAL;
                boolean isEmail = rsmd.getColumnName(i).startsWith("EMAIL");
                String s = rs.getString(i);
                boolean isXML = s != null && s.startsWith("<?xml");
                if (isMoney) s = moneyFormat.format(rs.getDouble(i));
                else if (isEmail) s = "<a href=\"mailto:" + rs.getString(i) + "\">" + s + "</a>";
                else {
                    s = StringUtils.htmlEncode(s);
                    s = StringUtils.replaceAll(s, "\n", "<br>\n");
                }
                ret.append("<td valign=\"top\"" + (isMoney ? " align=\"right\"" : "") + " class=\"little\">");
                ret.append(s != null ? s : "&nbsp;");
                ret.append("</td>");
            }
            ret.append("</tr>\n");
            if (!rs.next()) break;
        }
        ret.append("</table>\n");
        return ret.toString();
    } else {
        return noRowsMessage;
    }
} //...createTable
%>
