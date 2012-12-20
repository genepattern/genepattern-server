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


<%@ page import="java.io.File,
                 java.io.*,
                 java.io.IOException,
                 java.net.URLEncoder,
                 java.util.Collection,
                 java.util.Hashtable,
                 java.util.Iterator,
                 java.util.Comparator,
                 java.util.Arrays,
                 java.util.HashMap,
                 java.util.TreeMap,
                 java.util.ArrayList,
                 java.net.MalformedURLException,
                 org.genepattern.util.StringUtils,
                 org.genepattern.util.LSID,
                 org.genepattern.webservice.TaskInfo,
                 org.genepattern.webservice.SuiteInfo,
                 org.genepattern.webservice.TaskInfoAttributes,
                 org.genepattern.server.webservice.server.local.*,
                 org.genepattern.util.GPConstants,
                 org.genepattern.server.util.AccessManager,
                 org.genepattern.server.webservice.server.DirectoryManager,
                 org.genepattern.server.genepattern.GenePatternAnalysisTask"
         session="false" language="Java" %>
<%
    response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
    response.setHeader("Pragma", "no-cache");         // HTTP 1.0 cache control
    response.setDateHeader("Expires", 0);

    String userID = (String) request.getAttribute("userID"); // will force login if necessary
    LocalAdminClient adminClient = new LocalAdminClient(userID);
    LocalTaskIntegratorClient taskIntegratorClient = new LocalTaskIntegratorClient(userID, out);
    String name = request.getParameter("name");
    SuiteInfo si;
    TaskInfo ti;

    if (name == null) {
%>
<html>
    <head>
        <link href="skin/stylesheet.css" rel="stylesheet" type="text/css">
        <link href="skin/favicon.ico" rel="shortcut icon">
        <title>GenePattern module documentation</title>
        <meta http-equiv="content-type" content="text/html; charset=ISO-8859-1">
        <script language="JavaScript">
            function toggleVersions(divname) {
                var formobjs = document.getElementsByTagName('div');
                var acheckbox = document.getElementById(divname + 'cb');
                var visible = acheckbox.checked;
                for (var i = 0; i < formobjs.length; i++) {
                    if (formobjs[i].id == divname) {
                        if (!visible) {
                            formobjs[i].style.display = "none";
                        } else {
                            formobjs[i].style.display = "block";
                        }
                    }
                }
            }
        </script>
        <jsp:include page="navbarHead.jsp" />
    </head>
    <body>

        <jsp:include page="navbar.jsp" />
        <table>
            <tr>
                <td valign="top">
                    Note: most documentation is in Adobe's PDF format and requires Acrobat Reader for viewing. If you don't have it,
                    you can download it at no cost from the Adobe website.
                </td>
                <td valign="top">
                    <a href="http://www.adobe.com/products/acrobat/readstep2.html" target="_blank"><img src="http://www.adobe.com/images/getacro.gif" border="0"></a>
                </td>
            </tr>
        </table>
        <br>
        <table>
            <thead>
                <tr>
                    <td><b>name (version)</b></td>
                    <td><b>description</b></td>
                </tr>
            </thead>
            <tbody>
                <%

                    SuiteInfo[] suites = adminClient.getAllSuites();
                    HashMap suiteMap = new HashMap();
                    for (int i = 0; i < suites.length; i++) {
                        si = suites[i];
                        LSID lsid = new LSID(si.getLSID());
                        ArrayList versions = (ArrayList) suiteMap.get(lsid.toStringNoVersion());
                        if (versions == null) {
                            versions = new ArrayList();
                            suiteMap.put(lsid.toStringNoVersion(), versions);
                        }
                        versions.add(si);
                    }
                    for (Iterator iter = suiteMap.keySet().iterator(); iter.hasNext(); ) {
                        String key = (String) iter.next();
                        ArrayList versions = (ArrayList) suiteMap.get(key);
                        SuiteInfo[] sortedVersions = (SuiteInfo[]) versions.toArray(new SuiteInfo[versions.size()]);
                        Arrays.sort(sortedVersions, new Comparator() {
                            public int compare(Object o1, Object o2) {
                                SuiteInfo t1 = (SuiteInfo) o1;
                                SuiteInfo t2 = (SuiteInfo) o2;

                                LSID l1, l2;
                                try {
                                    l1 = new LSID((String) t1.getLSID());
                                    l2 = new LSID((String) t2.getLSID());
                                    return l2.getVersion().compareToIgnoreCase(l1.getVersion());

                                } catch (MalformedURLException mue) {
                                    // ignore
                                    return 0;
                                }
                            }
                        });
                        suiteMap.put(key, sortedVersions);
                    }


                    TreeMap sortedSuiteMap = new TreeMap(new Comparator() {
                        public int compare(Object o1, Object o2) {
                            String k1 = (String) o1;
                            String k2 = (String) o2;
                            return k1.compareToIgnoreCase(k2);
                        }
                    });


                    for (Iterator iter = suiteMap.keySet().iterator(); iter.hasNext(); ) {
                        String key = (String) iter.next();
                        SuiteInfo[] versions = (SuiteInfo[]) suiteMap.get(key);
                        sortedSuiteMap.put(((versions[0]).getName()), versions);
                    }

                    String description;
                    LSID lsid;
                    String taskType;
                    for (Iterator iter = sortedSuiteMap.keySet().iterator(); iter.hasNext(); ) {
                        String key = (String) iter.next();
                        SuiteInfo[] versions = (SuiteInfo[]) sortedSuiteMap.get(key);
                        String firstName = versions[0].getName();
                        for (int j = 0; j < versions.length; j++) {
                            si = (SuiteInfo) versions[j];
                            lsid = new LSID(si.getLSID());
                            description = si.getDescription();
                            if (description == null || description.length() == 0) description = "[no description]";
                            String indent = "";
                            String suiteName = si.getName();

                            if (j >= 1) {
                                indent = "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";
                                if (firstName.equalsIgnoreCase(suiteName)) suiteName = "";
                                out.println("");
                %>
                <tr>
                    <td valign="top">
                        <div id="<%=firstName%>" style="display:none"><%= indent %>
                            <font size='-2'><a name="<%= si.getName() %>" href=addTask.jsp?<%= GPConstants.NAME %>=<%= lsid.toString() %>&view=1" style="white-space: nowrap;"><%= si.getName() %> (<%= lsid.getVersion() %>)</a></font></div>
                    </td>
                    <td valign="top">
                        <div id="<%=firstName%>" style="display:none"><%= StringUtils.htmlEncode(description) %>
                            <br>
                                <%
		String[] docFiles = si.getDocumentationFiles();
		if (docFiles.length == 0) out.println("[no documentation]");
		for (int i = 0; i < docFiles.length; i++) {
%>
                            <a href="getTaskDoc.jsp?<%= GPConstants.NAME %>=<%= lsid %>&file=<%= URLEncoder.encode(docFiles[i]) %>"><%= docFiles[i] %>
                            </a>
                                <%
			}

out.println("</div></td></tr>");

		} else {  // Only one or first one

%>
                            <tr>
                                <td valign="top"><%= indent %>
                                    <a name="<%= si.getName() %>" href="addTask.jsp?<%= GPConstants.NAME %>=<%= lsid.toString() %>&view=1" style="white-space: nowrap;"><%= si.getName() %> (<%= lsid.getVersion() %>)<a />
                                            <%
	if (versions.length > 1){
%>
                                        <input id="<%=firstName%>cb" type="checkbox" onClick="toggleVersions('<%=firstName%>') "> all Versions</input>
                                            <%
	}
%>
                                </td>


                                <td valign="top"><%= StringUtils.htmlEncode(description) %>
                                    <br>
                                    <%
                                        String[] docFiles = si.getDocumentationFiles();
                                        if (docFiles.length == 0) out.println("[no documentation]");
                                        for (int i = 0; i < docFiles.length; i++) {
                                    %>
                                    <a href="getTaskDoc.jsp?<%= GPConstants.NAME %>=<%= lsid %>&file=<%= URLEncoder.encode(docFiles[i]) %>"><%= docFiles[i] %>
                                    </a>
                                    <%

                                                }
                                            }
                                        }
                                    %>
                                </td>
                            </tr>
                                <%

}
%>
            </tbody>
        </table>
        <jsp:include page="footer.jsp" />
    </body>
</html>
<%
        return;
    }
    si = adminClient.getSuite(name);

    if (si == null) {
        out.print("No such suite: " + name);
        return;
    }


    String filename = request.getParameter("file");
    if (filename != null && filename.length() == 0) filename = null;
    if (filename == null) {
        String[] docFiles = si.getDocumentationFiles();
        if (docFiles.length > 0) {
            filename = docFiles[0];
        }
    }
    if (filename == null) {
%>
<html>
    <head>
        <link href="skin/stylesheet.css" rel="stylesheet" type="text/css">
        <link href="skin/favicon.ico" rel="shortcut icon">
        <title>GenePattern module documentation</title>
        <meta http-equiv="content-type" content="text/html; charset=ISO-8859-1">
    </head>
    <body>
        <jsp:include page="navbar.jsp" />
        Sorry, no documentation available for <%= si.getName() %>.<br>
        <jsp:include page="footer.jsp" />
    </body>
</html>
<%
        return;
    }
    if (filename.indexOf("/") != -1) filename = filename.substring(filename.indexOf("/") + 1);
    String suiteLibDir = DirectoryManager.getSuiteLibDir(si.getName(), si.getLSID(), userID);
    int i = filename.lastIndexOf(".");

    File in = new File(suiteLibDir, filename);
    if (!in.exists()) {
%>
<html>
    <head>
        <link href="skin/stylesheet.css" rel="stylesheet" type="text/css">
        <link href="skin/favicon.ico" rel="shortcut icon">
        <title>GenePattern suite documentation</title>
        <meta http-equiv="content-type" content="text/html; charset=ISO-8859-1">
    </head>
    <body>
        <jsp:include page="navbar.jsp" />
        Sorry, no such file <%= filename %> for <%= si.getName() %>.<br>
        <jsp:include page="footer.jsp" />
    </body>
</html>
<%
        return;
    }

    String contentType = new File(filename).toURL().openConnection().getFileNameMap().getContentTypeFor(filename);
    if (contentType == null) {
        final Hashtable htTypes = new Hashtable();
        htTypes.put(".jar", "application/java-archive");
        htTypes.put(".zip", "application/zip");
        htTypes.put("." + GPConstants.TASK_TYPE_PIPELINE, "text/plain");
        htTypes.put(".class", "application/octet-stream");
        htTypes.put(".doc", "application/msword");

        i = filename.lastIndexOf(".");
        String extension = (i > -1 ? filename.substring(i) : "");
        contentType = (String) htTypes.get(extension.toLowerCase());
    }
    if (contentType == null) contentType = "text/plain";

    contentType = contentType + "; name=\"" + filename + "\";";
    response.addHeader("Content-Disposition", "attachment; filename=\"" + filename + "\";");
    response.setContentType(contentType);
    FileInputStream ins = new java.io.FileInputStream(in);
    int c = 0;
    while ((c = ins.read()) != -1) {
        out.write(c);
    }
    ins.close();
    ins = null;

    return;
%>