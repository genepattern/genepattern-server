<%--
  ~ Copyright 2012 The Broad Institute, Inc.
  ~ SOFTWARE COPYRIGHT NOTICE
  ~ This software and its documentation are the copyright of the Broad Institute, Inc. All rights are reserved.
  ~
  ~ This software is supplied without any warranty or guaranteed support whatsoever. The Broad Institute is not responsible for its use, misuse, or functionality.
  --%>

<!-- /*
The Broad Institute
SOFTWARE COPYRIGHT NOTICE AGREEMENT
This software and its documentation are copyright (2003-2012) by the
Broad Institute/Massachusetts Institute of Technology. All rights are
reserved.

This software is supplied without any warranty or guaranteed support
whatsoever. Neither the Broad Institute nor MIT can be responsible for its
use, misuse, or functionality.
*/ -->
<%@ page language="java" info="SQL test" %>
<%@ page import="java.text.*" %>
<%@ page import="java.util.*" %>
<%@ page import="java.net.*" %>
<%@ page import="java.text.*" %>
<%@ page import="java.io.*" %>
<%@ page import="org.genepattern.server.util.DateUtils" %>

<%
    response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
    response.setHeader("Pragma", "no-cache");         // HTTP 1.0 cache control
    response.setDateHeader("Expires", 0);

    String startDateStr = request.getParameter("startdate");
    //if (query == null) query = "";
    String endDateStr = request.getParameter("enddate");
    String startDateForm = request.getParameter("findStart");
    String endDateForm = request.getParameter("findEnd");


    System.out.println("Dates from " + startDateStr + " to " + endDateStr);
    java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("E dd MMMM, yyyy  HH:mm");

    ArrayList<String> reportNames = new ArrayList<String>();

    File reportDir = new File("../reports");
    if (!reportDir.exists()) reportDir.mkdir();

    for (String report : reportDir.list()) {
        if (report.endsWith("jrxml")) {
            reportNames.add(report);
        }
    }


%>
<html>
    <head>
        <link href="skin/stylesheet.css" rel="stylesheet" type="text/css">
        <link href="skin/favicon.ico" rel="shortcut icon">
        <title>Get Dates for Report</title>
        <style>
            TD.little {
                font-size: 9pt
            }
        </style>
        <jsp:include page="navbarHead.jsp" />
    </head>
    <body onLoad="javascript:dates.startdate.focus();">
        <jsp:include page="navbar.jsp" />
        <h1>Please select one or more reports and the start and end date</h1>

        <form method="post" name="dates" action="createReport.jsp">

            <%
                if (reportNames.size() == 0) {
            %>

            No reports are installed on this server. Install jasper jrxml files to your
            <pre>GenePatternServer/reports</pre>
            directory for them to show up here.
            For contained sub-reports, place the .jasper files in this directory.

            <%
            } else {
            %>
            <table>
                <%
                    for (String name : reportNames) {
                %>
                <tr>
                    <td />
                    <td colspan=>
                        <input type="checkbox" name="reportName" value="<%=name%>"><%=name%>
                        </input>
                    </td>
                </tr>

                <%
                    }
                %>
                <tr>
                    <td>&nbsp;</td>
                    <td />
                </tr>
                <tr>
                    <td> Format:</td>
                    <td>
                        <input type="checkbox" name="pdfFormat" value="pdf" checked="true">pdf</input>
                        <input type="checkbox" name="htmlFormat" value="html">Html</input>
                    </td>
                </tr>
                <tr>
                    <td valign="top">

                        Start date: (## month, YYYY)
                    </td>
                    <td valign="top">
                        <input type="text" size="40" name="startdate" value="<%=new Date()%>" wrap accesskey="Q" />
                        <input type="radio" name="findStart" value="month" checked="true">start of month</input>
                        <input type="radio" name="findStart" value="week">start of week</input>
                        <input type="radio" name="findStart" value="exact">exact</input>
                    </td>
                </tr>
                <tr>
                    <td valign="top">
                        End date: (## month, YYYY)
                    </td>
                    <td valign="top">
                        <input type="text" size="40" name="enddate" value="<%=new Date()%>" wrap accesskey="Q" />
                        <input type="radio" name="findEnd" value="month" checked="true">end of month</input>
                        <input type="radio" name="findEnd" value="week">end of week</input>
                        <input type="radio" name="findEnd" value="exact">exact</input>
                    </td>
                </tr>

                <tr>
                    <td colspan="2" valign="top" align="center">
                        <input type="submit" value="submit" name="submit" accesskey="S" class="little">
                        <input type="reset" value="reset" class="little">
                    </td>
                </tr>
            </table>
            <%
                } //end of if no reports else clause
            %>
        </form>

        <br>

        <jsp:include page="footer.jsp" />
    </body>
</html>

