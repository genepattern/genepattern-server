package org.genepattern.server.webapp.jsf;

import java.util.*;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;

import java.io.File;

public class JobHelper {

	public static List<File> getOutputFiles(JobInfo jobInfo) {

		List<File> outputFiles = new ArrayList<File>();
		ParameterInfo[] parameterInfoArray = jobInfo.getParameterInfoArray();
		if (parameterInfoArray != null) {
			String dir = System.getProperty("jobs", "./temp") + "/" + jobInfo.getJobNumber();
			for (int i = 0; i < parameterInfoArray.length; i++) {
				if (parameterInfoArray[i].isOutputFile()) {
					String fn = parameterInfoArray[i].getName();
					outputFiles.add(new File(dir, fn));
					// get modules for output file
				}
			}
		}

		return outputFiles;
	}

}
