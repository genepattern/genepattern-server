<%--
  ~ Copyright 2012 The Broad Institute, Inc.
  ~ SOFTWARE COPYRIGHT NOTICE
  ~ This software and its documentation are the copyright of the Broad Institute, Inc. All rights are reserved.
  ~
  ~ This software is supplied without any warranty or guaranteed support whatsoever. The Broad Institute is not responsible for its use, misuse, or functionality.
  --%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@ page import="org.genepattern.webservice.TaskInfo,
                 org.genepattern.webservice.TaskInfoAttributes,
                 org.genepattern.webservice.ParameterInfo,
                 org.genepattern.webservice.ParameterFormatConverter,
                 org.genepattern.webservice.OmnigeneException,
                 org.genepattern.server.util.*,
                 org.genepattern.server.genepattern.GenePatternAnalysisTask,
                 org.genepattern.server.genepattern.LSIDManager,
                 org.genepattern.server.webservice.server.local.*,
                 org.genepattern.util.LSID,
                 org.genepattern.util.StringUtils,
                 org.genepattern.util.LSIDUtil,
                 org.genepattern.util.GPConstants,
                 org.genepattern.data.pipeline.PipelineModel,
                 org.genepattern.server.webapp.jsf.AuthorizationHelper,
                 java.io.File,
                 java.io.FilenameFilter,
                 java.net.MalformedURLException,
                 java.net.URLEncoder,
                 java.util.Arrays,
                 java.util.Collection,
                 java.util.Collections,
                 java.util.Comparator,
                 java.util.HashMap,
                 java.util.HashSet,
                 java.util.Iterator,
                 java.util.Properties,
                 java.util.List,
                 java.util.Set,
                 java.util.TreeSet,
                 java.util.TreeMap,
                 java.util.Vector,
                 org.genepattern.server.config.GpContext,
                 org.genepattern.server.eula.EulaManager,
                 org.genepattern.server.eula.EulaInfo"
         session="false" contentType="text/html" language="Java" %>
<%
    String ua = request.getHeader("User-Agent");
    boolean isMSIE = (ua != null && ua.indexOf("MSIE") != -1);
    response.setHeader("Vary", "User-Agent");

    try {
        response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
        response.setHeader("Pragma", "no-cache");         // HTTP 1.0 cache control
        response.setDateHeader("Expires", 0);

        String userID = (String) request.getAttribute("userID");

        boolean createModuleAllowed = AuthorizationHelper.createModule(userID);

        LocalTaskIntegratorClient taskIntegratorClient = new LocalTaskIntegratorClient(userID, out);
        LocalAdminClient adminClient = new LocalAdminClient(userID);
// drop-down selection lists
        String[] taskTypes = GenePatternAnalysisTask.getTaskTypes();
        String[] cpuTypes = GenePatternAnalysisTask.getCPUTypes();
        String[] oses = GenePatternAnalysisTask.getOSTypes();
        String[] languages = GenePatternAnalysisTask.getLanguages();
        String[] qualities = GPConstants.QUALITY_LEVELS;
        String[] privacies = GPConstants.PRIVACY_LEVELS;


        int NUM_ATTACHMENTS = 5;
        int NUM_PARAMETERS = 5;
        String DELETE = "Delete";
        String CLONE = "Clone";
        String RUN = "Run";
        String taskName = request.getParameter(GPConstants.NAME);
        String attributeName = null;
        String attributeValue = null;
        String attributeType = null;
        boolean viewOnly = (request.getParameter("view") != null);

        TaskInfo taskInfo = null;
        ParameterInfo[] parameterInfoArray = null;
        TaskInfoAttributes tia = null;

        if (taskName != null && taskName.length() == 0) taskName = null;

        if ((taskName == null) && (!createModuleAllowed)) {
            response.sendRedirect("pages/notPermitted.jsf");
            return;
        }

        Vector errors = (Vector) request.getAttribute("errors");
        if (errors != null) {
            taskInfo = (TaskInfo) request.getAttribute("taskInfo");
            tia = taskInfo.giveTaskInfoAttributes();
            parameterInfoArray = taskInfo.getParameterInfoArray();
            taskName = (String) request.getAttribute("taskName"); // task name entered by user

        } else if (taskName != null) {
            try {
                taskInfo = GenePatternAnalysisTask.getTaskInfo(taskName, userID);
                if (taskInfo != null) {
                    taskName = taskInfo.getName();
                    parameterInfoArray = ParameterFormatConverter.getParameterInfoArray(taskInfo.getParameterInfo());
                    tia = taskInfo.giveTaskInfoAttributes();
                    LSID lsid = new LSID((String) tia.get(GPConstants.LSID));
                    boolean editable = createModuleAllowed && taskInfo.getUserId().equals(userID) && LSIDUtil.isAuthorityMine(taskInfo.getLsid());
                    viewOnly = viewOnly || !editable;

                    if (!isMSIE && !viewOnly) {
                        response.sendRedirect("modules/creator.jsf?lsid=" + lsid);
                    }
                } else {
%>
<script language="javascript">
    window.alert("<%= taskName %> does not exist");
</script>
<%
                taskName = null;
            }
        } catch (OmnigeneException oe) {
        }
    }

    if (isMSIE && !viewOnly) {
%>
<script language="javascript">
    window.alert("The new Module Integrator version 2.0 is not compatible with this browser. " +
            "You are being redirected to the previous version of the Module Integrator. To use the new " +
            "Module Integrator please switch to either Firefox, Chrome, or Safari.");
</script>
<%
    }
    TreeMap tmFileFormats = new TreeMap(String.CASE_INSENSITIVE_ORDER);

    int FILE_FORMAT_PARAM_OFFSET = -1;
    for (int j = 0; j < GPConstants.PARAM_INFO_ATTRIBUTES.length; j++) {
        if (GPConstants.PARAM_INFO_ATTRIBUTES[j] == GPConstants.PARAM_INFO_FILE_FORMAT) {
            FILE_FORMAT_PARAM_OFFSET = j;
            break;
        }
    }

    Collection tmTasks = adminClient.getTaskCatalog();
    TreeSet tsTaskTypes = new TreeSet(String.CASE_INSENSITIVE_ORDER);

// well-known task types, regardless of domain
    tsTaskTypes.add(""); // blank entry at top of list
    tsTaskTypes.add(GPConstants.TASK_TYPE_PIPELINE);
    tsTaskTypes.add(GPConstants.TASK_TYPE_VISUALIZER);

    for (Iterator itTasks = tmTasks.iterator(); itTasks.hasNext(); ) {
        TaskInfo ti = (TaskInfo) itTasks.next();
        TaskInfoAttributes tia2 = ti.giveTaskInfoAttributes();
        if (tia2 == null) continue;
        boolean isPrivate = tia2.get(GPConstants.PRIVACY).equals(GPConstants.PRIVATE);
        boolean isMine = tia2.get(GPConstants.PRIVACY).equals(GPConstants.PUBLIC) || tia2.get(GPConstants.USERID).equals(userID);
        String owner = tia2.get(GPConstants.USERID);
        if (!tsTaskTypes.contains(tia2.get(GPConstants.TASK_TYPE))) {
            tsTaskTypes.add(tia2.get(GPConstants.TASK_TYPE));
        }
    }
    taskTypes = (String[]) tsTaskTypes.toArray(new String[0]);

%>
<html>
    <head>
        <link href="skin/stylesheet.css" rel="stylesheet" type="text/css">
        <link href="css/style.css" rel="stylesheet" type="text/css">
        <link href="skin/favicon.ico" rel="shortcut icon">

        <title><%= taskName == null ? "add GenePattern module" : ((!viewOnly ? "update " : "") + taskName + " version " + new LSID(tia.get(GPConstants.LSID)).getVersion()) %>
        </title>
        <% if (viewOnly) { %>
        <style>.hideable {
            border-style: none;
            readonly: true;
        }</style>
        <% } %>
        <meta http-equiv="content-type" content="text/html; charset=ISO-8859-1">

        <script language="JavaScript" src="<%=request.getContextPath()%>/js/collapsiblePanel.js"></script>
        <script type="text/javascript" language="javascript">

            function showFileFormats(sel, i) {
                var val = sel.value;
                var div = document.getElementById("p" + i + "_fileFormatDiv");
                if (val == "java.io.File") {
                    div.style.display = "block";
                } else {
                    div.style.display = "none";
                }
            }

            function onPrivacyChange(selector) {
                if (selector.options[selector.selectedIndex].value == '<%= GPConstants.PRIVATE %>') {
                    // changing from public to private
                    document.forms['task'].<%= GPConstants.USERID %>.value = '<%= userID %>';
                } else {
                    // changing from private to public
                }
            }

            function confirmDeleteSupportFiles() {
                var sel = document.forms['task'].deleteFiles;
                var selection = sel.options[sel.selectedIndex].value;
                if (selection == null || selection == "") return;
                if (window.confirm('Really delete ' + selection + ' from ' + document.forms['task']['<%= GPConstants.FORMER_NAME %>'].value + '\'s support files?\nThis will discard other changes since the last save.')) {
                    sel.form.deleteSupportFiles.value = "1";
                    sel.form.submit();
                }
            }


            <% if (taskInfo != null) { %>
            function cloneTask() {
                var cloneName = window.prompt("Name for cloned module", "copyOf<%= taskName %>");
                if (cloneName == null || cloneName.length == 0) {
                    return;
                }
                window.location = "saveTask.jsp?clone=1&<%= GPConstants.NAME %>=<%= taskName %>&<%= GPConstants.LSID %>=<%= tia.get(GPConstants.LSID) %>&cloneName=" + cloneName + "&<%= GPConstants.USERID %>=<%= userID %>";

            }

            function runTask() {
                window.location = "<%= request.getContextPath() %>/pages/index.jsf?lsid=<%= tia.get(GPConstants.LSID) %>";
            }

            <% } %>

            function addNewTaskType() {
                var newTaskType = window.prompt("new module category", "");
                if (newTaskType == null || newTaskType == "") return;
                var fld = document.forms['task'].<%= GPConstants.TASK_TYPE %>;
                var n = fld.options.length;
                var found = false;
                for (i = 0; i < n; i++) {
                    if (fld.options[i].text == newTaskType) {
                        found = true;
                        fld.options.selectedIndex = i;
                        return;
                    }
                }
                fld.options[n] = new Option(newTaskType, newTaskType);
                fld.options.selectedIndex = n;
            }

            function addNewFileType(name, desc) {
                if (name == null || name == "") return;
                if (desc == "") desc = name;
                var frm = document.forms['task'];
                var fld = frm.<%= GPConstants.FILE_FORMAT %>;
                var n = fld.options.length;
                var found = false;
                for (i = 0; i < n; i++) {
                    if (fld.options[i].text == name) {
                        fld.options[i].selected = true;
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    fld.options[n] = new Option(name, desc, true, true);
                    for (i = 0; i < <%= GPConstants.MAX_PARAMETERS %>; i++) {
                        fld = frm["p" + i + "_<%= GPConstants.PARAM_INFO_FILE_FORMAT[GPConstants.PARAM_INFO_NAME_OFFSET] %>"];
                        fld.options[fld.options.length] = new Option(name, desc);
                    }
                }
            }


        </script>
        <jsp:include page="navbarHead.jsp" />
    </head>

    <body>
        <jsp:include page="navbar.jsp" />


        <% if (taskName != null && taskInfo == null) { %>
        <script language="javascript">
            alert('no such module <%= taskName %>');
        </script>
        <%
                taskName = null;
            }

            StringBuffer publicTasks = new StringBuffer();
            String name;
            String description;
            String lsid;
            StringBuffer otherTasks = new StringBuffer();
            String DONT_JUMP = "dontJump";

            // used to avoid displaying multiple versions of same basic task
            HashMap hmLSIDsWithoutVersions = new HashMap();

            // used to track multiple versions of current task
            Vector vVersions = new Vector();
            LSID l = null;
            String thisLSIDNoVersion = "";
            if (tia != null) {
                try {
                    lsid = tia.get(GPConstants.LSID);
                    if (lsid != null && !lsid.trim().equals("")) {
                        thisLSIDNoVersion = new LSID(lsid).toStringNoVersion();
                    }
                } catch (MalformedURLException mue) {
                }
            }

            String authorityType = null;

            for (Iterator itTasks = tmTasks.iterator(); itTasks.hasNext(); ) {
                TaskInfo ti = (TaskInfo) itTasks.next();
                name = ti.getName();
                description = ti.getDescription();
                TaskInfoAttributes tia2 = ti.giveTaskInfoAttributes();
                if (tia2 == null) continue;

                String fileFormat = tia2.get(GPConstants.FILE_FORMAT);
                if (fileFormat != null && fileFormat.length() > 0) {
                    String[] fileFormats = fileFormat.split(GPConstants.PARAM_INFO_CHOICE_DELIMITER);
                    for (int y = 0; y < fileFormats.length; y++) {
                        tmFileFormats.put(fileFormats[y], fileFormats[y]);
                    }
                }
                ParameterInfo[] pia = ParameterFormatConverter.getParameterInfoArray(ti.getParameterInfo());
                if (pia != null) {
                    for (int pNum = 0; pNum < pia.length; pNum++) {
                        HashMap pAttributes = pia[pNum].getAttributes();

                        fileFormat = (String) pAttributes.get(GPConstants.FILE_FORMAT);
                        if (fileFormat != null && fileFormat.length() > 0) {
                            String[] fileFormats = fileFormat.split(GPConstants.PARAM_INFO_CHOICE_DELIMITER);
                            for (int y = 0; y < fileFormats.length; y++) {
                                tmFileFormats.put(fileFormats[y], fileFormats[y]);
                            }
                        }
                    }
                }

                lsid = tia2.get(GPConstants.LSID);
                try {
                    l = new LSID(lsid);
                    String versionlessLSID = l.toStringNoVersion();
                    if (versionlessLSID.equals(thisLSIDNoVersion)) {
                        vVersions.add(lsid);
                    }

                    versionlessLSID = l.toStringNoVersion();
                    String key = versionlessLSID + "." + name;
                    if (hmLSIDsWithoutVersions.containsKey(key) &&
                            ((TaskInfo) hmLSIDsWithoutVersions.get(key)).getName().equals(name)) {
                        continue;
                    }
                    hmLSIDsWithoutVersions.put(key, ti);
                    authorityType = LSIDManager.getAuthorityType(l);
                } catch (MalformedURLException mue) {
                    l = null;
                }
                boolean bMine = tia2.get(GPConstants.PRIVACY).equals(GPConstants.PUBLIC) || tia2.get(GPConstants.USERID).equals(userID);
                String owner = tia2.get(GPConstants.USERID);
                if (owner != null && owner.indexOf("@") != -1) owner = " (" + owner.substring(0, owner.indexOf("@")) + ")";
                StringBuffer sb = bMine ? publicTasks : otherTasks;

                String n = (lsid != null ? lsid : name);
                if (n == null) n = "";
                sb.append("<option value=\"" + n + "\"" + (tia != null && n.equals((String) tia.get(GPConstants.LSID)) ? " selected" : "") +
                        " class=\"tasks-" + authorityType + "\">" +
                        (lsid != null ? ti.getName() : name) + (!bMine ? owner : "") +
                        (authorityType.equals(LSIDUtil.AUTHORITY_FOREIGN) ? (" (" + l.getAuthority() + ")") : "") +
                        "</option>\n");
            }

            Collections.sort(vVersions, new Comparator() {
                public int compare(Object v1, Object v2) {
                    try {
                        LSID lsid1 = new LSID((String) v1);
                        LSID lsid2 = new LSID((String) v2);
                        return lsid1.compareTo(lsid2);
                    } catch (MalformedURLException mue) {
                        // ignore
                        return 0;
                    }
                }
            });

            String[][] fileFormats = new String[tmFileFormats.size()][2];
            int i = 0;
            for (Iterator itFileFormat = tmFileFormats.keySet().iterator(); itFileFormat.hasNext(); i++) {
                String key = (String) itFileFormat.next();
                fileFormats[i] = new String[]{key, key};
            }
            GPConstants.PARAM_INFO_ATTRIBUTES[FILE_FORMAT_PARAM_OFFSET][GPConstants.PARAM_INFO_CHOICE_TYPES_OFFSET] = fileFormats;

            i = 0;

            //GPConstants.PARAM_INFO_ATTRIBUTES[DOMAIN_PARAM_OFFSET][GPConstants.PARAM_INFO_CHOICE_TYPES_OFFSET] = domains;

            if (tia != null) {
                lsid = tia.get(GPConstants.LSID);
            } else {
                lsid = "";
            }

        %>

        <table border="0" cellpadding="0" cellspacing="0" width="100%">
            <tbody>
                <tr>
                    <td class='barhead-other'>
                        <%= taskName == null ? "Create Module" : ((!viewOnly ? "Update " : "") + taskName + " version ") %>
                        <% if (taskName != null) { %>
                        <select name="notused" onchange="javascript:window.location='addTask.jsp?<%= GPConstants.NAME %>=' + this.options[this.selectedIndex].value + '<%= viewOnly ? "&view=1" : "" %>'" style="font-weight: bold; font-size: medium; outline-style: none;">
                            <%
                                for (Iterator itVersions = vVersions.iterator(); itVersions.hasNext(); ) {
                                    String vLSID = (String) itVersions.next();
                                    l = new LSID(vLSID);
                            %>
                            <option value="<%= l.toString() %>"<%= vLSID.equals(lsid) ? " selected" : "" %>><%= l.getVersion() %>
                            </option>
                            <%
                                }
                            %>
                        </select>
                        <% } %>

                    </td>
                </tr>
            </tbody>
        </table>
        <%
            if (errors != null) {
        %>
        <font color="red">
            <h2>
                There are some problems with the module that need to be fixed:
            </h2>
            <ul>
                <%
                    for (java.util.Enumeration eProblems = errors.elements(); eProblems.hasMoreElements(); ) {
                %>
                <li><%= StringUtils.htmlEncode((String) eProblems.nextElement()) %>
                </li>
                <%
                        }
                    }
                %>
            </ul>
        </font>

        <form name="task" action="saveTask.jsp" method="post" ENCTYPE="multipart/form-data">
            <input type="hidden" name="<%= GPConstants.FORMER_NAME %>" value="<%= taskInfo != null ? taskInfo.getName() : "" %>">


            <table cols="2" valign="top" width="100%">
                <tr class="taskperameter">
                    <td>* required field</td>
                    <td><a href='modules/createhelp.jsp' target='help'><img border='0' src='images/help2.jpg' /></a></td>
                </tr>
                <tr class="taskperameter" title="Module name without spaces, used as the name by which the module will be invoked.">
                    <td valign="top">Name:*</td>
                    <td><% if (!viewOnly) { %><input name="<%= GPConstants.NAME %>" maxlength="100" size="<%= taskInfo != null ? Math.max(taskInfo.getName().length() + 2, 20): 20 %>"
                                                     value="<%= taskInfo != null ? taskInfo.getName() : "" %>" xonblur="onTaskNameLostFocus(this)"> * (required, no spaces)<a href='modules/createhelp.jsp#Name_brief' target='help'><img border='0' src='images/help2.jpg' /></a><% } else { %><%= taskInfo.getName() %><% } %>
                        &nbsp;&nbsp;&nbsp;&nbsp;


                        <% if (taskInfo != null && !viewOnly && errors == null) { %>
                        <div title="Delete <%=taskInfo.getName() %> " style="display:inline">
                            <input type="button" value="<%= DELETE %>..." name="<%= DELETE %>" class="little"
                                   onclick="if (window.confirm('Really delete the ' + document.forms['task'].<%= GPConstants.NAME %>.value + ' task?')) { window.location='saveTask.jsp?delete=1&<%= GPConstants.NAME %>=' + document.forms['task'].<%= GPConstants.NAME %>.value + '&<%= GPConstants.LSID %>=' + document.forms['task'].<%= GPConstants.LSID %>.value; }">
                        </div>
                        <% }
                            if (taskInfo != null && errors == null) { %>
                        <div title="Run <%=taskInfo.getName() %>" style="display:inline">
                            <input type="button" value="<%= RUN %>" name="<%= RUN %>" class="little" onclick="runTask()">
                        </div>
                        <%
                            if (createModuleAllowed) { %>
                        <div title="Create a copy (clone) of <%=taskInfo.getName() %> you can edit" style="display:inline">
                            <input type="button" value="<%= CLONE %>..." name="<%= CLONE %>" class="little" onclick="cloneTask()">
                        </div>
                        <% }
                        } %>


                        &nbsp;&nbsp;&nbsp;
                    </td>
                </tr>

                <tr class="taskperameter" title="LSID">
                    <td valign="top">LSID:</td>
                    <td>
                        <% if (!viewOnly) { %>
                        <input type="text" name="<%= GPConstants.LSID %>" value="<%= taskInfo != null ? StringUtils.htmlEncode(tia.get(GPConstants.LSID)) : "" %>" size="100" readonly style="{ border-style: none; }">
                        <% } else {
                            out.print(taskInfo != null ? StringUtils.htmlEncode(tia.get(GPConstants.LSID)) : "");
                        }
                        %>
                    </td>
                </tr>

                <tr class="taskperameter" title="A verbose description of the purpose of the program, especially useful to someone who hasn't run the program before to determine whether it is suited to their problem.">
                    <td valign="top">Description:</td>
                    <td>
                        <% if (!viewOnly) { %>
                        <input name="<%= GPConstants.DESCRIPTION %>" size="80" class="hideable"
                               value="<%= taskInfo != null ? StringUtils.htmlEncode(taskInfo.getDescription()) : "" %>">
                        <%
                            } else {
                                out.print(taskInfo != null ? StringUtils.htmlEncode(taskInfo.getDescription()) : "");
                            }
                        %>
                        <a href='modules/createhelp.jsp#Description_brief' target='help'><img border='0' src='images/help2.jpg' /></a>
                    </td>
                </tr>

                <tr class="taskperameter" title="Author's name, affiliation, email address">
                    <td valign="top">Author:</td>
                    <td>
                        <%
                            if (!viewOnly) { %>
                        <input name="<%= GPConstants.AUTHOR %>" size="80" class="hideable"
                               value="<%= taskInfo != null ? StringUtils.htmlEncode(tia.get(GPConstants.AUTHOR)) : "" %>"> (name, affiliation)
                        <% } else {
                            out.print(taskInfo != null ? StringUtils.htmlEncode(tia.get(GPConstants.AUTHOR)) : "");
                        } %>
                        <a href='modules/createhelp.jsp#Author_brief' target='help'><img border='0' src='images/help2.jpg' /></a>

                    </td>
                </tr>
                <tr class="taskperameter" title="Make available to others">
                    <td valign="top">Privacy:</td>
                    <td><%= createSelection(tia, GPConstants.PRIVACY, privacies, "onchange=\"onPrivacyChange(this)\"", viewOnly) %>
                        <a href='modules/createhelp.jsp#Privacy_brief' target='help'><img border='0' src='images/help2.jpg' /></a>
                    </td>
                </tr>

                <tr class="taskperameter">
                    <td valign="top">
                        License:
                    </td>
                    <td>
                        <%
                            GpContext taskContext = GpContext.getContextForUser(userID);
                            if (taskContext != null) {
                                List<EulaInfo> eulas = EulaManager.instance(taskContext).getEulas(taskInfo);
                                if (eulas != null && eulas.size() != 0) {
                        %> <a href="<%=eulas.get(0).getLink(request.getContextPath())%>" target="new"><%=eulas.get(0).getLicense() %>
                    </a>
                        <%
                                }
                            }
                        %>
                        <a href='modules/createhelp.jsp#License_brief' target='help'><img border='0' src='images/help2.jpg' /></a>
                    </td>
                </tr>
                <tr class="taskperameter" title="Readiness for use by others">
                    <td valign="top">Quality&nbsp;level:</td>
                    <td><%= createSelection(tia, GPConstants.QUALITY, qualities, "", viewOnly) %>
                        <a href='modules/createhelp.jsp#Quality_brief' target='help'><img border='0' src='images/help2.jpg' /></a>
                    </td>
                </tr>

                <%
                    if (taskName != null) {
                        File[] docFiles = null;
                        try {
                            docFiles = taskIntegratorClient.getDocFiles(taskInfo);
                        } catch (org.genepattern.webservice.WebServiceException wse) {
                            docFiles = new File[0];
                        }
                %>
                <tr class="taskperameter">
                    <td valign="top"><%
                        boolean isPipeline = tia != null && tia.get(GPConstants.TASK_TYPE).equals(GPConstants.TASK_TYPE_PIPELINE);
                        boolean hasDoc = docFiles != null && docFiles.length > 0;
                        if (hasDoc || isPipeline) {
                    %>Documentation:
                    </td>
                    <td width="*"><%
                        }
                        if (hasDoc) {
                            for (i = 0; i < docFiles.length; i++) { %>
                        <a href="getTaskDoc.jsp?<%= GPConstants.NAME %>=<%= StringUtils.htmlEncode(request.getParameter(GPConstants.NAME)) %>&file=<%= URLEncoder.encode(docFiles[i].getName()) %>" target="new"><%= StringUtils.htmlEncode(docFiles[i].getName()) %>
                        </a>
                        <% }
                        }
                            if (isPipeline) {
                        %>
                        <a href="pipeline/index.jsf?lsid=<%= tia.get(GPConstants.LSID) %>">pipeline designer</a>
                        <input name="<%= PipelineModel.PIPELINE_MODEL %>" type="hidden" value="<%= StringUtils.htmlEncode(tia.get(PipelineModel.PIPELINE_MODEL)) %>">
                        <%
                            }
                        %>
                    </td>
                </tr>
                <% } %>

                <tr class="taskperameter" title="the command line used to invoke the application, using &lt;tags&gt; for param &amp; environment variable substitutions.">
                    <td valign="top">Command&nbsp;line:*<a href='modules/createhelp.jsp#Command_brief' target='help'><img border='0' src='images/help2.jpg' /></a></td>

                    <td valign="top">
                        <% if (!viewOnly) { %><textarea name="<%= GPConstants.COMMAND_LINE %>" cols="90" rows="5"><% } %><%= tia != null ? StringUtils.htmlEncode(tia.get(GPConstants.COMMAND_LINE)) : "" %><% if (!viewOnly) { %></textarea><a href='modules/createhelp.jsp#Command_brief' target='help'><img border='0' src='images/help2.jpg' /></a>
                        <% } %>

                    </td>

                </tr>

                <tr class="taskperameter">
                    <td valign="top">Module&nbsp;Category:
                    </td>
                    <td>
                        <%= createSelection(tia, GPConstants.TASK_TYPE, taskTypes, "", viewOnly) %>
                        <% if (!viewOnly) { %>
                        <input type="button" onclick="addNewTaskType()" value="New..." class="little">
                        <% } %>
                        <a href='modules/createhelp.jsp#TaskType_brief' target='help'><img border='0' src='images/help2.jpg' /></a>

                    </td>
                </tr>

                <tr class="taskperameter">
                    <td valign="top">CPU&nbsp;type:</td>
                    <td>
                        <%= createSelection(tia, GPConstants.CPU_TYPE, cpuTypes, "", viewOnly) %> (if compiled for a specific one)
                        <a href='modules/createhelp.jsp#cpu_brief' target='help'><img border='0' src='images/help2.jpg' /></a>
                    </td>
                </tr>

                <tr class="taskperameter">
                    <td valign="top">Operating&nbsp;system:</td>
                    <td>
                        <%= createSelection(tia, GPConstants.OS, oses, "", viewOnly) %> (if operating system-dependent)
                        <a href='modules/createhelp.jsp#os_brief' target='help'><img border='0' src='images/help2.jpg' /></a>
                    </td>
                </tr>

                <%--
           <tr>
          <td align="right"><b>Java&nbsp;JVM&nbsp;level:</b></td>
          <td width="*">
            <%= createSelection(tia, GPConstants.JVM_LEVEL, jvms, "", viewOnly) %> (if Java is used)
                 </td>
           </tr>
        --%>
                <tr class="taskperameter">
                    <td valign="top">Language:</td>
                    <td>
                        <%= createSelection(tia, GPConstants.LANGUAGE, languages, "", viewOnly) %> &nbsp;
                        <b>min. language version:</b> <% if (!viewOnly) { %><input name="<%= GPConstants.JVM_LEVEL %>" value="<%= tia != null ? StringUtils.htmlEncode(tia.get(GPConstants.JVM_LEVEL)) : "" %>" size="10"><% } else { %><%= tia != null ? StringUtils.htmlEncode(tia.get(GPConstants.JVM_LEVEL)) : "" %><% } %>
                        <a href='modules/createhelp.jsp#Language_brief' target='help'><img border='0' src='images/help2.jpg' /></a>
                    </td>
                </tr>

                <tr class="taskperameter">
                    <td valign="top">Version&nbsp;comment:</td>
                    <td>
                        <% if (!viewOnly) { %><textarea name="<%= GPConstants.VERSION %>" cols="50" rows="1"><% } %><%= taskInfo != null ? StringUtils.htmlEncode(tia.get(GPConstants.VERSION)) : "" %><% if (!viewOnly) { %></textarea><% } %>
                        <a href='modules/createhelp.jsp#VersionComment_brief' target='help'><img border='0' src='images/help2.jpg' /></a>
                    </td>
                </tr>

                <tr class="taskperameter">
                    <td> File format(s):<a href='modules/createhelp.jsp#OutputDescription_brief' target='help'><img border='0' src='images/help2.jpg' /></a></td>
                    <td>
                        <table>
                            <tr>

                                <td valign="top">
                                    <%
                                        attributeValue = (tia != null ? tia.get(GPConstants.FILE_FORMAT) : "");
                                        if (attributeValue == null) attributeValue = "";
                                    %>
                                    <% if (!viewOnly) {
                                        String[] file_formats = attributeValue.split(GPConstants.PARAM_INFO_CHOICE_DELIMITER);
                                        String[][] choices = (String[][]) GPConstants.PARAM_INFO_ATTRIBUTES[FILE_FORMAT_PARAM_OFFSET][GPConstants.PARAM_INFO_CHOICE_TYPES_OFFSET];
                                    %>
                                    <select multiple name="<%= GPConstants.FILE_FORMAT %>" size="<%= Math.min(3, tmFileFormats.size()) %>">
                                        <%
                                            for (Iterator itChoices = tmFileFormats.values().iterator(); itChoices.hasNext(); ) {
                                                String c = (String) itChoices.next();
                                                boolean isSelected = false;
                                                for (i = 0; i < file_formats.length; i++) {
                                                    if (c.equals(file_formats[i])) {
                                                        isSelected = true;
                                                        break;
                                                    }
                                                }
                                                out.println("<option value=\"" + c + "\"" + (isSelected ? " selected" : "") + ">" + StringUtils.htmlEncode(c) + "</option>");
                                            }
                                        %>
                                    </select>
                                    <% } else { %>
                                    <%= attributeValue %>
                                    <% } %>
                                </td>
                                <% if (!viewOnly) { %>
                                <td valign="top">
                                    <input type="button" onclick="window.open('pages/newFileType.jsf', 'Add New File Type','toolbar=no, location=no, status=no, resizable=yes, scrollbars=yes, menubar=no, width=300, height=200');" value="New..." class="little">
                                </td>
                                <% }%>
                                <!--	 <td valign="top">
		domain(s):
		</td>
		<td valign="top">
	<%
			//attributeValue = (tia != null ? tia.get(GPConstants.DOMAIN) : "");
			//if (attributeValue == null) attributeValue = "";
	%>
	  <% //if (!viewOnly) { %>

	<%
		/*{
			String[] taskDomains = attributeValue.split(GPConstants.PARAM_INFO_CHOICE_DELIMITER);
			String[][] choices = (String[][])GPConstants.PARAM_INFO_ATTRIBUTES[DOMAIN_PARAM_OFFSET][GPConstants.PARAM_INFO_CHOICE_TYPES_OFFSET];

			//System.out.println("domain offset: " + DOMAIN_PARAM_OFFSET);
			for(Iterator itChoices = tmDomains.values().iterator(); itChoices.hasNext(); ) {
				String c = (String)itChoices.next();
				boolean isSelected = false;
				for (i = 0; i < taskDomains.length; i++) {
					if (c.equals(taskDomains[i])) {
						isSelected = true;
						break;
					}
				}

				out.println("<option value=\"" + c + "\"" + (isSelected ? " selected" : "") + ">" + StringUtils.htmlEncode(c) + "</option>");
			}
		}*/
	%>
		</select>
	<% //} else { %>
		//	<%= attributeValue %>
	<% //} %>
	<!--	</td> -->
                                <% //if (!viewOnly) { %>
                                <!--	<td valign="top">
                 <input type="button" onclick="javascript:window.open('newDomain.html', 'newDomain', 'width=200,height=200').focus()"  value="new..." class="little">
                  </td> -->
                                <%// } %>
                            </tr>
                        </table>


                    </td>
                </tr>
                <input type="hidden" name="<%= GPConstants.REQUIRED_PATCH_LSIDS %>" value="<%= tia != null ? tia.get(GPConstants.REQUIRED_PATCH_LSIDS) : "" %>">
                <input type="hidden" name="<%= GPConstants.REQUIRED_PATCH_URLS %>" value="<%= tia != null ? tia.get(GPConstants.REQUIRED_PATCH_URLS) : "" %>">

                <% if (!viewOnly) { %>
                <tr>
                    <td valign="top">Support&nbsp;files:<br>(jar, dll, exe, pl, doc, etc.)<br>
                    </td>
                    <td>
                        <font size=-1>
                            The actual program plus any required libraries will be accessible to your command line as
                            &lt;<%= GPConstants.LIBDIR %>&gt;
                            <file.separator><i>filename</i>
                        </font><a href='modules/createhelp.jsp#SupportFiles_brief' target='help'><img border='0' src='images/help2.jpg' /></a>
                        <br>

                        <% for (i = 1; i <= NUM_ATTACHMENTS; i++) { %>
                        <input type="file" name="file<%= i %>" size="70" class="little"><br>
                        <% } %>
                    </td>
                </tr>
                <% } %>


                <%
                    if (taskName != null) {
                        File[] allFiles = null;
                        try {
                            allFiles = taskIntegratorClient.getAllFiles(taskInfo);
                        } catch (org.genepattern.webservice.WebServiceException wse) {
                            allFiles = new File[0];
                        }
                        if (allFiles.length > 0) {
                %>
                <tr class="taskperameter">
                    <td valign="top">Current&nbsp;files:</td>
                    <td>
                        <%

                            }
                            for (i = 0; i < allFiles.length; i++) { %>
                        <a href="getFile.jsp?task=<%= (String)taskInfo.giveTaskInfoAttributes().get(GPConstants.LSID) %>&file=<%= URLEncoder.encode(allFiles[i].getName()) %>" target="new"><%= StringUtils.htmlEncode(allFiles[i].getName()) %>
                        </a>
                        <% if (i != 0 && i % 10 == 0) {%>

                        <br /><%
                            }
                        } %>

                        <% if (allFiles != null && allFiles.length > 0 && !viewOnly) { %>
                        <br>
                        <select name="deleteFiles">
                            <option value="">- File -</option>
                            <% for (i = 0; i < allFiles.length; i++) { %>
                            <option value="<%= StringUtils.htmlEncode(allFiles[i].getName()) %>"><%= allFiles[i].getName() %>
                            </option>
                            <% } %>
                        </select>
                        <input type="hidden" name="deleteSupportFiles" value="">
                        <input type="button" value="<%= DELETE %>..." class="little" onclick="confirmDeleteSupportFiles()">
                        <% } %>

                        <% } %>
                        <br>

                    </td>
                </tr>

                <tr>
                    <td valign="top">Parameters:<font size=-1>&nbsp;</font><br>
                    </td>
                    <td>
                        <font size=-1>
                            The names of these parameters will be available for the command line (above) in the form &lt;name&gt;.<br></font>&nbsp;&nbsp;<a href='modules/createhelp.jsp#Parameters_brief' target='help'><img border='0' src='images/help2.jpg' /></a>
                        <br />
                        <br />
                        <br />
                        <table cols="<%= 3+GPConstants.PARAM_INFO_ATTRIBUTES.length %>" class="pipeline_item">
                            <tr>
                                <td width="20%" valign="bottom"><b>name</b></td>
                                <td width="30%" valign="bottom"><b>description (optional)</b></td>
                                <td width="20" valign="bottom"><b>choices</b><br><font size="-1">(optional semicolon-separated list of choices.)</font></td>
                                <%
                                    for (int attribute = 0; attribute < GPConstants.PARAM_INFO_ATTRIBUTES.length; attribute++) {
                                        attributeName = ((String) GPConstants.PARAM_INFO_ATTRIBUTES[attribute][GPConstants.PARAM_INFO_NAME_OFFSET]);
                                        if (attributeName != null) attributeName = attributeName.replace(GPConstants.PARAM_INFO_SPACER, ' ');
                                        if (attributeName.equals("fileFormat")) {
                                            attributeName = "file format";
                                        }

                                %>
                                <td valign="bottom"><b><%= attributeName %>
                                </b></td>
                                <% } %>
                            </tr>
                            <tr>
                                <td><i>min</i></td>
                                <td><i>values below minimum will be set to this value</i></td>
                                <td><i>2=green, default;0=red;1=blue</i></td>
                                <td><i>2</i></td>
                            </tr>

                            <%= createParameterEntries(0, NUM_PARAMETERS, parameterInfoArray, taskInfo, viewOnly) %>

                            <% if (!viewOnly) { %>
                            <tr>
                                <td></td>
                            </tr>
                            <tr>
                                <td colspan="3" align="center">
                                    <input type="submit" value="Save" name="save" class="little">&nbsp;&nbsp;
                                    <input type="reset" value="Clear" class="little">&nbsp;&nbsp;
                                    <input type="button" value="Help" onclick="window.open('modules/createhelp.jsp', 'help')" class="little">
                                </td>
                            </tr>
                            <tr>
                                <td></td>
                            </tr>

                            <% } %>
                            <!--
           <tr><td>
           <p onclick="document.all.parameters.style.display=(document.all.parameters.style.display=='none' ? '' : 'none')"><u><font color="blue">more parameters...</font></u></p>
           </td></tr>
           <div id="parameters" style="display: none">
           -->
                            <%= createParameterEntries(NUM_PARAMETERS, GPConstants.MAX_PARAMETERS, parameterInfoArray, taskInfo, viewOnly) %>

                            <tr>
                                <td></td>
                            </tr>
                            <tr>
                                <td colspan="3" align="center">
                                    <% if (!viewOnly) { %>
                                    <input type="submit" value="Save" name="save" class="little">&nbsp;&nbsp;
                                    <input type="reset" value="Clear" class="little">&nbsp;&nbsp;
                                    <input type="button" value="Help" onclick="window.open('modules/createhelp.jsp', 'help')" class="little">
                                    <% } else {
                                        lsid = tia.get(GPConstants.LSID);
                                        l = new LSID(lsid);
                                        authorityType = LSIDManager.getAuthorityType(l);
                                        if (authorityType.equals(LSIDUtil.AUTHORITY_MINE)) {
                                    %>
                                    <% } else { %>
                                    <input type="button" value="<%= RUN %>" name="<%= RUN %>" class="little" onclick="runTask()">
                                    <%
                                        if (createModuleAllowed && errors == null) { %>
                                    <input type="button" value="<%= CLONE %>..." name="<%= CLONE %>" class="little" onclick="cloneTask()">
                                    <% } %>

                                    <% }
                                    }
                                    %>
                                </td>
                            </tr>

                            <!-- </div> -->

                        </table>

                    </td>
                </tr>
            </table>
            <p />

        </form>
        <% if (tia != null && errors == null) { %>
        <a href="makeZip.jsp?<%= GPConstants.NAME %>=<%= request.getParameter(GPConstants.NAME) %>&includeDependents=1">package this module into a zip file</a><br>
        <% } %>
        <jsp:include page="footer.jsp" />
    </body>

</html>
<% } catch (Throwable t) {
    t.printStackTrace();
    t.printStackTrace(new java.io.PrintWriter(out));
}
%>
<%!
    public String createSelection(TaskInfoAttributes tia, String name, String[] values, String eventHandlers, boolean viewOnly) {
        StringBuffer sbOut = new StringBuffer();
        String value = (tia != null ? tia.get(name) : "");
        //super hack for privacy menu, get rid of this code when porting from JSP to JSF
        if (name.equals(GPConstants.PRIVACY)) {
            if (value.equals("" + GPConstants.ACCESS_PRIVATE)) {
                value = GPConstants.PRIVATE;
            } else if (value.equals("" + GPConstants.ACCESS_PUBLIC)) {
                value = GPConstants.PUBLIC;
            }
        }
        boolean found = false;
        if (!viewOnly) {
            sbOut.append("<select name=\"" + name + "\"");
            sbOut.append(" " + eventHandlers);
            sbOut.append(">\n");
        }
        String optionValue;
        String optionDisplay;
        int delimiter;
        for (int i = 0; i < values.length; i++) {
            optionDisplay = values[i];
            optionValue = optionDisplay;
            delimiter = optionDisplay.indexOf("=");
            if (delimiter != -1) {
                optionDisplay = optionDisplay.substring(0, delimiter);
                optionValue = optionValue.substring(delimiter + 1);
            }
            if (value.equals(values[i])) {
                found = true;
            }
            if (!viewOnly) {
                sbOut.append("<option value=\"");
                sbOut.append(optionValue);
                sbOut.append("\"");
                if (value.equals(values[i])) {
                    sbOut.append(" selected");
                }
                sbOut.append(">");
            }
            if (!viewOnly || value.equals(values[i])) {
                sbOut.append(StringUtils.htmlEncode(optionDisplay));
            }
            if (!viewOnly) {
                sbOut.append("</option>\n");
            }
        }
        if (!found && value.length() > 0) {
            // add unexpected entry to the selection list
            sbOut.append("<option selected>");
            sbOut.append(StringUtils.htmlEncode(value));
            sbOut.append("</option>\n");
        }

        if (!viewOnly) {
            sbOut.append("</select>");
        }
        return sbOut.toString();
    }
%>
<%!
    public String createParameterEntries(int from, int to, ParameterInfo[] parameterInfoArray, TaskInfo taskInfo, boolean viewOnly) throws Exception {

        StringBuffer out = new StringBuffer();
        ParameterInfo p = null;
        HashMap attributes = null;
        String attributeName = null;
        String attributeValue = null;
        String attributeType = null;

        for (int i = from; i < to; i++) {
            p = (parameterInfoArray != null && i < parameterInfoArray.length) ? parameterInfoArray[i] : null;
            if (viewOnly && p == null) continue;
            attributes = null;
            if (p != null) attributes = p.getAttributes();
            if (attributes == null) attributes = new HashMap();

            out.append("<tr>\n");
            out.append("<td valign=\"top\">" + (!viewOnly ? ("<input name=\"p" + i + "_" + GPConstants.NAME + "\"" + ((p == null) ? "" : ("\" value=\"" + StringUtils.htmlEncode(p.getName()) + "\"")) + ">") : ((p == null) ? "" : StringUtils.htmlEncode(p.getName()))) + "</td>\n");
            out.append("<td valign=\"top\">" + (!viewOnly ? ("<input name=\"p" + i + "_" + GPConstants.DESCRIPTION + "\" size=\"50\"" + ((p == null || p.getDescription() == null) ? "" : ("\" value=\"" + StringUtils.htmlEncode(p.getDescription()) + "\"")) + ">") : ((p == null || p.getDescription() == null) ? "" : (StringUtils.htmlEncode(p.getDescription())))) + "</td>\n");
            out.append("<td valign=\"top\">" + (!viewOnly ? ("<input name=\"p" + i + "_" + "value\" size=\"30\"" + ((p == null || p.getValue() == null) ? "" : ("\" value=\"" + StringUtils.htmlEncode(p.getValue()) + "\"")) + ">") : (((p == null || p.getValue() == null) ? "" : StringUtils.htmlEncode(GenePatternAnalysisTask.replace(p.getValue(), GPConstants.PARAM_INFO_CHOICE_DELIMITER, GPConstants.PARAM_INFO_CHOICE_DELIMITER + " "))))) + "</td>\n");

            if (p != null && p.isInputFile()) {
                attributes.put(GPConstants.PARAM_INFO_TYPE[GPConstants.PARAM_INFO_TYPE_NAME_OFFSET], GPConstants.PARAM_INFO_TYPE_INPUT_FILE);
            }

            for (int attributeNum = 0; attributeNum < GPConstants.PARAM_INFO_ATTRIBUTES.length; attributeNum++) {
                attributeName = (String) GPConstants.PARAM_INFO_ATTRIBUTES[attributeNum][GPConstants.PARAM_INFO_NAME_OFFSET];
                attributeType = (String) GPConstants.PARAM_INFO_ATTRIBUTES[attributeNum][GPConstants.PARAM_INFO_TYPE_OFFSET];

                out.append("<td valign=\"top\">");
                if (attributeType.equals(GPConstants.PARAM_INFO_STRING)) {
                    attributeValue = (String) attributes.get(attributeName);
                    if (attributeValue == null) {
                        attributeValue = "";
                    }
                    if (!viewOnly) {
                        out.append("<input name=\"p" + i + "_" + attributeName + "\" size=\"10\" value=\"");
                    }
                    out.append(StringUtils.htmlEncode(attributeValue));
                    if (!viewOnly) {
                        out.append("\">\n");
                    }

                } else if (attributeType.equals(GPConstants.PARAM_INFO_CHOICE)) {
                    attributeValue = (String) attributes.get(attributeName);
                    if (attributeValue == null) {
                        attributeValue = "";
                    }
                    String[][] choices = null;
                    choices = (String[][]) GPConstants.PARAM_INFO_ATTRIBUTES[attributeNum][GPConstants.PARAM_INFO_CHOICE_TYPES_OFFSET];
                    boolean multiple = (GPConstants.PARAM_INFO_ATTRIBUTES[attributeNum].length > GPConstants.PARAM_INFO_CHOICE_TYPES_MULTIPLE_OFFSET);

                    if (!viewOnly) {
                        String[] items = attributeValue.split(GPConstants.PARAM_INFO_CHOICE_DELIMITER);
                        boolean isFileFormat = attributeName.equals("fileFormat");
                        if (isFileFormat) {
                            String display = "block";
                            if (p == null || !p.isInputFile()) {
                                display = "none";
                            }
                            out.append("<div id=\"p" + i + "_fileFormatDiv\" style=\"display:" + display + "\">");

                        }
                        if (!isFileFormat) {
                            out.append("<select onchange=\"showFileFormats(this," + i + ")\" name=\"p" + i + "_" + attributeName + "\"" + (multiple ? " multiple size=\"" + Math.min(3, choices.length) + "\"" : "") + ">\n");
                        } else {
                            out.append("<select name=\"p" + i + "_" + attributeName + "\"" + (multiple ? " multiple size=\"" + Math.min(3, choices.length) + "\"" : "") + ">\n");
                        }

                        for (int choice = 0; choice < choices.length; choice++) {
                            boolean selected = false;
                            for (int sel = 0; sel < items.length; sel++) {

                                if (choices[choice][GPConstants.PARAM_INFO_TYPE_OFFSET].equals(items[sel])) {
                                    selected = true;
                                    break;
                                }
                            }
                            out.append("<option value=\"" +
                                    choices[choice][GPConstants.PARAM_INFO_TYPE_OFFSET] + "\"" +
                                    (selected ? " selected" : "") + ">" +
                                    StringUtils.htmlEncode(choices[choice][GPConstants.PARAM_INFO_NAME_OFFSET]) +
                                    "</option>\n");
                        }
                        out.append("</select>\n");
                    } else {
                        if (!multiple) {
                            for (int choice = 0; choice < choices.length; choice++) {
                                if (choices[choice][1].equals(attributeValue)) {
                                    out.append(StringUtils.htmlEncode(choices[choice][GPConstants.PARAM_INFO_NAME_OFFSET]));
                                }
                            }
                        } else {
                            out.append(StringUtils.htmlEncode(attributeValue));
                        }
                    }

                } else if (attributeType.equals(GPConstants.PARAM_INFO_CHECKBOX)) {
                    attributeValue = (String) attributes.get(attributeName);
                    if (attributeValue == null) {
                        attributeValue = "";
                    }

                    out.append("<input name=\"p" + i + "_" + attributeName + "\" type=\"checkbox\"" + (attributeValue.length() > 0 ? " checked" : "") + (viewOnly ? " disabled" : "") + ">\n");
                } else {
                    throw new Exception("Unknown attribute type " + attributeType);
                }

                out.append("</td>\n");

            } // end for each attribute
            out.append("</tr>\n");
        } // end for each parameter
        return out.toString();
    } // end of method
%>
</div>

