package org.genepattern.server.webapp;

import java.io.PrintStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Properties;

import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.util.StringUtils;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;

public class RunPipelineNullDecorator implements RunPipelineOutputDecoratorIF {
	PipelineModel model = null;
 	RunPipelineExecutionLogger logger =  new RunPipelineExecutionLogger();


	Properties genepatternProps = null;

	public void setOutputStream(PrintStream outstr) {
	}

	public void beforePipelineRuns(PipelineModel model) {
		this.model = model;
		try {
			String genePatternPropertiesFile = System.getProperty("genepattern.properties") +  java.io.File.separator + "genepattern.properties";
     		java.io.FileInputStream fis = new java.io.FileInputStream(genePatternPropertiesFile);
      		genepatternProps = new Properties();
      		genepatternProps.load(fis);
      		fis.close();
     	} catch(Exception e) {
     		genepatternProps = new Properties();
     	}
      logger.setRegisterExecutionLog(false);
		logger.beforePipelineRuns(model);

	}

	public void recordTaskExecution(JobSubmission jobSubmission, int idx,
			int numSteps) {
		ParameterInfo[] parameterInfo = jobSubmission.giveParameterInfoArray();
		for (int i = 0; i < parameterInfo.length; i++) {
			ParameterInfo aParam = parameterInfo[i];
			boolean isInputFile = aParam.isInputFile();
			HashMap hmAttributes = aParam.getAttributes();
			String paramType = null;
			if (hmAttributes != null)
				paramType = (String) hmAttributes.get(ParameterInfo.TYPE);
			if (!isInputFile && !aParam.isOutputFile() && paramType != null
					&& paramType.equals(ParameterInfo.FILE_TYPE)) {
				isInputFile = true;
			}
			isInputFile = (aParam.getName().indexOf("filename") != -1);

			if (isInputFile) {
				// convert from "localhost" to the actual host name so that
				// it can be referenced from anywhere (eg. visualizer on
				// non-local client)
				aParam.setValue(localizeURL(aParam.getValue()));

			}
		}
		logger.recordTaskExecution(jobSubmission, idx, numSteps);

	}

	public void recordTaskCompletion(JobInfo jobInfo, String name) {
  		logger.recordTaskCompletion(jobInfo, name);

	}

	public void afterPipelineRan(PipelineModel model) {
		logger.afterPipelineRan(model);

	}

	protected String localizeURL(String original) {
		if (original == null)
			return "";
		String GENEPATTERN_PORT = "GENEPATTERN_PORT";
		String GENEPATTERN_URL = "GenePatternURL";
		String port = genepatternProps.getProperty(GENEPATTERN_PORT);
		original = StringUtils.replaceAll(original, GPConstants.LEFT_DELIMITER
				+ GPConstants.LSID + GPConstants.RIGHT_DELIMITER, model
				.getLsid());
		//		original = GenePatternAnalysisTask.replace(original,
		// GPConstants.LEFT_DELIMITER + GENEPATTERN_PORT +
		// GPConstants.RIGHT_DELIMITER, port);
		//		original = GenePatternAnalysisTask.replace(original,
		// GPConstants.LEFT_DELIMITER + GENEPATTERN_URL +
		// GPConstants.RIGHT_DELIMITER, System.getProperty("GenePatternURL"));
		try {
			// one of ours?
			if (!original.startsWith("http://localhost:" + port)
					&& !original.startsWith("http://127.0.0.1:" + port)) {
				return original;
			}
			URL org = new URL(original);
			String localhost = InetAddress.getLocalHost()
					.getCanonicalHostName();
			if (localhost.equals("localhost")) {
				// MacOS X can't resolve localhost when unplugged from network
				localhost = "127.0.0.1";
			}
			URL url = new URL("http://" + localhost + ":" + port
					+ org.getFile());
			return url.toString();
		} catch (UnknownHostException uhe) {
			return original;
		} catch (MalformedURLException mue) {
			return original;
		}
	}

}