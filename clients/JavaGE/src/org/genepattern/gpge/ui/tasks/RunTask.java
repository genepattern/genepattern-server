/*
  The Broad Institute
  SOFTWARE COPYRIGHT NOTICE AGREEMENT
  This software and its documentation are copyright (2003-2006) by the
  Broad Institute/Massachusetts Institute of Technology. All rights are
  reserved.

  This software is supplied without any warranty or guaranteed support
  whatsoever. Neither the Broad Institute nor MIT can be responsible for its
  use, misuse, or functionality.
*/


package org.genepattern.gpge.ui.tasks;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.*;
import org.genepattern.gpge.ui.tasks.*;
import org.genepattern.gpge.GenePattern;

/**
 * Runs tasks for the GPGE
 * 
 * @author Joshua Gould
 */
public class RunTask {
	AnalysisService analysisService;

	ParameterInfo[] formalParams;

	TaskInfo taskInfo;

	ParameterInfo[] actualParams;

	String username;
        
        String password;

	public RunTask(AnalysisService analysisService,
			ParameterInfo[] actualParams, String username, String password) {
		this.analysisService = analysisService;
		this.taskInfo = analysisService.getTaskInfo();
		this.actualParams = actualParams;
		this.formalParams = taskInfo.getParameterInfoArray();
		this.username = username;
                this.password = password;
	}

	public void exec() {
		final List directoryInputs = new ArrayList();
		List missingParams = new ArrayList();
		final Map paramName2ActualParam = new HashMap();

		for (int i = 0, length = actualParams != null ? actualParams.length : 0; i < length; i++) {
			paramName2ActualParam.put(actualParams[i].getName(),
					actualParams[i]);
		}
		for (int i = 0, length = formalParams != null ? formalParams.length : 0; i < length; i++) {
			ParameterInfo actualParam = (ParameterInfo) paramName2ActualParam
					.get(formalParams[i].getName());
			String actualValue = actualParam != null ? actualParam.getValue()
					: null;
			
			if (actualValue == null) {
				java.util.HashMap attrs = formalParams[i].getAttributes();
				String sOptional = (String) attrs
						.get(GPConstants.PARAM_INFO_OPTIONAL[0]);
				boolean optional = (sOptional != null && sOptional.length() > 0);
				if (!optional) {
					missingParams.add(formalParams[i].getName());
				}
				continue;
			}

			if (actualParam.isInputFile()) {
				String fileOrUrl = actualValue;
				URL url = null;
				try {
					url = new URL(fileOrUrl);
				} catch (MalformedURLException mfe) {
				}
				if (url != null && "file".equals(url.getProtocol())) {
					java.io.File f = new java.io.File(url.getFile());
					try {
						fileOrUrl = f.getCanonicalPath();
					} catch (IOException e1) {
                  e1.printStackTrace();
						GenePattern.showErrorDialog("An error occurred while running " + analysisService.getTaskInfo().getName());
                  return;
					}
				}
				java.io.File file = new java.io.File(fileOrUrl);
				if (file.exists() && file.isDirectory()) {
					directoryInputs.add(actualParam);
				}
			}
		}

		if (missingParams.size() > 0) {
			String message = "Missing required fields: ";
			for (int j = 0, size = missingParams.size(); j < size; j++) {
				message += "\n" + AnalysisServiceDisplay.getDisplayString((String) missingParams.get(j));
			}
         GenePattern.showErrorDialog(message);
         return;
		}
		if (directoryInputs.size() > 1) {
			String message = "Only one input field can be a folder. The following input fields are folders: ";
			for (int j = 0, size = directoryInputs.size(); j < size; j++) {
				ParameterInfo param = (ParameterInfo) directoryInputs.get(j);
				message += "\n" + param.getName();
			}
			GenePattern.showErrorDialog(message);
         return;
		}

		Map tia = taskInfo.getTaskInfoAttributes();

		// if(RunPipeline.isPipeline(taskInfo)) {
		//  new RunPipeline(analysisService, directoryInputs,
		// paramName2ActualParam, username);
		// return;
		// }
		String server = analysisService.getServer();
		
		if (directoryInputs.size() > 0) {
			ParameterInfo directoryParam = (ParameterInfo) directoryInputs
					.get(0);
			File dir = new File(directoryParam.getValue());
			File[] files = dir.listFiles(new java.io.FileFilter() {
				public boolean accept(File f) {
					return !f.isDirectory() && !f.getName().startsWith(".")
							&& !f.getName().endsWith("~");
				}
			});

			for (int j = 0, length = files.length; j < length; j++) {
            try {
               directoryParam.getAttributes().put(
                  GPConstants.PARAM_INFO_CLIENT_FILENAME[0],
                  files[j].getCanonicalPath());
               directoryParam.setValue(files[j].getCanonicalPath());
            } catch(IOException ioe) {
               ioe.printStackTrace();
               GenePattern.showErrorDialog("An error occurred while running " + analysisService.getTaskInfo().getName());
               return;
            }
            try {
               AnalysisWebServiceProxy serviceProxy = new AnalysisWebServiceProxy(server, username, password);
               
               if (TaskLauncher.isVisualizer(analysisService)) {
                  TaskLauncher.submitVisualizer(analysisService,
                        copyParameterInfo(actualParams), username, password, serviceProxy);
               } else {
                  TaskLauncher.submitAndWaitUntilCompletionInNewThread(
                        copyParameterInfo(actualParams), serviceProxy, analysisService);
               }
            } catch (WebServiceException wse) {
               wse.printStackTrace();
               if(!GenePattern.disconnectedFromServer(wse, server)) {
                  GenePattern.showErrorDialog("An error occurred while running " +  analysisService.getTaskInfo().getName()); 
               }
               return;
            }
			}
		} else {
         try {
            AnalysisWebServiceProxy serviceProxy = new AnalysisWebServiceProxy(server, username, password);
            if (TaskLauncher.isVisualizer(analysisService)) {
               TaskLauncher.submitVisualizer(analysisService,
                     actualParams, username, password, serviceProxy);
            } else {
               TaskLauncher.submitAndWaitUntilCompletionInNewThread(
                     actualParams, serviceProxy, analysisService);
            }
         } catch (WebServiceException wse) {
            if(!GenePattern.disconnectedFromServer(wse, server)) {
               GenePattern.showErrorDialog("An error occurred while running " +  analysisService.getTaskInfo().getName()); 
            }
            return;
         }
			
		}
	}
   
   private static ParameterInfo[] copyParameterInfo(ParameterInfo[] p) {
      ParameterInfo[] c = new ParameterInfo[p.length];
      for(int i = 0; i < p.length; i++) {
         c[i] = org.genepattern.gpge.ui.maindisplay.GPGE.copyParameterInfo(p[i]);  
      }
      return c;
   }

}