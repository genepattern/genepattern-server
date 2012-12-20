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
    TaskInfo ti;
    TaskInfoAttributes tia;

    if (true) {
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

                    Collection tasks = adminClient.getTaskCatalog();
                    HashMap taskMap = new HashMap();
                    for (Iterator iter = tasks.iterator(); iter.hasNext(); ) {
                        ti = (TaskInfo) iter.next();
                        LSID lsid = new LSID((String) ti.giveTaskInfoAttributes().get(GPConstants.LSID));
                        ArrayList versions = (ArrayList) taskMap.get(lsid.toStringNoVersion());
                        if (versions == null) {
                            versions = new ArrayList();
                            taskMap.put(lsid.toStringNoVersion(), versions);
                        }
                        versions.add(ti);
                    }
                    for (Iterator iter = taskMap.keySet().iterator(); iter.hasNext(); ) {
                        String key = (String) iter.next();
                        ArrayList versions = (ArrayList) taskMap.get(key);
                        TaskInfo[] sortedVersions = (TaskInfo[]) versions.toArray(new TaskInfo[versions.size()]);
                        Arrays.sort(sortedVersions, new Comparator() {
                            public int compare(Object o1, Object o2) {
                                TaskInfo t1 = (TaskInfo) o1;
                                TaskInfo t2 = (TaskInfo) o2;

                                LSID l1, l2;
                                try {
                                    l1 = new LSID((String) t1.giveTaskInfoAttributes().get(GPConstants.LSID));
                                    l2 = new LSID((String) t2.giveTaskInfoAttributes().get(GPConstants.LSID));
                                    return l2.getVersion().compareToIgnoreCase(l1.getVersion());

                                } catch (MalformedURLException mue) {
                                    // ignore
                                    return 0;
                                }
                            }
                        });
                        taskMap.put(key, sortedVersions);
                    }


                    TreeMap sortedTaskMap = new TreeMap(new Comparator() {
                        public int compare(Object o1, Object o2) {
                            String k1 = (String) o1;
                            String k2 = (String) o2;
                            return k1.compareToIgnoreCase(k2);
                        }
                    });


                    for (Iterator iter = taskMap.keySet().iterator(); iter.hasNext(); ) {
                        String key = (String) iter.next();
                        TaskInfo[] versions = (TaskInfo[]) taskMap.get(key);
                        sortedTaskMap.put(((versions[0]).getName()), versions);
                    }

                    String description;
                    LSID lsid;
                    String taskType;
                    for (Iterator iter = sortedTaskMap.keySet().iterator(); iter.hasNext(); ) {
                        String key = (String) iter.next();
                        TaskInfo[] versions = (TaskInfo[]) sortedTaskMap.get(key);
                        String firstName = versions[0].getName();
                        for (int j = 0; j < versions.length; j++) {
                            ti = (TaskInfo) versions[j];
                            tia = ti.giveTaskInfoAttributes();
                            lsid = new LSID(tia.get(GPConstants.LSID));
                            taskType = tia.get(GPConstants.TASK_TYPE);
                            description = ti.getDescription();
                            if (description == null || description.length() == 0) description = "[no description]";
                            boolean isPipeline = taskType.equals(GPConstants.TASK_TYPE_PIPELINE);
                            String indent = "";
                            String taskName = ti.getName();

                            if (j >= 1) {
                                indent = "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";
                                if (firstName.equalsIgnoreCase(taskName)) taskName = "";
                                out.println("");
                %>
                <tr>
                    <td valign="top">
                        <div id="<%=firstName%>" style="display:none"><%= indent %>
                            <font size='-2'><a name="<%= ti.getName() %>" href="<%= !isPipeline ? "addTask.jsp" : "pipeline/index.jsf" %>?lsid=<%= lsid.toString() %>&view=1" style="white-space: nowrap;"><%= ti.getName() %> (<%= lsid.getVersion() %>)</a></font></div>
                    </td>
                    <td valign="top">
                        <div id="<%=firstName%>" style="display:none"><%= StringUtils.htmlEncode(description) %>
                            <br>
                                <%
				File[] docFiles = taskIntegratorClient.getDocFiles(ti);
				if (docFiles.length == 0) out.println("[no documentation]");
				for (int i = 0; i < docFiles.length; i++) {
		%>
                            <a href="getTaskDoc.jsp?<%= GPConstants.NAME %>=<%= lsid %>&file=<%= URLEncoder.encode(docFiles[i].getName()) %>"><%= docFiles[i].getName() %>
                            </a>
                                <%
					}

		out.println("</div></td></tr>");

				} else {  // Only one or first one

		%>
                            <tr>
                                <td valign="top"><%= indent %>
                                    <a name="<%= ti.getName() %>" href="<%= !isPipeline ? "addTask.jsp" : "pipeline/index.jsf" %>?lsid=<%= lsid.toString() %>&view=1" style="white-space: nowrap;"><%= ti.getName() %> (<%= lsid.getVersion() %>)<a />
                                            <%
			if (versions.length > 1){
		%>
                                        <input id="<%=firstName%>cb" type="checkbox" onClick="toggleVersions('<%=firstName%>') "> Show all versions</input>
                                            <%
			}
		%>
                                </td>


                                <td valign="top"><%= StringUtils.htmlEncode(description) %>
                                    <br>
                                    <%
                                        File[] docFiles = taskIntegratorClient.getDocFiles(ti);
                                        if (docFiles.length == 0) out.println("[no documentation]");
                                        for (int i = 0; i < docFiles.length; i++) {
                                    %>
                                    <a href="getTaskDoc.jsp?<%= GPConstants.NAME %>=<%= lsid %>&file=<%= URLEncoder.encode(docFiles[i].getName()) %>"><%= docFiles[i].getName() %>
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
%>