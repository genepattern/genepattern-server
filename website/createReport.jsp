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
<%@ page import="java.sql.Connection" %>
<%@ page import="org.genepattern.server.database.HibernateUtil" %>

<%@ page import="net.sf.jasperreports.engine.design.*" %>
<%@ page import="net.sf.jasperreports.view.*" %>
<%@ page import="net.sf.jasperreports.engine.*" %>
<%@ page import="org.genepattern.server.util.DateUtils" %>

<%
    response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
    response.setHeader("Pragma", "no-cache"); // HTTP 1.0 cache control
    response.setDateHeader("Expires", 0);

    String startDateStr = request.getParameter("startdate");
    //if (query == null) query = "";
    String endDateStr = request.getParameter("enddate");
    String startDateForm = request.getParameter("findStart");
    String endDateForm = request.getParameter("findEnd");
    String[] reportNames = request.getParameterValues("reportName");
    boolean htmlFormat = request.getParameter("htmlFormat") != null;
    boolean pdfFormat = request.getParameter("pdfFormat") != null;
    boolean returnDocument = request.getParameter("returnDocument") != null;

    java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat(
            "E dd MMMM, yyyy  HH:mm");
    java.text.SimpleDateFormat filenameFormatter = new java.text.SimpleDateFormat(
            "yyMMdd");

    String server = request.getScheme() + "://"
            + InetAddress.getLocalHost().getCanonicalHostName() + ":"
            + System.getProperty("GENEPATTERN_PORT");

    System.out.println("Start = " + startDateStr + "    " + startDateForm);
    System.out.println("End = " + endDateStr + "    " + endDateForm);
    System.out.println("pdf = " + pdfFormat);
    System.out.println("html = " + htmlFormat);
    System.out.println("ret = " + returnDocument);
    System.out.println("Rep = " + reportNames[0]);


    Connection con = HibernateUtil.getSession().connection();

    /**
     * Set up the dates
     */
    Date givenStartDate = new Date();
    try {
        givenStartDate = new Date(startDateStr);
    } catch (Exception e) {
        givenStartDate = filenameFormatter.parse(startDateStr);
    }
    Date startDate;
    if ("week".equals(startDateForm)) {
        startDate = (new Date(DateUtils.getPreviousDay(givenStartDate
                .getTime(), 1)));
    } else if ("month".equals(startDateForm)) {
        startDate = new Date(DateUtils.getStartOfMonth(givenStartDate
                .getTime()));
    } else {
        startDate = givenStartDate;
    }
    Date givenEndDate = new Date();
    try {
        givenEndDate = new Date(endDateStr);
    } catch (Exception e) {
        givenEndDate = filenameFormatter.parse(endDateStr);
    }

    Date endDate;
    if ("week".equals(endDateForm)) {
        endDate = DateUtils.endOfDay(new Date(DateUtils.getNextDay(
                givenEndDate.getTime(), 7)));
    } else if ("month".equals(endDateForm)) {
        endDate = new Date(DateUtils.getEndOfMonth(givenEndDate
                .getTime()));
    } else {
        endDate = givenEndDate;
    }

    //
    // put the report outputs into a directory called reports in the jobs directory
    //
    String jobDirStr = System.getProperty("jobs");
    File outDir = new File(jobDirStr + "/../reports/");

    if (!outDir.exists())
        outDir.mkdir();

    File reportDir = new File("../reports/");
    ArrayList<JasperReport> reports = new ArrayList<JasperReport>();
    Map<String, JasperReport> reportMap = new HashMap<String, JasperReport>();
    for (String name : reportNames) {
        File f = new File(reportDir, name);
        int idx = name.indexOf(".jrxml");
        String nom = name.substring(0, idx);

        JasperDesign jasperDesign = JasperManager.loadXmlDesign(f
                .getCanonicalPath());
        JasperReport jasperReport = JasperManager
                .compileReport(jasperDesign);
        reports.add(jasperReport);
        reportMap.put(nom, jasperReport);
    }

    // Second, create a map of parameters to pass to the report.
    Map parameters = new HashMap();
    parameters.put("StartDate", startDate);
    parameters.put("EndDate", endDate);
    parameters.put("HostName", InetAddress.getLocalHost()
            .getCanonicalHostName());
    parameters.put("HostUrl", server + request.getContextPath());
    parameters.put("SUBREPORT_DIR", reportDir.getCanonicalPath() + "/");

    StringBuffer reportLinksBuff = new StringBuffer();

    File firstReportToReturn = null;
    for (Iterator iter = reportMap.keySet().iterator(); iter.hasNext(); ) {
        String key = (String) iter.next();
        JasperReport aReport = reportMap.get(key);
        JasperPrint jasperPrint = JasperFillManager.fillReport(aReport,
                parameters, con);

        String reportFileName = key + "_"
                + filenameFormatter.format(startDate) + "_to_"
                + filenameFormatter.format(endDate);
        String htmlReportFilePath = outDir.getCanonicalPath()
                + File.separator + reportFileName + ".html";
        String pdfReportFilePath = outDir.getCanonicalPath()
                + File.separator + reportFileName + ".pdf";

        if (htmlFormat) {
            JasperExportManager.exportReportToHtmlFile(jasperPrint,
                    htmlReportFilePath);
            reportLinksBuff
                    .append("Created HTML report  <a href=\"reports/"
                            + reportFileName + ".html\">"
                            + reportFileName + ".html</a><br/>");
        }
        if (pdfFormat) {
            JasperExportManager.exportReportToPdfFile(jasperPrint,
                    pdfReportFilePath);
            if (firstReportToReturn == null)
                firstReportToReturn = new File(pdfReportFilePath);
            reportLinksBuff
                    .append("Created PDF report  <a href=\"reports/"
                            + reportFileName + ".pdf\">"
                            + reportFileName + ".pdf</a><br/>");


        }
    }

    if (returnDocument) {
        response.setHeader("Content-disposition", "inline; filename=\""
                + firstReportToReturn.getName() + "\"");
        response.setHeader("Content-Type", "application/pdf");
        response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
        response.setHeader("Pragma", "no-cache"); // HTTP 1.0 cache control
        response.setDateHeader("Expires", 0);
        response.setDateHeader("Last-Modified", firstReportToReturn
                .lastModified());
        response.setHeader("Content-Length", ""
                + firstReportToReturn.length());

        OutputStream os = response.getOutputStream();
        InputStream is = null;
        try {
            is = new BufferedInputStream(new FileInputStream(
                    firstReportToReturn));
            byte[] b = new byte[10000];
            int bytesRead;
            while ((bytesRead = is.read(b)) != -1) {
                os.write(b, 0, bytesRead);
            }
        } finally {
            if (is != null) {
                is.close();
            }
        }
        out.clear();
        //out = pageContext.pushBody();

        return;
    } else {
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
    <body>
        <jsp:include page="navbar.jsp" />
        <h1>Your reports have been created</h1>

        <%
            out.println(reportLinksBuff.toString());

            if (startDateStr != null) {
        %>
        <br /><br />
        Start time is: <%=formatter.format(startDate)%><br />

        <%
            }
            if (endDateStr != null) {
        %>
        <br />
        End time is:  <%=formatter.format(endDate)%><br />
        <%
            }
        %>
        <a href="viewReports.jsp">See all compiled reports</a><br /><br />

        <br>

        <jsp:include page="footer.jsp" />
    </body>
</html>
<% } %>
