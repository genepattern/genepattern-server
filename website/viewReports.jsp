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

%>

<html>
    <head>
        <link href="skin/stylesheet.css" rel="stylesheet" type="text/css">
        <link href="skin/favicon.ico" rel="shortcut icon">
        <title>View GenePattern server Reports</title>
        <style>
            TD.little {
                font-size: 9pt
            }
        </style>
        <jsp:include page="navbarHead.jsp" />
    </head>
    <body onLoad="javascript:dates.startdate.focus();">
        <jsp:include page="navbar.jsp" />
        <h1>Reports available for viewing</h1>
        To create a new report, <a href="requestReport.jsp">Click here</a><br /><br />

        <%
            String jobDirStr = System.getProperty("jobs");
            File outDir = new File(jobDirStr + "/../reports/");
            if (!outDir.exists()) outDir.mkdir();

            String[] reportFiles = outDir.list(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith(".html") || name.endsWith(".pdf");
                }
            });

            TreeMap<Date, ArrayList> dateMap = mapFilesByEndDate(reportFiles);
            java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("E dd MMMM, yyyy");

            for (Date d : dateMap.keySet()) {
                out.println("Reports ending on : <b>" + formatter.format(d) + "</b><br/>");
                ArrayList<String> files = dateMap.get(d);
                for (String reportFileName : files) {
                    out.println("&nbsp;&nbsp;&nbsp;&nbsp;<a href=\"reports/" + reportFileName + "\" target=\"report\" >" + reportFileName + "</a><br/>");

                }
                out.println("<hr/><br/><br/>");
            }


        %>

        <br>

        <jsp:include page="footer.jsp" />
    </body>
</html>

<%! String getEndDate(String s1) {
    int sIdx1 = s1.lastIndexOf("_");
    int sIdx2 = s1.lastIndexOf(".html");
    if (sIdx2 == -1) sIdx2 = s1.lastIndexOf(".pdf");
    return s1.substring(sIdx1 + 1, sIdx2);
}
%>

<%! String getStartDate(String s1) {
    int sIdx2 = s1.lastIndexOf("_");
    sIdx2 = s1.lastIndexOf("_", sIdx2 - 1);
    int sIdx1 = s1.lastIndexOf("_", sIdx2 - 1);
    return s1.substring(sIdx1 + 1, sIdx2);
}
%>

<%! TreeMap<Date, ArrayList> mapFilesByEndDate(String[] filenames) throws Exception {
    TreeMap<Date, ArrayList> dateMap = new TreeMap<Date, ArrayList>();
    SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd");

    for (String name : filenames) {
        String endDateStr = getEndDate(name);
        Date endDate = sdf.parse(endDateStr);
        ArrayList thisMonth = dateMap.get(endDate);
        if (thisMonth == null) {
            thisMonth = new ArrayList<String>();
            dateMap.put(endDate, thisMonth);
        }
        thisMonth.add(name);

    }
    return dateMap;
}
%>


