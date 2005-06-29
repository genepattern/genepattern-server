<%@ page import="java.io.File,
		 java.net.MalformedURLException,
		 java.util.Collection,
		 java.util.HashMap,
		 java.util.StringTokenizer,
		 java.util.Iterator,
		 org.genepattern.webservice.TaskInfo,
		 org.genepattern.webservice.TaskInfoAttributes,
		 org.genepattern.webservice.ParameterInfo,
		 org.genepattern.webservice.ParameterFormatConverter,
		 org.genepattern.webservice.OmnigeneException,
		 org.genepattern.util.GPConstants,
		 org.genepattern.server.webservice.server.local.*,
		 org.genepattern.util.LSID,
 		 org.genepattern.util.StringUtils,
		 org.genepattern.server.genepattern.LSIDManager,
		 org.genepattern.server.util.AccessManager,
		 org.genepattern.server.genepattern.GenePatternAnalysisTask"
	session="false" contentType="text/plain" language="Java" %><%

response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
response.setHeader("Pragma", "no-cache");		 // HTTP 1.0 cache control
response.setDateHeader("Expires", 0);

String serverPort = System.getProperty("GENEPATTERN_PORT");
String userID = request.getParameter(GPConstants.USERID);
if (userID == null) {
	userID = AccessManager.getUserID(request, null); // get userID but don't force login if not defined
}
LocalAdminClient adminClient = new LocalAdminClient(userID);
String password = "";
String language = request.getParameter(GPConstants.LANGUAGE);
if (language == null) language = "R";

	String taskName = request.getParameter(GPConstants.NAME); // null if not specified
	Collection tmTasks = adminClient.getTaskCatalog();
	TaskInfo taskInfo = null;
	TaskInfoAttributes taskInfoAttributes = null;
	String sLSID = null;
	LSID lsid = null;
	String server = "http://" + request.getServerName() + ":" + serverPort;

HashMap hmTasks = computeDefaultLSIDs(tmTasks);

if (language.equals("R")) {

%># R wrappers for GenePattern analysis tasks for user <%= userID %> on server http://<%= request.getServerName() %>:<%= serverPort %>
<%
	// dump a list of named values with one name per LSID and one for the task name, each with a value of the input parameter descriptions
	out.print("gpGetParameters <<- list(\n");
	// first output a list by task name
	for (Iterator itTasks = tmTasks.iterator(); itTasks.hasNext(); ) {
		taskInfo = (TaskInfo)itTasks.next();
		taskInfoAttributes = taskInfo.giveTaskInfoAttributes();
		sLSID = (String)taskInfoAttributes.get(GPConstants.LSID);
		if (taskName != null && !taskName.equals(taskInfo.getName()) && !taskName.equals(sLSID)) {
			continue;
		}
		if (!hmTasks.containsValue(sLSID)) continue;
		out.print("\"" + taskInfo.getName() + "\"=function(");
		// invoke the appropriate version
		out.print(functionGuts(taskInfo, taskInfoAttributes, sLSID, server, userID, true) + ",\n");
	}

	// now do it again, by LSID
	for (Iterator itTasks = tmTasks.iterator(); itTasks.hasNext(); ) {
		taskInfo = (TaskInfo)itTasks.next();
		taskInfoAttributes = taskInfo.giveTaskInfoAttributes();
		sLSID = (String)taskInfoAttributes.get(GPConstants.LSID);
		if (taskName != null && !taskName.equals(taskInfo.getName()) && !taskName.equals(sLSID)) {
			continue;
		}
		out.print("\"" + sLSID + "\"=function(");
		out.print(functionGuts(taskInfo, taskInfoAttributes, sLSID, server, userID, true) + ",\n");
	}
	out.print(");\n");
	out.println("");

	// with latest LSID as default
	for (Iterator itTasks = tmTasks.iterator(); itTasks.hasNext(); ) {
		taskInfo = (TaskInfo)itTasks.next();
		taskInfoAttributes = taskInfo.giveTaskInfoAttributes();
		sLSID = (String)taskInfoAttributes.get(GPConstants.LSID);
		if (taskName != null && !taskName.equals(taskInfo.getName()) && !taskName.equals(sLSID)) {
			continue;
		}
		if (!hmTasks.containsValue(sLSID)) continue;
		lsid = new LSID(sLSID);
		try {
			boolean bNeedsQuoting = !GenePatternAnalysisTask.isRSafe(taskInfo.getName());
			out.print((bNeedsQuoting ? "\"" : "") + taskInfo.getName() + 
				(bNeedsQuoting ? "\"" : ""));
			out.println(" <<- function(..., LSID=\"" + sLSID + "\") { return (gpRunByLSID(LSID, ...)); };");
		} catch (Exception e) {
			out.println("code generation for " + request.getParameter(GPConstants.NAME) + " task failed:");
			out.println(e.getMessage());
			e.printStackTrace();
		}
	}
	out.println("");

} else { // not R
	throw new Exception("getAllTaskWrappers.jsp: unsupported language request: " + language);
}
 %>
<%! public String functionGuts(TaskInfo taskInfo, TaskInfoAttributes taskInfoAttributes, String sLSID, String server, String userID, boolean withBody) throws Exception {
	StringBuffer out = new StringBuffer();

        ParameterInfo[] parameterInfo = new ParameterFormatConverter().getParameterInfoArray(taskInfo.getParameterInfo());
	boolean hasServer = false;
	boolean bNeedsQuoting;
	int i = 0;
	if (parameterInfo != null) {
	      for (i = 0; i < parameterInfo.length; i++) {
		String valueList = parameterInfo[i].getValue();
		HashMap attributes = parameterInfo[i].getAttributes();
		String def = null;
		if (attributes != null) {
			def = (String)attributes.get(GPConstants.PARAM_INFO_DEFAULT_VALUE[GPConstants.PARAM_INFO_NAME_OFFSET]);
			if (def != null && def.length() == 0) def = null;
			if (def == null) {
				String opt = (String)attributes.get(GPConstants.PARAM_INFO_OPTIONAL[GPConstants.PARAM_INFO_NAME_OFFSET]);
				if (opt != null && opt.length() > 0) {
					def = "";
				}
			}
			if (def != null && parameterInfo[i].hasChoices(GPConstants.PARAM_INFO_CHOICE_DELIMITER)) {
				String[] stChoices = parameterInfo[i].getChoices(GPConstants.PARAM_INFO_CHOICE_DELIMITER);
				String display = null;
				String option = null;
				for (int iChoice = 0; iChoice < stChoices.length; iChoice++) {
					String choice = stChoices[iChoice];
					int c = choice.indexOf(GPConstants.PARAM_INFO_TYPE_SEPARATOR);
					if (c == -1) {
						display = choice;
						option = choice;
					} else {
						option = choice.substring(0, c);
						display = choice.substring(c+1);
					}
					if (def.equals(display)) {
// System.out.println("getAllTaskWrappers: " + taskInfo.getName() + " was " + display + " and is now " + option);
						def = option.trim();
						break;
					}
				}
			}

		}
		if (i > 0) out.append(", ");

		hasServer |= (parameterInfo[i].getName().equals("server"));
		out.append(parameterInfo[i].getName().replace('_','.'));
		if (def != null) {
			out.append("=");
			out.append("\"");
			out.append(def);
			out.append("\"");
		}
	      }
	}
	if (!hasServer) {
		out.append(i > 0 ? ", " : "");
		out.append("server");
		out.append("=");
		out.append("\"");
		out.append(server);
		out.append("\"");
	}
	out.append(", LSID=");
	out.append("\"");
	out.append(sLSID);
	out.append("\"");

	out.append(")");
	out.append(" {");
	out.append(" return (");

	boolean isVisualizer = taskInfoAttributes.get(GPConstants.TASK_TYPE).equals(GPConstants.TASK_TYPE_VISUALIZER);
	if (isVisualizer) {
		String libdir = GenePatternAnalysisTask.getTaskLibDir(taskInfo.getName(), sLSID, userID);
		File[] supportFiles = new File(libdir).listFiles();
		String paramName = null;

		String commandLine = taskInfoAttributes.get(GPConstants.COMMAND_LINE);
		commandLine = GenePatternAnalysisTask.replace(commandLine, "\"", "\\\""); // change quotes to backslash-quotes
		commandLine = makePaste(commandLine);
		
		// libdir: change backslashes to forward slashes
		libdir = GenePatternAnalysisTask.replace(libdir, "\\", "/");

		// task name
		out.append("gpRunVisualizer(\"" + taskInfo.getName() + "\", ");
		// command line
		out.append(commandLine + ", ");
		out.append("\"" + libdir + "\", ");
		// OS
		out.append("\"" + taskInfoAttributes.get(GPConstants.OS) + "\", ");
		// CPU
		out.append("\"" + taskInfoAttributes.get(GPConstants.CPU_TYPE) + "\", ");

		// downloadable files
		out.append("\"");
		int numToDownload = 0;
		for (i = 0; parameterInfo != null && i < parameterInfo.length; i++) {
			paramName = parameterInfo[i].getName();
			if (parameterInfo[i].isInputFile() && 
			    (parameterInfo[i].getValue().startsWith("http:") ||
			     parameterInfo[i].getValue().startsWith("https:") ||
			     parameterInfo[i].getValue().startsWith("ftp:"))) {
				// note that this parameter is a URL that must be downloaded by adding it to the CSV list for the applet
				if (numToDownload > 0) out.append(",");
				out.append(StringUtils.htmlEncode(paramName));
				numToDownload++;
			}
		}
		out.append("\", ");

		// parameter names
		out.append("\"");
		if (parameterInfo != null) {
		      	for (i = 0; i < parameterInfo.length; i++) {
				paramName = parameterInfo[i].getName();
				bNeedsQuoting = !GenePatternAnalysisTask.isRSafe(paramName);
				//if (bNeedsQuoting) paramName = "\"" + paramName + "\"";
				if (i > 0) out.append(",");
				out.append(paramName);
	      		}
		}
		out.append("\", ");

		// have to break the line here so it doesn't get too long for R to handle!
		out.append("\n\t\t\t");

		// support filenames
		out.append("\"");
		for (i = 0; i < supportFiles.length; i++) {
			if (i > 0) out.append(",");
			out.append(StringUtils.htmlEncode(supportFiles[i].getName()));
		}
		out.append("\", ");

		// support file modification dates
		out.append("\"");
		for (i = 0; i < supportFiles.length; i++) {
			if (i > 0) out.append(",");
			out.append(supportFiles[i].lastModified());
		}
		out.append("\", ");

		// visualizer LSID
		out.append("LSID, ");
		
		// now all of the individual parameters...
	} else { // not visualizer
		out.append("runAnalysis(taskName=LSID, ");
		i = 1;
	}
	if (parameterInfo != null) {
	      	for (i = 0; i < parameterInfo.length; i++) {
			String pname = parameterInfo[i].getName();
			bNeedsQuoting = !GenePatternAnalysisTask.isRSafe(pname);
			if (bNeedsQuoting) pname = "\"" + pname + "\"";
			if (i > 0) out.append(", ");
			out.append(pname + "=" + parameterInfo[i].getName().replace('_','.'));
      		}
	}
	if (!hasServer) out.append((i > 0 ? ", " : "") + "server=server");
	out.append(")) }");

	return out.toString();
}
%>
<%! public HashMap computeDefaultLSIDs(Collection tmTasks) throws MalformedURLException {
	// compute default (latest) LSIDs for each unique task name
	// When multiple authorities offer the same module, preference is mine > Broad > foreign
	// Within an authority, choose the greatest identifier
	// Within an identifier, choose the greatest version

	TaskInfo taskInfo = null;
	TaskInfoAttributes taskInfoAttributes = null;
	String sLSID = null;
	LSID lsid = null;
	HashMap hmTasks = new HashMap();
	LSIDManager lsidManager = LSIDManager.getInstance();

	for (Iterator itTasks = tmTasks.iterator(); itTasks.hasNext(); ) {
		taskInfo = (TaskInfo)itTasks.next();
		taskInfoAttributes = taskInfo.giveTaskInfoAttributes();
		sLSID = (String)taskInfoAttributes.get(GPConstants.LSID);
		lsid = new LSID(sLSID);
		//System.out.println(taskInfo.getName() + ": " + sLSID + ": ");
		// seen this LSID (without a version) before?
		if (!hmTasks.containsKey(taskInfo.getName())) {
			hmTasks.put(taskInfo.getName(), sLSID);
			//System.out.println("new");
			continue;
		}
		LSID previousLSID = new LSID((String)hmTasks.get(taskInfo.getName()));
		LSID nearerLSID = lsidManager.getNearerLSID(lsid, previousLSID);
		if (nearerLSID.equals(lsid)) {
			hmTasks.put(taskInfo.getName(), sLSID);
			continue;
		}
	}
	return hmTasks;
}
%>
<%! public String makePaste(String line) { 
	StringBuffer ret = new StringBuffer();
	int CHUNK_SIZE = 512;
	if (line.length() < CHUNK_SIZE) {
		ret.append("\"");
		ret.append(line);
		ret.append("\"");
	} else {
		int i = 0;
		for (; i < line.length(); i += CHUNK_SIZE) {
			if (ret.length() == 0) {
				ret.append("paste(\"");
			} else {
				ret.append("\",\n\t\t\t\"");
			}
			ret.append(line.substring(i, Math.min(i + CHUNK_SIZE, line.length())));
		}
		if (i < line.length()) {
			ret.append("\",\n\t\t\t\"");
			ret.append(line.substring(i));
		}
		ret.append("\", sep=\"\", collapse=\"\")\n\t\t\t");
	}
	return ret.toString();
    }
%>