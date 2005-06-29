<%@ page import="org.genepattern.server.webapp.*,
		 org.genepattern.data.pipeline.*,
		 org.genepattern.server.util.AccessManager,
		 org.genepattern.server.genepattern.GenePatternAnalysisTask,
		 org.genepattern.server.webservice.server.local.*,
		 org.genepattern.webservice.TaskInfo,
		 org.genepattern.webservice.ParameterInfo,
		 org.genepattern.webservice.TaskInfoAttributes,
		 org.genepattern.server.genepattern.LSIDManager,
		 org.genepattern.data.pipeline.*,
		 org.genepattern.server.*,
         	 org.genepattern.server.webapp.RunPipelineForJsp,
		 org.genepattern.server.webservice.server.local.LocalTaskIntegratorClient,
		 org.genepattern.util.GPConstants,
		 org.genepattern.util.LSID,
		 org.genepattern.util.LSIDUtil,
		 java.io.File,
		 java.util.Map,
		 java.util.HashMap,
		 java.util.*,
		 java.util.Collection,
		 java.util.Iterator"
	session="true" contentType="text/html" language="Java" %>
<%
String userID = AccessManager.getUserID(request, response); // will force login if necessary
if (userID == null) return; // come back after login

String pipelineName = request.getParameter("name");
if (pipelineName == null) {
%>	Must specify a name parameter
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
		 String serializedModel = (String)tia.get(GenePatternAnalysisTask.SERIALIZED_MODEL);
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
function toggle() {
	for(var i = 0; i < numTasks; i++) {
		formobj = document.getElementById('id' + i);
		var visible = document.form1.togglecb.checked;
		if(!visible) {
			formobj.style.display = "none";
		} else {
			formobj.style.display = "block";
		}
	}
}

function toggleLSID() {
	formobj = document.getElementById('pipeline_lsid');
	var visible = document.form1.togglelsid.checked;
	if(!visible) {
		formobj.style.display = "none";
	} else {
		formobj.style.display = "inline";
	}

	for(var i = 0; i < numTasks; i++) {
		formobj = document.getElementById('lsid' + i);
		if(!visible) {
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
		if(cloneName.lastIndexOf(".pipeline")==-1) {
			cloneName = cloneName + ".pipeline";
		}
		window.location = "saveTask.jsp?clone=1&name="+origName+"&LSID=" + lsid + "&cloneName=" + cloneName + "&userid=" + user + "&pipeline=1";
		break;
	}
}

function runpipeline( url) {
		window.location= url;
}
</script>
	
<link href="stylesheet.css" rel="stylesheet" type="text/css">
<link rel="SHORTCUT ICON" href="favicon.ico" >
<title><%=task.getName()%></title>

</head>
<body>
<jsp:include page="navbar.jsp"></jsp:include>
<%
String displayName = task.getName();
if(displayName.endsWith(".pipeline")) {
	displayName = displayName.substring(0, displayName.length()-".pipeline".length());
}
out.println("<p><font size='+2'><b>" + displayName+ "</font> version <font size='+2'>"+version+"</font></b>");

// show edit link when task has local authority and either belongs to current user or is public
String lsid = (String) task.getTaskInfoAttributes().get(GPConstants.LSID);
boolean showEdit = false;
try {
	LSIDManager manager = LSIDManager.getInstance();
	String authority = manager.getAuthorityType(new org.genepattern.util.LSID(lsid));
	if(authority.equals(LSIDUtil.AUTHORITY_MINE)) {
		showEdit = task.getTaskInfoAttributes().get(GPConstants.PRIVACY).equals(GPConstants.PUBLIC) || task.getTaskInfoAttributes().get(GPConstants.USERID).equals(userID);
	}
} catch(Exception e){e.printStackTrace(System.out);}
if(showEdit) {
	String editURL = "pipelineDesigner.jsp?name=" + pipelineName;
	out.println("  <input type=\"button\" value=\"edit\" name=\"edit\" class=\"little\" onclick=\"window.location='" + editURL + "'\"; />");
}
out.println("  <input type=\"button\" value=\"clone...\" name=\"clone\"       class=\"little\" onclick=\"cloneTask('"+displayName+"', '" + pipelineName + "', '" + userID + "')\"; />");

if (! RunPipelineForJsp.isMissingTasks(model, userID)){
	out.println("  <input type=\"button\" value=\"run\"      name=\"runpipeline\" class=\"little\" onclick=\"runpipeline('runTask.jsp?cmd=run&name="+pipelineName + "')\"; />");
}				
//XXXXXXXXXXXXX
String descrip = task.getDescription();
out.print("<span id=\"pipeline_lsid\" style=\"display:none;\">");
out.print("<pre>     " + lsid + "</pre></span>");


if ((descrip != null) && (descrip.length() > 0))
	out.println("</br>"+ descrip);
out.println("<br>Owner: " + task.getUserId());
		

LocalTaskIntegratorClient taskIntegratorClient = new LocalTaskIntegratorClient(userID, out);
File[] docFiles = taskIntegratorClient.getDocFiles(task);
if (docFiles != null){
	if (docFiles.length > 0){
		out.println("<br>Documentation: ");
		for (int i = 0; i < docFiles.length; i++) {
			if (i > 0) out.println("  ,");
			out.println("<a href='getTaskDoc.jsp?name="+pipelineName+"'&file="+docFiles[i].getName()+" target='_new'>"+docFiles[i].getName()+"</a>");
		}

	}
}


//XXXXXXXXXXX
out.println("&nbsp;&nbsp;<form name=\"form1\"><input name=\"togglecb\" type=\"checkbox\" onClick=toggle();>Show Input Parameters</input><input name=\"togglelsid\" type=\"checkbox\" onClick=toggleLSID();>Show LSIDs</input></form>");

try {
   RunPipelineForJsp.isMissingTasks(model, new java.io.PrintWriter(out), userID);
} catch(Exception e) {
    out.println("An error occurred while processing your request. Please try again.");
   return;
}
List tasks = model.getTasks();
for(int i = 0; i < tasks.size(); i++) {

	JobSubmission js = (JobSubmission) tasks.get(i);
	ParameterInfo[] parameterInfo = js.giveParameterInfoArray();
	int displayNumber = i+1;
	TaskInfo formalTask = GenePatternAnalysisTask.getTaskInfo(js.getName(), userID);

	TaskInfo ti = GenePatternAnalysisTask.getTaskInfo(js.getLSID(), userID);
	boolean unknownTask = !GenePatternAnalysisTask.taskExists(js.getLSID(), userID);
	boolean unknownTaskVersion = false;
	if (unknownTask){
		// check for alternate version
		String taskLSIDstr = js.getLSID();
		LSID taskLSID = new LSID(taskLSIDstr);
		String taskLSIDstrNoVer = taskLSID.toStringNoVersion();
		
		unknownTaskVersion = !GenePatternAnalysisTask.taskExists(taskLSIDstrNoVer , userID);
	}
    
   out.print("<p><font size=\"+1\"><a name=\""+ displayNumber +"\"/> " + displayNumber + ". ");
    
   Map tia = formalTask!=null?formalTask.getTaskInfoAttributes():null;

	ParameterInfo[] formalParams = formalTask!=null?formalTask.getParameterInfoArray():null;
   if(formalParams==null) {
      formalParams = new ParameterInfo[0];
   }
   if(formalTask==null) {
      out.print("<font color='red'>"+ js.getName()  + "</font></font> is not present on this server.");
      tia = new HashMap();
      formalParams = new ParameterInfo[0];
   } else if (!unknownTask){
		out.print("<a href=\"addTask.jsp?view=1&name=" + js.getLSID() + "\">" + js.getName() + "</a></font> " + GenePatternAnalysisTask.htmlEncode(formalTask.getDescription()));
		
	} else {
		if (!unknownTaskVersion) {
			LSID taskLSID = new LSID(js.getLSID());
			TaskInfo altVersionInfo = GenePatternAnalysisTask.getTaskInfo(taskLSID.toStringNoVersion(), userID);
			Map altVersionTia = altVersionInfo.getTaskInfoAttributes();
			
			LSID altVersionLSID = new LSID((String)(altVersionTia.get(GPConstants.LSID)) );

			out.print("<font color='red'>"+ js.getName() + "</font></font> This task version <b>("+taskLSID.getVersion()+")</b> is not present on this server. The version present on this server is <br>"  );
		out.print("<dd><a href=\"addTask.jsp?view=1&name=" + js.getName() + "\">" + js.getName() + " <b>("+altVersionLSID .getVersion()+")</b> </a> " + GenePatternAnalysisTask.htmlEncode(formalTask.getDescription()));

		

		} else {

			out.print("<font color='red'>"+ js.getName() + "</font></font> This task is not present on this server"  );

		}


	}



	out.print("<div id=\"lsid"+ i + "\" style=\"display:none;\">");
	out.print("<pre>     " + js.getLSID() + "</pre>");
	out.print("</div>");

	out.println("<div id=\"id"+ i + "\" style=\"display:none;\">");

out.println("<table cellspacing='0' width='100%' frame='box'>");
	boolean[] runtimePrompt = js.getRuntimePrompt();
	java.util.Map paramName2FormalParamMap = new java.util.HashMap();
   
	for(int j = 0; j < formalParams.length; j++) {
		paramName2FormalParamMap.put(formalParams[j].getName(), formalParams[j]);
	}
	boolean odd = false;
	for(int j = 0; j < formalParams.length; j++) {
		String paramName = formalParams[j].getName();

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
				String outputFileNumber = (String) pipelineAttributes
				.get(PipelineModel.INHERIT_FILENAME);
				int taskNumberInt = Integer.parseInt(taskNumber.trim());
				String inheritedOutputFileName = null;
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
            
				try {
					new java.net.URL(value); // see if parameter if a URL
					value = "<a href=\"" + value + "\">" + value + "</a>";
               
				} catch(java.net.MalformedURLException x) { 
			               value = GenePatternAnalysisTask.htmlEncode(value);
				}
			}
			
		}  else {
			String[] values = formalParam.getValue().split(GPConstants.PARAM_INFO_CHOICE_DELIMITER);
			String[] eachValue;
			value = informalParam.getValue();
			for (int v = 0; v < values.length; v++) {
				eachValue = values[v].split(GPConstants.PARAM_INFO_TYPE_SEPARATOR);
				if (value.equals(eachValue[0])) {
					if (eachValue.length == 2) {
						value = eachValue[1];
					}
					break;
				}
			}
			value = GenePatternAnalysisTask.htmlEncode(value);
		}
      
		paramName = paramName.replace('.', ' ');
		//	out.print("<dd>" + paramName);
		//	out.println(": " + value);
		if (odd)
			out.print("<tr ><td width='25%' align='right'>" + paramName );
		else 
			out.print("<tr  bgcolor='#EFEFFF'><td width='25%' align='right'>" + paramName);

	
		out.flush();
	
		out.print(":</td><td>&nbsp;&nbsp;&nbsp; " + value);
		out.println("</td></tr>");

		odd = !odd;
	}
	out.println("</table>");

	out.println("</div>");
   

}out.println("<table cellspacing='0' width='100%' frame='box'>");
if (! RunPipelineForJsp.isMissingTasks(model, userID)){

out.println("<table width='100%'><tr><td align='center'><input type=\"button\" value=\"run\"      name=\"runpipeline\" class=\"little\" onclick=\"runpipeline('runTask.jsp?cmd=run&name="+pipelineName + "')\"; /></td></tr></table>");
}

%>
<jsp:include page="footer.jsp"></jsp:include>
</body>
</html>
