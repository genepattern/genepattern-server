<%--
  ~ Copyright 2012 The Broad Institute, Inc.
  ~ SOFTWARE COPYRIGHT NOTICE
  ~ This software and its documentation are the copyright of the Broad Institute, Inc. All rights are reserved.
  ~
  ~ This software is supplied without any warranty or guaranteed support whatsoever. The Broad Institute is not responsible for its use, misuse, or functionality.
  --%>


<%@ page import="org.genepattern.server.webapp.*,
                 org.genepattern.data.pipeline.*,
                 java.io.File,
                 java.io.FilenameFilter,
                 java.io.FileInputStream,
                 java.io.FileOutputStream,
                 java.io.FileWriter,
                 java.io.InputStream,
                 java.io.IOException,
                 java.lang.reflect.Constructor,
                 java.net.URLEncoder,
                 java.util.ArrayList,
                 java.util.Collection,
                 java.util.Enumeration,
                 java.util.HashMap,
                 java.util.Properties,
                 java.util.Hashtable,
                 java.util.Iterator,
                 java.util.List,
                 java.util.Map,
                 java.util.StringTokenizer,
                 java.util.TreeMap,
                 java.util.Vector,
                 java.nio.channels.FileChannel,
                 org.genepattern.webservice.ParameterFormatConverter,
                 org.genepattern.webservice.ParameterInfo,
                 org.genepattern.webservice.TaskInfo,
                 org.genepattern.webservice.TaskInfoAttributes,
                 org.genepattern.server.util.AccessManager,
                 org.genepattern.server.genepattern.GenePatternAnalysisTask,
                 org.genepattern.server.webservice.server.local.*,
                 org.genepattern.server.genepattern.TaskInstallationException,
                 org.genepattern.server.webservice.server.local.IAdminClient,
                 org.genepattern.server.webservice.server.local.LocalAdminClient,
                 org.genepattern.server.webservice.server.DirectoryManager,
                 org.genepattern.data.pipeline.*,
                 org.genepattern.util.GPConstants,
                 org.genepattern.util.StringUtils,
                 org.genepattern.codegenerator.*,
                 org.apache.commons.fileupload.DiskFileUpload,
                 org.apache.commons.fileupload.FileItem,
                 org.apache.commons.fileupload.FileUpload,
                 java.io.StringWriter"

         session="false" contentType="text/html" language="Java" %>
<%@ page import="org.genepattern.server.config.ServerConfigurationFactory" %>
<%@ page import="org.genepattern.server.config.GpContext" %>
<%
    response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
    response.setHeader("Pragma", "no-cache");         // HTTP 1.0 cache control
    response.setDateHeader("Expires", 0);

    Properties requestParameters = new Properties();
    HashMap requestFiles = new HashMap();
    DiskFileUpload fub = new DiskFileUpload();
    boolean isEncodedPost = FileUpload.isMultipartContent(request);
    List rParams = fub.parseRequest(request);
    int fileCount = 0;
    try {
        for (Iterator iter = rParams.iterator(); iter.hasNext(); ) {
            FileItem fi = (FileItem) iter.next();

            if (fi.isFormField()) {
                requestParameters.put(fi.getFieldName(), fi.getString());
                //System.out.println("TestP " +  fi.getFieldName()+" = "+ fi.getString());

            } else {
                //System.out.println("-- TestP " +  fi.getFieldName());


                // it is the file
                fileCount++;
                String name = fi.getName();

                if (name == null || name.equals("")) {
                    continue;
                }
                // strip out paths on IE -- BUG 1819
                int idx = name.lastIndexOf('/');
                if (idx >= 0) name = name.substring(idx + 1);
                idx = name.lastIndexOf('\\');
                if (idx >= 0) name = name.substring(idx + 1);


                File aFile = new File(ServerConfigurationFactory.instance().getTempDir(GpContext.getServerContext()), name);
                requestFiles.put(fi.getFieldName(), aFile);

                fi.write(aFile);

            }
        }
    } catch (Exception eee) {
        eee.printStackTrace();
    }

    String userID = null;
    boolean bRun = false;
    boolean bClone = false;
    boolean bDelete = false;

    try {
        // Initialization
        userID = requestParameters.getProperty(GPConstants.USERID);

        String RUN = "run";
        String CLONE = "clone";

        boolean DEBUG = false; // (requestParameters.getProperty("debug") != null);

        if (DEBUG) {
            System.out.println("\n\nMAKEPIPELINE Request parameters:<br>");
            System.out.println(requestParameters);
            System.out.println(requestFiles);
        }

        bRun = requestParameters.getProperty("cmd").equals(RUN);
        bClone = requestParameters.getProperty("cmd").equals(CLONE);
        bDelete = requestParameters.getProperty("delete") != null;


        String pipelineName = requestParameters.getProperty("pipeline_name");


        if (bDelete) {
            try {
                TaskInfo taskInfo = GenePatternAnalysisTask.getTaskInfo(requestParameters.getProperty("changePipeline"), userID);
                String lsid = (String) taskInfo.getTaskInfoAttributes().get(GPConstants.LSID);
                String attachmentDir = DirectoryManager.getTaskLibDir(requestParameters.getProperty("pipeline_name"), lsid, userID); // + "." + GPConstants.TASK_TYPE_PIPELINE);
                File dir = new File(attachmentDir);
                try {
                    GenePatternAnalysisTask.deleteTask(lsid);
                } catch (Exception oe) {
                    // ignore, probably already deleted
                }

                // clear out the directory
                File[] oldFiles = dir.listFiles();
                for (int i = 0; oldFiles != null && i < oldFiles.length; i++) {
                    oldFiles[i].delete();
                }
                dir.delete();
%>
<html>
    <head>
        <link href="skin/stylesheet.css" rel="stylesheet" type="text/css">
        <link href="skin/favicon.ico" rel="shortcut icon">
        <title>Delete pipeline</title>
        <jsp:include page="navbarHead.jsp" />
    </head>
    <body>

        <jsp:include page="navbar.jsp" />
        Stopped and deleted <%= taskInfo.getName() %> along with its support files.<br><br>
            <%
		} catch (Throwable t) {
			out.println(t + " while attempting to delete " + pipelineName);
		}
		return;
	}

	if (pipelineName.endsWith("." + GPConstants.TASK_TYPE_PIPELINE)) pipelineName = pipelineName.substring(0, pipelineName.lastIndexOf("."));
	if (bRun && (pipelineName == null || pipelineName.trim().length() == 0)) {
		pipelineName = "unnamed" + "." + GPConstants.TASK_TYPE_PIPELINE;
	}
	

	if (!bRun && (pipelineName == null || pipelineName.trim().length() == 0)) {
%>
        <html>
            <head>
                <link href="skin/stylesheet.css" rel="stylesheet" type="text/css">
                <link href="skin/favicon.ico" rel="shortcut icon">
                <title>Delete pipeline</title>
                <jsp:include page="navbarHead.jsp" />

            </head>
            <body>
                <jsp:include page="navbar.jsp" />
                Error: pipeline must be named.
                <a href="javascript:window.close()">back</a>
                    <%
		return;
	}

	Hashtable htFilenames = new Hashtable(); // map form field names to filenames for attached (fixed) files

	// tranform requestParameters into model data
	String taskName;
	String taskPrefix = null;
	String taskLSID;
	int numParameterInfos;
	String key = null;
	String value = null;
	TaskInfo pTaskInfo = new TaskInfo();
	TaskInfoAttributes pTia = null;
	Vector vProblems = new Vector();
    File tmpDir = ServerConfigurationFactory.instance().getTempDir(GpContext.getServerContext());

	IAdminClient adminClient = new LocalAdminClient(requestParameters.getProperty(GPConstants.USERID));
	Map taskCatalog = adminClient.getTaskCatalogByLSID();

	PipelineModel model = new PipelineModel();
	model.setAdminClient(adminClient);
	// NB: could use any language Fcode generator here
	String language = requestParameters.getProperty(GPConstants.LANGUAGE);
	if (language == null) language = "R";
	String version = requestParameters.getProperty(GPConstants.VERSION);

	JobSubmission jobSubmission = null;
	String paramName = null;
	String modelName = pipelineName;
	if (modelName == null || modelName.equals("")) {
		if (bRun) {
			modelName = "unnamed";
		} else {
			modelName = "p" + Double.toString(Math.random()).substring(2);
		}
	}
	model.setName(modelName);
	model.setDescription(requestParameters.getProperty("pipeline_description"));
	model.setAuthor(requestParameters.getProperty("pipeline_author"));
	boolean isTemp = modelName.startsWith("try.") || bRun;
	String lsid = requestParameters.getProperty(GPConstants.LSID);
	String origLSID = lsid;

	if (lsid == null) {
		lsid = "";
	}
	model.setLsid(lsid);
	model.setVersion(version);
	String display = requestParameters.getProperty("display");
	if (display == null || display.length() == 0) display = requestParameters.getProperty("custom");
	model.setUserID(requestParameters.getProperty(GPConstants.USERID));
	String privacy = requestParameters.getProperty(GPConstants.PRIVACY);
	model.setPrivacy(privacy != null && privacy.equals(GPConstants.PRIVATE));

	PipelineCreationHelper controller = new PipelineCreationHelper(model);
	lsid = controller.generateLSID();


	// save uploaded files as part of pipeline definition
	if (fileCount > 0) {
		String attachmentDir = null;
		File dir = null;
		String attachmentName = null;

		File attachedFile = null;
		for (Iterator iter = requestFiles.keySet().iterator(); iter.hasNext(); ){
			key = (String)iter.next();

			attachedFile = (File)requestFiles.get(key);
			if (DEBUG) System.out.println("\n=>   '" + attachedFile.getName() +"'  "+ attachedFile.exists());

			if (!attachedFile.exists()) continue;
			attachmentName = attachedFile.getName();
			if (attachmentName.trim().length() == 0) continue;
			String fieldName = key;
			String fullName = fieldName; // attachedFile.getFilePathName();
			if (fullName.startsWith("http:") || fullName.startsWith("https:") || fullName.startsWith("ftp:") || fullName.startsWith("file:") || (fullName.startsWith("<GenePatternURL>"))) {
				// don't bother trying to save a file that is a URL, retrieve it at execution time instead
				htFilenames.put(fieldName, fullName); // map between form field name and filesystem name

				continue;
			}

			if (isTemp) {
				// leave the task name blank for getFile and put the file into the temp directory
				model.setLsid("");
				// it's for a temporary pipeline
				dir = tmpDir;
			}
			htFilenames.put(fieldName, "<GenePatternURL>getFile.jsp?task=" + URLEncoder.encode(lsid) + "&file=" + URLEncoder.encode(attachmentName)); // map between form field name and filesystem name
		}
	}

	for (int taskNum = 0; ; taskNum++) {
		taskPrefix = "t" + taskNum;
		taskLSID = requestParameters.getProperty(taskPrefix + "_taskLSID");
		taskName = requestParameters.getProperty(taskPrefix + "_taskName");
		if (taskName == null) break;
		TaskInfo mTaskInfo = null;
		if (taskLSID != null && taskLSID.length() > 0) {
			mTaskInfo = (TaskInfo)taskCatalog.get(taskLSID);
		}
		if (mTaskInfo == null) {
			mTaskInfo = (TaskInfo)taskCatalog.get(taskName);
		}
		if (mTaskInfo == null) {
			vProblems.add("makePipeline: couldn't find module number " + taskNum + " searching for name " + taskName + " or LSID " + taskLSID);
			continue;
		}
		TaskInfoAttributes mTia = mTaskInfo.giveTaskInfoAttributes();
		if (DEBUG) out.println("<br>" + mTaskInfo.getName() + "<br>");

		ParameterInfo[] params = ParameterFormatConverter.getParameterInfoArray(mTaskInfo.getParameterInfo());
		ParameterInfo p = null;
		boolean[] runTimePrompt = (params != null ? new boolean[params.length] : null);
		String inheritedTaskNum = null;
		String inheritedFilename = null;
		String origValue = null;
		if (params != null) {
			for (int i = 0; i < params.length; i++) {
				p = params[i];
				paramName = p.getName();
				origValue = p.getValue();

				String paramKey = taskPrefix + "_" + paramName;


				String val = requestParameters.getProperty(paramKey);


				if (val == null) {
					paramKey = taskName + (taskNum+1) + "." + taskPrefix + "_" + paramName;
					val = requestParameters.getProperty(paramKey);
				}
				if (val != null) val = GenePatternAnalysisTask.replace(val, "\\", "\\\\");

				p.setValue(val);

				runTimePrompt[i] = (requestParameters.getProperty(taskPrefix + "_prompt_" + i) != null);

				if (runTimePrompt[i]){
					// get an alternate name for the param
					String altNameKey = taskPrefix + "_" + paramName + "_altName";
					String altName = requestParameters.getProperty(altNameKey);
					String altDescKey = taskPrefix + "_" + paramName + "_altDescription";
					String altDesc = requestParameters.getProperty(altDescKey);


					if (altName != null){
						p.getAttributes().put("altName", altName);
					}
					if (altDesc != null){
						p.getAttributes().put("altDescription", altDesc);
					}

				}

				String inheritFrom = requestParameters.getProperty(taskPrefix + "_i_" + i);


				// safari comes in with '[module not set]' because it just needs to be different
				boolean inherited = (inheritFrom != null && inheritFrom.length() > 0 && !inheritFrom.equals("NOT SET") && !inheritFrom.startsWith("[module")  );

				boolean isOptional = (((String)p.getAttributes().get(GPConstants.PARAM_INFO_OPTIONAL[0])).length() > 0);

//System.out.println(taskName + ": " + paramName + "=" + val + ", prompt= " + runTimePrompt[i] + ", optional=" + isOptional + ", inherited=" + inherited + " (" + requestParameters.getProperty(taskPrefix + "_i_" + i) + "), isInputFile=" + p.isInputFile());

				// inheritance has priority over run time prompt
				if (inherited) {
					runTimePrompt[i] = false;
					inheritedTaskNum = requestParameters.getProperty(taskPrefix + "_i_" + i);
					inheritedFilename = null;
					String inheritFromFile = requestParameters.getProperty(taskPrefix + "_if_" + i);
					if ((inheritFromFile != null) && (!inheritFromFile.startsWith("[module")) && !inheritFrom.equals("NOT SET")  ){
						inheritedFilename = ""+ inheritFromFile ;
					} else {
						vProblems.add("Step " + (taskNum+1) + ", " + taskName + ", is missing required parameter " + p.getName());

					}
				}

				if (runTimePrompt[i]) {
					p.getAttributes().put("runTimePrompt", "1");
					model.addInputParameter(taskName + (taskNum + 1) + "." + paramName, p);
				} else {
					p.getAttributes().put("runTimePrompt", null);

				}

				if (inheritedTaskNum != null || inheritedFilename != null) {
					if (DEBUG) out.println(taskPrefix + "_i_" + i + "=" + requestParameters.getProperty(taskPrefix + "_i_" + i) + "<br>");
					if (DEBUG) out.println(taskPrefix + "_if_" + i + "=" + requestParameters.getProperty(taskPrefix + "_if_" + i) + "<br>");
				}

				if (DEBUG) out.println(paramName + " is " + (inherited ? "" : "not ") + " inherited and is " + (runTimePrompt[i] ? "" : "not ") + " runtime-prompted<br>");

				// inheritance and run time prompt both have priority over explicitly named input file
				if (inherited || runTimePrompt[i]) {
					p.setValue("");
				}
				if (p.isInputFile()) {
					if (inherited) {
						p.getAttributes().put(AbstractPipelineCodeGenerator.INHERIT_FILENAME, inheritedFilename);
						p.getAttributes().put(AbstractPipelineCodeGenerator.INHERIT_TASKNAME, inheritedTaskNum);
					} else {
						String shadowName = taskPrefix + "_shadow" + i;
						String shadow = requestParameters.getProperty(shadowName);

						if (shadow != null && (shadow.startsWith("http:") || shadow.startsWith("https:") || shadow.startsWith("ftp:") || shadow.startsWith("file:") || shadow.startsWith("<GenePatternURL>"))) {

							// if this is a URL that is in the taskLib, repoint it to the cloned taskLib
							if (bClone) {
								String taskFile = "<GenePatternURL>getFile.jsp?task=" + URLEncoder.encode(lsid) + "&file=";
								if (shadow.startsWith(taskFile)) {
									taskFile = shadow.substring(taskFile.length());
									// use clone's LSID, not the name
									shadow = "<GenePatternURL>getFile.jsp?task=" + URLEncoder.encode(lsid) + "&file=" + URLEncoder.encode(taskFile, "UTF-8");
								}
							}
							htFilenames.put(taskPrefix + "_" + paramName, shadow);
						}
                                    String filenameKey = taskPrefix + "_" + paramName;


						p.setValue((String)htFilenames.get(filenameKey));

					}
				}

				// if runtime prompt, save choice list for display later
				if (runTimePrompt[i]) {
					p.setValue(origValue);
				}

				// else {
					//p.getAttributes().put("runTimePrompt", null);
				//}
				if (!inherited && !runTimePrompt[i] && (p.getValue() == null || p.getValue().equals("")) && !isOptional) {
					vProblems.add("Step " + (taskNum+1) + ", " + taskName + ", is missing required parameter " + p.getName());
				}
			}
		}


		boolean isVisualizer = ((String)mTia.get(GPConstants.TASK_TYPE)).equals(GPConstants.TASK_TYPE_VISUALIZER);
		jobSubmission = new JobSubmission(taskName, mTaskInfo.getDescription(), taskLSID, params, runTimePrompt, isVisualizer, mTaskInfo);


		model.addTask(jobSubmission);
	}

	if ((!bRun && !bClone) || vProblems.size() > 0) {
%>
                <html>
                    <head>
                        <link href="skin/stylesheet.css" rel="stylesheet" type="text/css">
                        <link href="skin/favicon.ico" rel="shortcut icon">
                        <title><%= pipelineName %> - saved</title>
                        <script language="Javascript">window.focus();</script>
                        <jsp:include page="navbarHead.jsp" />
                    </head>
                    <body>
                        <jsp:include page="navbar.jsp" />
                        <%
                            }
                            if (controller == null)
                                controller = new PipelineCreationHelper(model);

                            //lsid = null;
                            if (vProblems.size() == 0) {
                                String oldLSID = origLSID;

                                if (bClone) {
                                    model.setName(requestParameters.getProperty("cloneName"));
                                    // TODO: change URLs that are task-relative to point to the new task
                                    String oldUser = model.getUserID();
                                    String requestUserID = (String) request.getAttribute("userID");
                                    if (oldUser.length() > 0 && !oldUser.equals(requestUserID)) {
                                        oldUser = " (" + oldUser + ")";
                                    } else {
                                        oldUser = "";
                                    }
                                    model.setUserID(requestUserID + oldUser);
                                    model.setLsid("");
                                }


                                if (!bRun) {
                                    // save the task to the database
                                    try {


                                        lsid = controller.generateTask(); ///  MP 3
                                        model.setLsid(lsid);

                                        if (bClone || !"".equals(oldLSID)) {
                                            // TODO: change URLs that are task-relative to point to the new task
                                            //System.out.println("copying support files from " + oldLSID + " to " + lsid);
                                            copySupportFiles(modelName, model.getName(), oldLSID, lsid, userID);
                                        }
                                    } catch (TaskInstallationException tie) {
                                        vProblems.addAll(tie.getErrors());
                                    }
                                }

                                // save uploaded files as part of pipeline definition
                                if (fileCount > 0) {
                                    String attachmentDir = null;
                                    File dir = null;
                                    String attachmentName = null;

                                    if (!isTemp) {

                                        attachmentDir = DirectoryManager.getTaskLibDir(modelName, lsid, userID);
                                        // bug 1555 // attachmentDir = DirectoryManager.getTaskLibDir(modelName + "." + GPConstants.TASK_TYPE_PIPELINE, lsid, userID);
                                        dir = new File(attachmentDir);
                                        dir.mkdir();
                                    } else {
                                        model.setLsid("");
                                        dir = tmpDir;
                                    }
                                    File attachedFile = null;
                                    for (Iterator iter = requestFiles.keySet().iterator(); iter.hasNext(); ) {
                                        key = (String) iter.next();
                                        attachedFile = (File) requestFiles.get(key);

                                        if (!attachedFile.exists()) continue;
                                        try {
                                            attachmentName = attachedFile.getName();
                                            if (attachmentName.trim().length() == 0) continue;
                                            String fieldName = key;
                                            String fullName = attachedFile.getCanonicalPath();
                                            if (DEBUG) System.out.println("makePipeline: " + fieldName + " -> " + fullName);
                                            if (fullName.startsWith("http:") || fullName.startsWith("https:") || fullName.startsWith("ftp:") || fullName.startsWith("file:")) {
                                                // don't bother trying to save a file that is a URL, retrieve it at execution time instead
                                                htFilenames.put(fieldName, fullName); // map between form field name and filesystem name
                                                continue;
                                            }

                                            htFilenames.put(fieldName, "<GenePatternURL>getFile.jsp?task=" + URLEncoder.encode(lsid) + "&file=" + URLEncoder.encode(attachmentName)); // map between form field name and filesystem name


                                            if (dir != tmpDir) {
                                                File attachment = new File(dir, attachedFile.getName());
                                                if (attachment.exists()) attachment.delete();

                                                FileChannel inChannel = null, outChannel = null;
                                                try {
                                                    inChannel = new FileInputStream(attachedFile).getChannel();
                                                    outChannel = new FileOutputStream(attachment).getChannel();
                                                    outChannel.transferFrom(inChannel, 0, inChannel.size());
                                                } finally {
                                                    if (inChannel != null) inChannel.close();
                                                    if (outChannel != null) outChannel.close();
                                                }

                                            }


                                        } catch (IOException sue) {
                                            throw new Exception("error saving " + attachmentName + ": " + sue.getMessage());
                                        }
                                    }
                                }

                                // run immediately, without saving?
                                if (bRun) {
                                    vProblems.add("run immediately no longer allowed!");
                                }
                            }

                            if (vProblems.size() > 0) {
                        %>
                        There are some problems with the <%= model.getName() %> pipeline description that need to be fixed:<br>
                        <ul>
                            <%
                                for (Enumeration eProblems = vProblems.elements(); eProblems.hasMoreElements(); ) {
                            %>
                            <li><%= StringUtils.htmlEncode((String) eProblems.nextElement()) %>
                            </li>
                            <%
                                }
                            %>
                        </ul>
                        <a href="javascript:history.back()">back</a><br>
                        <script language="javascript">
                            window.opener.focus();
                            window.toolbar.visibility = false;
                            window.personalbar.visibility = false;
                            window.menubar.visibility = false;
                            window.locationbar.visibility = false;
                            window.focus();
                        </script>
                        <%
                                return;
                            } else {
                                // delete the legacy R file for the pipeline, if it exists

                                pipelineName = model.getName();
                                // bug 1555 // pipelineName = model.getName() + "." + GPConstants.TASK_TYPE_PIPELINE;

                                String dir = DirectoryManager.getTaskLibDir(pipelineName, lsid, userID);
                                out.println(model.getName() + " version " + new org.genepattern.util.LSID(model.getLsid()).getVersion() + " has been saved.<br><br>");
                                new File(dir, model.getName() + ".r").delete();
                                if (requestParameters.getProperty("cmd").equals(CLONE)) {
                                    response.sendRedirect("pipeline/index.jsf?lsid=" + lsid);
                                    return;
                                }

                                if (requestParameters.getProperty("autoSave").length() > 0) {
                                    out.println("<script language=\"Javascript\">window.close();</script>");
                                }

                                out.println("&nbsp;&nbsp;<a href='" + request.getContextPath() + "/pages/index.jsf?lsid=" + lsid + "'>Continue to Modules & Pipeline Start.</a>");
                                out.println("<br />");


                            }
                        } catch (Exception e) {
                        %>
                        makePipeline failed: <br>
                        <%= e.getMessage() %><br>
	<pre>
	<% e.printStackTrace(); %>
	</pre>
                        <br>
                        <a href="javascript:history.back()">back</a><br>
                        <%
                        } finally {
                            if (!bClone) {
                        %>
                        <jsp:include page="footer.jsp" />
                    </body>
                </html>
                    <%
	}
} %>
                    <%! void copySupportFiles(String oldTaskName, String newTaskName, String oldLSID, String newLSID, String userID) throws Exception {
	//copySupportFiles(modelName, model.getName(), oldLSID, lsid, userID);
	//DirectoryManager.getTaskLibDir(modelName + "." + GPConstants.TASK_TYPE_PIPELINE, lsid, userID);



	String oldDir = DirectoryManager.getTaskLibDir(oldTaskName + "." + GPConstants.TASK_TYPE_PIPELINE, oldLSID, userID);
	String newDir = DirectoryManager.getTaskLibDir(newTaskName + "." + GPConstants.TASK_TYPE_PIPELINE, newLSID, userID);


	File[] oldFiles = new File(oldDir).listFiles(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return (!name.endsWith(".old") && !name.equals("version.txt"));
				} });
	byte[] buf = new byte[100000];
	int j;
	for (int i=0; oldFiles != null && i < oldFiles.length; i++) {
		FileInputStream is = new FileInputStream(oldFiles[i]);
		FileOutputStream os = new FileOutputStream(new File(newDir, oldFiles[i].getName()));
		while ((j = is.read(buf, 0, buf.length)) > 0) {
			os.write(buf, 0, j);
		}
		is.close();
		os.close();
	}
    }
%>
