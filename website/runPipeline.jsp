<%--
  ~ Copyright 2012 The Broad Institute, Inc.
  ~ SOFTWARE COPYRIGHT NOTICE
  ~ This software and its documentation are the copyright of the Broad Institute, Inc. All rights are reserved.
  ~
  ~ This software is supplied without any warranty or guaranteed support whatsoever. The Broad Institute is not responsible for its use, misuse, or functionality.
  --%>
<%@ page
        import="
	java.io.BufferedReader,
                java.io.File,
                java.io.InputStream,
                java.io.InputStreamReader,
                java.io.IOException,
                java.io.OutputStream,
                java.net.URL,
                java.text.DateFormat,
                java.text.ParseException,
                java.text.SimpleDateFormat,
                java.util.Date,
                java.util.Enumeration,
                java.util.GregorianCalendar,
                java.util.Map,
                java.util.HashMap,
                org.genepattern.server.user.User,
                org.genepattern.server.user.UserDAO,
                org.genepattern.webservice.JobStatus,
                org.genepattern.webservice.ParameterInfo,
                org.genepattern.webservice.TaskInfo,
                org.genepattern.util.StringUtils,
                org.genepattern.server.genepattern.GenePatternAnalysisTask,
                org.genepattern.server.executor.RuntimeExecCommand,
                org.genepattern.data.pipeline.PipelineModel,
                org.genepattern.data.pipeline.PipelineUtil,
                org.genepattern.util.GPConstants,
                java.net.URLDecoder"
        session="false" language="Java" buffer="1kb" %>
<%
    response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
    response.setHeader("Pragma", "no-cache"); // HTTP 1.0 cache control
    response.setDateHeader("Expires", 0);
    response.setHeader("Connection", "Keep-Alive");
    response.setHeader("Keep-Alive", "timeout=1000, max=50");
    response.setHeader("HTTP-Version", "HTTP/1.1");

    HashMap requestParamsAndAttributes = new HashMap();

    for (Enumeration eNames = request.getParameterNames(); eNames.hasMoreElements(); ) {
        String n = (String) eNames.nextElement();
        requestParamsAndAttributes.put(n, request.getParameter(n));
    }
    for (Enumeration eNames = request.getAttributeNames(); eNames.hasMoreElements(); ) {
        String n = (String) eNames.nextElement();
        requestParamsAndAttributes.put(n, request.getAttribute(n));
    }

    HashMap commandLineParams = new HashMap();

    int UNDEFINED = -1;
    int jobID = UNDEFINED;
    byte[] userPassword = new byte[0];
    String userEmail = null;
    try {
        boolean DEBUG = false;
        if (!DEBUG && requestParamsAndAttributes.get("DEBUG") != null)
            DEBUG = true;

        User user = null;
        String userID = (String) request.getAttribute("userID"); // will force login if necessary
        if (userID == null || userID.length() == 0) {
            return; // come back after login
        }

        try {
            user = (new UserDAO()).findById(userID);
            userPassword = user.getPassword();
            userEmail = user.getEmail();
            if ((userEmail == null) || (userEmail.length() == 0)) {
                userEmail = userID;
            }
        } catch (Exception e) {
            userEmail = userID;
        }

        String NAME = "name";
        String DOT_PIPELINE = "." + GPConstants.TASK_TYPE_PIPELINE;
        String STOP = "stop";
        String CMD = "cmd";
        String RUN = "run";
        String CODE = "code";
        String EMAIL = "email";
        String JOBID = "jobID";

        String name = (String) requestParamsAndAttributes.get(NAME);
        String command = (String) requestParamsAndAttributes.get(CMD);
        if (command == null) {
            command = RUN;
        }
        String description = null;
        boolean isSaved = (requestParamsAndAttributes.get("saved") == null);
        TaskInfo taskInfo = null;
        if (!isSaved) {
            description = (String) requestParamsAndAttributes.get("description");
            taskInfo = (TaskInfo) requestParamsAndAttributes.get("taskInfo");
        } else if (!command.equals(STOP) && name != null) {
            taskInfo = GenePatternAnalysisTask.getTaskInfo(URLDecoder.decode(name, "UTF-8"), userID);
        }
        String pipelineName = (name == null ? ("unnamed" + DOT_PIPELINE) : taskInfo.getName());

        String baseURL = "";
        String gpURL = System.getProperty("GenePatternURL", "");
        gpURL = gpURL.trim();
        if (gpURL.length() > 0) {
            URL url = new URL(gpURL);
            String portStr = "";
            int port = url.getPort();
            if (port > 0) {
                portStr = ":" + port;
            }
            baseURL = url.getProtocol() + "://" + url.getHost() + portStr + request.getRequestURI();
        } else {
            String portStr = "";
            int port = request.getServerPort();
            if (port > 0) {
                portStr = ":" + port;
            }
            baseURL = request.getScheme() + "://" + request.getServerName() + portStr + request.getRequestURI();
        }
        baseURL = baseURL.substring(0, baseURL.lastIndexOf("/") + 1);
        try {
            if (command.equals(STOP)) {
%>
<script language="Javascript">
    self.window.alert('STOP button not working');
    self.window.close();
</script>
<%
        return;
    }

    /**
     * Collect the command line params from the request and see if they are all present
     */

    ParameterInfo[] parameterInfoArray = taskInfo.getParameterInfoArray();
    ParameterInfo[] parameters = (ParameterInfo[]) request.getAttribute("parameters");
    Map<String, String> nameToParameter = new HashMap<String, String>();
    if (parameters != null) {
        for (ParameterInfo p : parameters) {
            nameToParameter.put(p.getName(), p.getValue());
        }
    }
    if (parameterInfoArray != null && parameterInfoArray.length > 0) {
        // the pipeline needs parameters.  If they are provided, set them
        // into the correct place.
        for (int i = 0; i < parameterInfoArray.length; i++) {
            ParameterInfo param = parameterInfoArray[i];
            String key = param.getName();

            Object val = requestParamsAndAttributes.get(key); // get non-file param
            if (val == null) {
                val = nameToParameter.get(key);
            }
            if (val != null) {
                commandLineParams.put(key, val);
            }

        }
    }

    // check all required params present and punt if not
    boolean paramsRequired = PipelineUtil.validateAllRequiredParametersPresent(taskInfo, commandLineParams);
    if (paramsRequired) {
        request.setAttribute("name", pipelineName);
        request.getRequestDispatcher("cannotRunPipeline.jsp").forward(request, response);
        return;
    }

    // set the title
    String title = "pipeline";
    if (name != null)
        title = taskInfo.getName();
    if (description != null)
        title = title + ": " + description;

    if (isSaved) {
        try {
            if ((name != null) && (!(name.trim().length() == 0))) {
                taskInfo = GenePatternAnalysisTask.getTaskInfo(URLDecoder.decode(name, "UTF-8"), userID);
            } else {
                taskInfo = GenePatternAnalysisTask.getTaskInfo(pipelineName, userID);
            }
        } catch (Exception e) {
        }
        if (taskInfo == null) {
            request.setAttribute("name", pipelineName);
            request.getRequestDispatcher("unknownPipeline.jsp").forward(request, response);
            return;
        }
        description = taskInfo.getDescription();
    } else { // !isSaved
        taskInfo = (TaskInfo) requestParamsAndAttributes.get("taskInfo");
    }
    String taskName = (String) request.getAttribute("taskName");
%>

<html>
    <head>
        <link href="skin/stylesheet.css" rel="stylesheet" type="text/css">
        <link href="css/style.css" rel="stylesheet" type="text/css">
        <link href="css/style-jobresults.css" rel="stylesheet" type="text/css">
        <link href="skin/favicon.ico" rel="shortcut icon">

        <title>GenePattern | <%=title.replace('.', ' ')%> Results</title>
        <script language="Javascript" src="js/prototype.js"></script>
        <script language="Javascript" src="js/commons-validator-1.3.0.js"></script>
        <script language="Javascript" src="js/genepattern.js"></script>
        <jsp:include page="navbarHead.jsp" />
    </head>
    <body>
        <jsp:include page="navbar.jsp" />
        <script language="Javascript">
            var outputFileCount = new Array();
        </script>
        <%
            out.flush();
            if (command.equals(RUN)) {
                out.println("Pipeline RUN command not working <br>");
                out.flush();
            } else {
                out.println("unknown command: " + command + "<br>");
            }
        } catch (Throwable e) {
            out.flush();
            out.println("<br>");
            out.println("runPipeline failed: <br>");
            out.println(e.getMessage());
            out.println("<br><pre>");
            e.printStackTrace();
            out.println("</pre><br>");

            if (jobID != UNDEFINED) {
                // RuntimeExecCommand.updatePipelineStatus(jobID, JobStatus.JOB_ERROR, null);
                // RuntimeExecCommand.terminatePipeline(Integer.toString(jobID));
            }
            return;
        } finally {
        %>

        <jsp:include page="footer.jsp" />
    </body>
</html>
<%
        }
    } catch (Throwable t) {
        out.println(t);
        out.println("<br><pre>");
        t.printStackTrace();
        out.println("</pre><br>");
    }
%>
