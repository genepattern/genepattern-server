<%--
  ~ Copyright 2012 The Broad Institute, Inc.
  ~ SOFTWARE COPYRIGHT NOTICE
  ~ This software and its documentation are the copyright of the Broad Institute, Inc. All rights are reserved.
  ~
  ~ This software is supplied without any warranty or guaranteed support whatsoever. The Broad Institute is not responsible for its use, misuse, or functionality.
  --%>


<%@ page import="org.genepattern.server.webapp.*,
                 org.genepattern.data.pipeline.JobSubmission,
                 org.genepattern.data.pipeline.PipelineModel,
                 org.genepattern.data.pipeline.PipelineUtil,
                 org.genepattern.server.util.AccessManager,
                 org.genepattern.server.genepattern.GenePatternAnalysisTask,
                 org.genepattern.server.webservice.server.local.*,
                 org.genepattern.webservice.TaskInfo,
                 org.genepattern.webservice.ParameterInfo,
                 org.genepattern.webservice.TaskInfoAttributes,
                 org.genepattern.server.genepattern.LSIDManager,
                 org.genepattern.util.StringUtils,
                 org.genepattern.server.*,
                 org.genepattern.server.webservice.server.local.LocalTaskIntegratorClient,
                 org.genepattern.util.GPConstants,
                 org.genepattern.util.LSID,
                 org.genepattern.util.LSIDUtil,
                 java.io.File,
                 java.util.Map,
                 java.util.HashMap,
                 org.genepattern.server.webapp.jsf.AuthorizationHelper,
                 java.util.*,
                 java.util.Collection,
                 java.util.Iterator,
                 org.genepattern.server.eula.EulaManager,
                 org.genepattern.server.eula.EulaInfo,
                 org.genepattern.server.config.ServerConfiguration"
         session="true" contentType="text/html" language="Java" %>
<%
    response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
    response.setHeader("Pragma", "no-cache");         // HTTP 1.0 cache control
    response.setDateHeader("Expires", 0);


    String userID = (String) request.getAttribute("userID"); // will force login if necessary

    String pipelineName = request.getParameter("name");
    if (pipelineName == null) {
%>    Must specify a name parameter
<%
        return;
    }
    PipelineModel model = null;

    if (LSID.isLSID(pipelineName)) pipelineName = new LSID(pipelineName).toString();

    TaskInfo task = new org.genepattern.server.webservice.server.local.LocalAdminClient(userID).getTask(pipelineName);
    String version = "";
    if (task != null) {
        TaskInfoAttributes tia = task.giveTaskInfoAttributes();
        if (tia != null) {
            String serializedModel = (String) tia.get(GPConstants.SERIALIZED_MODEL);
            if (serializedModel != null && serializedModel.length() > 0) {
                try {
                    model = PipelineModel.toPipelineModel(serializedModel);
                } catch (Throwable x) {
                    System.out.println("exception loading serialized model " + x);

                    x.printStackTrace(System.out);
                }
            }
            String lsidStr = tia.get("LSID");
            LSID pipeLSID = new LSID(lsidStr);
            version = pipeLSID.getVersion();
        }
    }
%>
<html>
    <head>
        <script language="JavaScript">
            var numTasks = <% out.print(model.getTasks().size()); %>
                    function toggle(visible) {
                        for (var i = 0; i < numTasks; i++) {
                            toggleTask(visible, i);
                        }
                    }

            function toggleTask(visible, i) {
                formobj = document.getElementById('id' + i);
                arrowImg = document.getElementById('arrowTask' + i);
                if (visible == null) {
                    visible = (formobj.style.display == "none");
                }
                if (!visible) {
                    formobj.style.display = "none";
                    if (arrowImg != null)
                        arrowImg.src = "images/arrow-pipelinetask-right.gif"
                } else {
                    formobj.style.display = "block";
                    if (arrowImg != null)
                        arrowImg.src = "images/arrow-pipelinetask-down.gif"
                }
            }

            function toggleLSID(visible) {
                formobj = document.getElementById('pipeline_lsid');

                if (!visible) {
                    formobj.style.display = "none";
                } else {
                    formobj.style.display = "inline";
                }

                for (var i = 0; i < numTasks; i++) {
                    formobj = document.getElementById('lsid' + i);
                    if (!visible) {
                        formobj.style.display = "none";
                    } else {
                        formobj.style.display = "block";
                    }
                }
            }


            function cloneTask(origName, lsid, user) {
                while (true) {
                    suggestedName = "copyOf" + origName;
                    var cloneName = window.prompt("Name for cloned pipeline", suggestedName);
                    if (cloneName == null || cloneName.length == 0) {
                        return;
                    }
                    window.location = "saveTask.jsp?clone=1&name=" + origName + "&LSID=" + lsid + "&cloneName=" + cloneName + "&userid=" + user + "&pipeline=1";
                    break;
                }
            }

            function runpipeline(url) {
                window.location = url;
            }
        </script>

        <link href="skin/stylesheet.css" rel="stylesheet" type="text/css">
        <link rel="SHORTCUT ICON" href="favicon.ico">
        <title><%=task.getName()%>
        </title>
        <jsp:include page="navbarHead.jsp" />
    </head>
    <body>
        <jsp:include page="navbar.jsp" />
        <%
            String displayName = task.getName();
            if (displayName.endsWith(".pipeline")) {
                displayName = displayName.substring(0, displayName.length() - ".pipeline".length());
            }
            out.println("<p><font size='+2'><b>" + displayName + "</font> version <font size='+2'>" + version + "</font></b>");

// show edit link when task has local authority and either belongs to current user or is public
            String lsid = (String) task.getTaskInfoAttributes().get(GPConstants.LSID);
            boolean showEdit = false;
            try {

                showEdit = task.getUserId().equals(userID) && LSIDUtil.getInstance().isAuthorityMine(task.getLsid());

            } catch (Exception e) {
                e.printStackTrace(System.out);
            }
            if (showEdit) {
/*                 String editURL = "pipeline/index.jsf?lsid=" + pipelineName;
                out.println("  <input type=\"button\" value=\"Edit\" name=\"edit\" class=\"little\" onclick=\"window.location='" + editURL + "'\"; />"); */
            }

            if (AuthorizationHelper.createPipeline(userID)) {
                out.println("  <input type=\"button\" value=\"Clone...\" name=\"clone\" class=\"little\" onclick=\"cloneTask('" + displayName + "', '" + pipelineName + "', '" + userID + "')\"; />");
            }
            if (!PipelineUtil.isMissingTasks(model, userID)) {
                out.println("  <input type=\"button\" value=\"Run\" name=\"runpipeline\" class=\"little\" onclick=\"runpipeline('" + request.getContextPath() + "/pages/index.jsf?lsid=" + pipelineName + "')\"; />");
            }
//XXXXXXXXXXXXX
            String descrip = task.getDescription();
            out.print("<span id=\"pipeline_lsid\" style=\"display:none;\">");
            out.print("<pre>     " + lsid + "</pre></span>");


            if ((descrip != null) && (descrip.length() > 0))
                out.println("<br>" + descrip);
            out.println("<br>Author: " + StringUtils.htmlEncode(task.getTaskInfoAttributes().get(GPConstants.AUTHOR)));


            LocalTaskIntegratorClient taskIntegratorClient = new LocalTaskIntegratorClient(userID, out);
            File[] docFiles = taskIntegratorClient.getDocFiles(task);
            if (docFiles != null) {
                if (docFiles.length > 0) {
                    out.println("<br>Documentation: ");
                    for (int i = 0; i < docFiles.length; i++) {
                        if (i > 0) out.println("  ,");
                        out.println("<a href='getTaskDoc.jsp?name=" + pipelineName + "'&file=" + docFiles[i].getName() + " target='_new'>" + docFiles[i].getName() + "</a>");
                    }

                }
            }

            out.println("<br>License: ");
            ServerConfiguration.Context taskContext = ServerConfiguration.Context.getContextForUser(userID);
            if (taskContext != null) {
                String contextPath=request.getContextPath();
                List<EulaInfo> eulas = EulaManager.instance(taskContext).getEulas(task);
                if (eulas != null && eulas.size() != 0) {
                    out.println("<a href='" + eulas.get(0).getLink( contextPath ) + "' target='new'>" + eulas.get(0).getLicense() + "</a>");
                }
            }
            out.println("<br>");
        %>

        <%
            //XXXXXXXXXXX

            try {
                PipelineUtil.isMissingTasks(model, new java.io.PrintWriter(out), userID);
            } catch (Exception e) {
                out.println("An error occurred while processing your request. Please try again.");
                return;
            }
            List tasks = model.getTasks();
        %>
        <br>

        <form name="form1"><a href="#" onClick="toggle(true)" class="smalltype">open all</a> | <a href="#" onClick=toggle(false) class="smalltype">close all</a></form>

        <%
            //<input id="togglelsid" type="checkbox" onClick=toggleLSID();>Show LSIDs</input>

            for (int i = 0; i < tasks.size(); i++) {
                out.println("<div class=\"pipeline_item\">");
                JobSubmission js = (JobSubmission) tasks.get(i);
                ParameterInfo[] parameterInfo = js.giveParameterInfoArray();
                int displayNumber = i + 1;
                TaskInfo formalTask = GenePatternAnalysisTask.getTaskInfo(js.getName(), userID);

                TaskInfo ti = GenePatternAnalysisTask.getTaskInfo(js.getLSID(), userID);
                boolean unknownTask = !GenePatternAnalysisTask.taskExists(js.getLSID(), userID);
                boolean unknownTaskVersion = false;
                if (unknownTask) {
                    // check for alternate version
                    String taskLSIDstr = js.getLSID();
                    LSID taskLSID = new LSID(taskLSIDstr);
                    String taskLSIDstrNoVer = taskLSID.toStringNoVersion();

                    unknownTaskVersion = !GenePatternAnalysisTask.taskExists(taskLSIDstrNoVer, userID);
                }
                String taskLSIDstr = js.getLSID();
                LSID taskLSID = new LSID(taskLSIDstr);
                String taskLsidVersion = taskLSID.getVersion();

                Map tia = formalTask != null ? formalTask.getTaskInfoAttributes() : null;

                ParameterInfo[] formalParams = formalTask != null ? formalTask.getParameterInfoArray() : null;
                if (formalParams == null) {
                    formalParams = new ParameterInfo[0];
                }
        %>

        <table width="100%" border="0" cellpadding="0" cellspacing="0" class="barhead-task">
            <tr>
                <%
                    if (formalTask == null) {
                        out.print("<td width=\"8\">&nbsp;</td><td>&nbsp;&nbsp;" + displayNumber + ". <font color='red'>" + js.getName() + "</font></font> is not present on this server.</td><td class='smalltype' align='right'>version " + taskLsidVersion + "</td></tr></table><table class=\"attribute\"><td>&nbsp;</td><td>&nbsp;</td>");
                        tia = new HashMap();
                        formalParams = new ParameterInfo[0];
                    } else if (!unknownTask) {
                %>
                <td width=\
                "8\"><a href="#" onClick="toggleTask(null, <%=displayNumber-1%>)"><img id="arrowTask<%=displayNumber-1%>" src="images/arrow-pipelinetask-right.gif" alt="hide module" width="8" height="8" vspace="3" border="0" /></a></td>
                <td><%=displayNumber%>. <a href="addTask.jsp?view=1&name=<%=js.getLSID()%>"><%=js.getName()%>
                </a></td>
                <td class='smalltype' align='right'>version <%=taskLsidVersion %>
                </td>
            </tr>
        </table>
        <table class="attribute">
            <tr>
                <td>&nbsp;</td>
                <td><%= StringUtils.htmlEncode(formalTask.getDescription())%>
                </td>
                    <%


	} else {
		if (!unknownTaskVersion) {
			TaskInfo altVersionInfo = GenePatternAnalysisTask.getTaskInfo(taskLSID.toStringNoVersion(), userID);
			Map altVersionTia = altVersionInfo.getTaskInfoAttributes();

			LSID altVersionLSID = new LSID((String)(altVersionTia.get(GPConstants.LSID)) );

			out.print("<td width=\"8\">&nbsp;</td><td>&nbsp;&nbsp; "+displayNumber+". <font color='red'>"+ js.getName() + "</font></font> This module version <b>("+taskLSID.getVersion()+")</b> is not present on this server.</td><td class='smalltype' align='right'>version "+taskLsidVersion +"</td></tr></table><table><tr><td>&nbsp;</td><td> The version present on this server is <br>"  );
		out.print("<dd><a href=\"addTask.jsp?view=1&name=" + js.getName() + "\">" + js.getName() + " <b>("+altVersionLSID .getVersion()+")</b> </a> " + StringUtils.htmlEncode(formalTask.getDescription()) + "</td>");



		} else {

			out.print("<td width=\"8\">&nbsp;</td><td><font color='red'>"+ js.getName() + "</font></font> This module is not present on this server</td>"  );

		}


	}
	out.println("</tr></table>");



	out.print("<div id=\"lsid"+ i + "\" style=\"display:none;\">");
	out.print("<pre>     " + js.getLSID() + "</pre>");
	out.print("</div>");

	out.println("<div id=\"id"+ i + "\" style=\"display:none;\">");

out.println("<table cellspacing='0' width='100%' class='attribute'>");
	boolean[] runtimePrompt = js.getRuntimePrompt();
	java.util.Map paramName2FormalParamMap = new java.util.HashMap();

	for(int j = 0; j < formalParams.length; j++) {
		paramName2FormalParamMap.put(formalParams[j].getName(), formalParams[j]);
	}
	boolean odd = false;
%>

            <tr class="tableheader-row2">
                <td>parameter name</td>
                <td>value</td>
                <td>description</td>
            </tr>
                <%

	for(int j = 0; j < formalParams.length; j++) {
		String paramName = formalParams[j].getName();
		String paramDescription = formalParams[j].getDescription();

		ParameterInfo formalParam = (ParameterInfo) paramName2FormalParamMap.get(paramName);
		ParameterInfo informalParam = null;
		int k;
		for (k=0; k < parameterInfo.length; k++){
			if (paramName.equals(parameterInfo[k].getName())){
				informalParam = parameterInfo[k];
				break;
			}
		} // for k
		if (informalParam == null) {
			informalParam = formalParam;
			k = j;
		}

 		String value = null;
		if(formalParam.isInputFile()) {

			java.util.Map pipelineAttributes = informalParam.getAttributes();

			String taskNumber = null;
			if(pipelineAttributes!=null) {
				taskNumber = (String) pipelineAttributes.get(PipelineModel.INHERIT_TASKNAME);
			}

			if((k < runtimePrompt.length )&&(runtimePrompt[k])) {
				value = "Prompt when run";
			} else if (taskNumber != null) {
				String outputFileNumber = (String) pipelineAttributes.get(PipelineModel.INHERIT_FILENAME);
				int taskNumberInt = Integer.parseInt(taskNumber.trim());
				String inheritedOutputFileName = outputFileNumber;
				if(outputFileNumber.equals("1")) {
					inheritedOutputFileName = "1st output";
				} else if(outputFileNumber.equals("2")) {
					inheritedOutputFileName = "2nd output";
				} else if(outputFileNumber.equals("3")) {
					inheritedOutputFileName = "3rd output";
				} else if(outputFileNumber.equals("stdout")) {
					inheritedOutputFileName = "standard output";
				} else if(outputFileNumber.equals("stderr")) {
					inheritedOutputFileName = "standard error";
				}
				JobSubmission previousTask = (JobSubmission) tasks.get(taskNumberInt);
				int displayTaskNumber = taskNumberInt + 1;

				value = "Use <b>" + inheritedOutputFileName + "</b> from <a href=\"#"+displayTaskNumber +"\">" + displayTaskNumber + ". " + previousTask.getName() +"</a>";
			} else {

				value = informalParam.getValue();
				Properties props = System.getProperties();
				props.setProperty("LSID", lsid);


				try {
					new java.net.URL(value); // see if parameter if a URL
					value = "<a href=\"" + value + "\">" + value + "</a>";

				} catch(java.net.MalformedURLException x) {
					try {
						String svalue = GenePatternAnalysisTask.substitute(value, props, null);
						new java.net.URL(svalue); // see if parameter if a URL

						String filename = value;
						int idx = value.indexOf("file=");
						if (idx >= 0) filename = value.substring(idx+5);
						value = "<a href=\"" + svalue + "\">" + filename + "</a>";


					} catch (java.net.MalformedURLException xx){
			               value = StringUtils.htmlEncode(value);
					}
				}
			}

		}  else {
			String[] values = formalParam.getValue().split(GPConstants.PARAM_INFO_CHOICE_DELIMITER);
			value = informalParam.getValue();
            try {
                for (int v = 0; v < values.length; v++) {
                    final String[] eachValue = values[v].split(GPConstants.PARAM_INFO_TYPE_SEPARATOR);
                    if (eachValue.length==0) {
                        if (value == "") {
                            break;
                        }
                    }
                    else if (value.equals(eachValue[0])) {
                        if (eachValue.length == 2) {
                            value = eachValue[1];
                        }
                        break;
                    }
                }
            }
            catch (Throwable t) {
                //TODO: log the error, for GP-4806
                value= value + " (Error parsing choice list: "+t.getLocalizedMessage()+" )";
            }
			value = StringUtils.htmlEncode(value);
		}

		paramName = paramName.replace('.', ' ');
		//	out.print("<dd>" + paramName);
		//	out.println(": " + value);

		out.print("<tr class='taskperameter'><td width='25%' class='attribute-required'>" + paramName );


		out.flush();

		out.print(":</td><td class='attribute-required' >" + value);
		out.print("</td><td class='attribute-required' >" + paramDescription);

		out.println("</td></tr>");

		odd = !odd;
	}
	out.println("</table>");

	out.println("</div>");
   out.println("</div><br>");



}
if (! PipelineUtil.isMissingTasks(model, userID)){
out.println("<table width='100%'><tr><td align='center'><input type=\"button\" value=\"Run\"      name=\"runpipeline\" class=\"little\" onclick=\"runpipeline('" + request.getContextPath() + "/pages/index.jsf?lsid="+pipelineName + "')\"; /></td></tr></table>");
}

%>
            <jsp:include page="footer.jsp" />
    </body>
</html>
