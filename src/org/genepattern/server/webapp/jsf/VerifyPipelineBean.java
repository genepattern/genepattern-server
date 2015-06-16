/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.server.webapp.jsf;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.genepattern.codegenerator.AbstractPipelineCodeGenerator;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.WebServiceException;
import org.json.JSONArray;

/**
 * 
 * @author jgould
 * 
 */
public class VerifyPipelineBean {

    private static final Logger log = Logger.getLogger(VerifyPipelineBean.class);

    /**
     * Verifies that required parameters for a pipeline have been provided.
     * 
     * This really should just be an action method, but the ajax servlet is configured to get properties from beans, not
     * call action methods. So the method has to follow java bean property conventions (get...). *
     * 
     * 
     */
    public String getVerifyPipeline() {
	String returnValue = verifyPipeline().toString();
	return returnValue;
    }

    private static String getParameter(HttpServletRequest request, String name) {
	String value = request.getParameter(name);
	if (value != null) {
	    value = value.trim();
	}
	return value;
    }

    private JSONArray verifyPipeline() {
	List<String> problems = new ArrayList<String>();
	HttpServletRequest request = UIBeanHelper.getRequest();

	// for (Enumeration e = request.getParameterNames(); e.hasMoreElements();) {
	// String n = (String) e.nextElement();
	// log.error(n + " -> " + getParameter(request, n) + "end");
	// }
	String pipelineName = getParameter(request, "pipeline_name");
	if ((pipelineName == null || pipelineName.trim().length() == 0)) {
	    problems.add("Pipeline must be named");
	    return new JSONArray(problems);
	}

	if (pipelineName.endsWith("." + GPConstants.TASK_TYPE_PIPELINE))
	    pipelineName = pipelineName.substring(0, pipelineName.lastIndexOf("."));

	// transform requestParameters into model data
	String taskName;
	String taskPrefix = null;
	String taskLSID;

	Map<String, TaskInfo> taskCatalog;
	try {
	    taskCatalog = new LocalAdminClient(getParameter(request, GPConstants.USERID)).getTaskCatalogByLSID();
	} catch (WebServiceException e) {
	    log.error("Error", e);
	    JSONArray array = new JSONArray();
	    array.put("Error getting modules from database.");
	    return array;
	}

	PipelineModel model = new PipelineModel();

	String version = getParameter(request, GPConstants.VERSION);

	model.setDescription(getParameter(request, "pipeline_description"));
	model.setAuthor(getParameter(request, "pipeline_author"));
	String lsid = getParameter(request, GPConstants.LSID);

	if (lsid == null) {
	    lsid = "";
	}
	model.setLsid(lsid);
	model.setVersion(version);
	String display = getParameter(request, "display");
	if (display == null || display.length() == 0) {
	    display = getParameter(request, "custom");
	}
	model.setUserID(getParameter(request, GPConstants.USERID));
	String privacy = getParameter(request, GPConstants.PRIVACY);
	model.setPrivacy(privacy != null && privacy.equals(GPConstants.PRIVATE));

	for (int taskNum = 0;; taskNum++) {
	    taskPrefix = "t" + taskNum;
	    taskLSID = getParameter(request, taskPrefix + "_taskLSID");
	    taskName = getParameter(request, taskPrefix + "_taskName");
	    if (taskName == null || taskName.length() == 0)
		break;
	    TaskInfo mTaskInfo = null;
	    if (taskLSID != null && taskLSID.length() > 0) {
		mTaskInfo = taskCatalog.get(taskLSID);
	    }
	    if (mTaskInfo == null) {
		mTaskInfo = taskCatalog.get(taskName);
	    }
	    if (mTaskInfo == null) {
		problems.add("Couldn't find module number " + taskNum + " searching for name " + taskName + " or LSID "
			+ taskLSID);
		continue;
	    }
	    ParameterInfo[] params = mTaskInfo.getParameterInfoArray();
	    boolean[] runTimePrompt = (params != null ? new boolean[params.length] : null);

	    if (params != null) {
		for (int i = 0; i < params.length; i++) {
		    ParameterInfo p = params[i];
		    String paramName = p.getName();
		    String origValue = p.getValue();

		    String paramKey = taskPrefix + "_" + paramName;

		    String val = getParameter(request, paramKey);

		    if (val == null || val.length() == 0) {
			paramKey = taskName + (taskNum + 1) + "." + taskPrefix + "_" + paramName;
			val = getParameter(request, paramKey);
		    }
		    if (val != null) {
			val = GenePatternAnalysisTask.replace(val, "\\", "\\\\");
		    }
		    p.setValue(val);

		    String promptWhenRun = getParameter(request, taskPrefix + "_prompt_" + i);
		    runTimePrompt[i] = promptWhenRun != null && promptWhenRun.trim().length() > 0;

		    // if (runTimePrompt[i]) {
		    // // get an alternate name for the param
		    // String altNameKey = taskPrefix + "_" + paramName + "_altName";
		    // String altName = getParameter(request, altNameKey);
		    // String altDescKey = taskPrefix + "_" + paramName + "_altDescription";
		    // String altDesc = getParameter(request, altDescKey);
		    //
		    // if (altName != null && altName.length() > 0) {
		    // p.getAttributes().put("altName", altName);
		    // }
		    // if (altDesc != null && altDesc.length() > 0) {
		    // p.getAttributes().put("altDescription", altDesc);
		    // }
		    //
		    // }

		    String inheritFrom = getParameter(request, taskPrefix + "_i_" + i);

		    // safari comes in with '[module not set]' because it just needs to be different
		    boolean inherited = (inheritFrom != null && inheritFrom.length() > 0
			    && !inheritFrom.equals("NOT SET") && !inheritFrom.startsWith("[module"));

		    boolean optional = (((String) p.getAttributes().get(GPConstants.PARAM_INFO_OPTIONAL[0])).length() > 0);

		    String inheritedTaskNum = null;
		    String inheritedFilename = null;
		    // inheritance has priority over run time prompt
		    if (inherited) {
			runTimePrompt[i] = false;
			inheritedTaskNum = getParameter(request, taskPrefix + "_i_" + i);

			String inheritFromFile = getParameter(request, taskPrefix + "_if_" + i);
			if ((inheritFromFile != null && inheritFromFile.length() > 0)
				&& (!inheritFromFile.startsWith("[module")) && !inheritFrom.equals("NOT SET")) {
			    inheritedFilename = "" + inheritFromFile;
			} else {
			    problems.add("Step " + (taskNum + 1) + ", " + taskName + ", is missing required parameter "
				    + p.getName().replace('.', ' '));

			}
		    }

		    if (runTimePrompt[i]) {
			p.getAttributes().put("runTimePrompt", "1");
			model.addInputParameter(taskName + (taskNum + 1) + "." + paramName, p);
		    } else {
			p.getAttributes().put("runTimePrompt", null);

		    }

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
			    String shadow = getParameter(request, shadowName);

			    if (shadow != null
				    && shadow.length() > 0
				    && (shadow.startsWith("http:") || shadow.startsWith("https:")
					    || shadow.startsWith("ftp:") || shadow.startsWith("file:") || shadow
					    .startsWith("<GenePatternURL>"))) {

				p.setValue(shadow);
			    }

			}
		    }

		    // if runtime prompt, save choice list for display later
		    if (runTimePrompt[i]) {
			p.setValue(origValue);
		    }

		    if (p.getValue() != null) {
			p.setValue(p.getValue().trim());
		    }
		    if (!optional && !inherited && !runTimePrompt[i]
			    && (p.getValue() == null || p.getValue().length() == 0)) {
			problems.add("Step " + (taskNum + 1) + ", " + taskName + ", is missing required parameter "
				+ p.getName().replace('.', ' '));
		    }
		}
	    }
	}

	return (problems.size() == 0) ? new JSONArray() : new JSONArray(problems);
    }
    
    public boolean getUseBeta() {
        return ServerConfigurationFactory.instance().getGPBooleanProperty(UIBeanHelper.getUserContext(), "pipeline.useBeta", false);
    }
}
