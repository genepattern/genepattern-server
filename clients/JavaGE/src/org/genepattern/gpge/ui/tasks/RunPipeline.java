package org.genepattern.gpge.ui.tasks;
import java.util.*;
import java.io.*;
import java.net.*;

import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.*;
import org.genepattern.gpge.ui.tasks.*;

/**
 *  Runs a pipeline
 *
 * @author    Joshua Gould
 */
class RunPipeline {
   AnalysisService analysisService;
   List directoryInputs;
   String username;
   Map paramName2ActualParamMap;
   /**
   *
   * @param the analysis service to run
   * @param directoryInputs A list of ParameterInfo objects that contains directories as input
   */
   public RunPipeline(AnalysisService analysisService, List directoryInputs, Map paramName2ActualParamMap, String username) {
      this.analysisService = analysisService;
      if(directoryInputs==null) {
         directoryInputs = new ArrayList();  
      }
      this.directoryInputs = directoryInputs;
      this.paramName2ActualParamMap = paramName2ActualParamMap;
      this.username = username;
   }


   public void exec() {
         new Thread() {
            public void run() {
               final AnalysisWebServiceProxy serviceProxy = new AnalysisWebServiceProxy(analysisService.getURL(), username, "");
               try {
                  PipelineModel model = null;
                  try {
                     model = PipelineModel.toPipelineModel((String)(analysisService.getTaskInfo().getTaskInfoAttributes().get(GPConstants.LSID)));
                  } catch(Throwable x) {
                     throw new RunTaskException(x);
                  }

                  final List tasks = model.getTasks();

                  AnalysisJob[] results = new AnalysisJob[tasks.size()];
                  int _directoryTaskIndex = -1;// index of task that
                  // has a directory as
                  // input
                  int _directoryParamIndex = -1;
                  boolean taskIndexFound = false;
                  File[] _files = null;
                  if(directoryInputs.size() > 0) {
                     ParameterInfo directoryParam = (ParameterInfo) directoryInputs.get(0);
                     for(int j = 0, size = tasks.size(); j < size
                            && !taskIndexFound; j++) {
                        JobSubmission js = (JobSubmission) tasks.get(j);

                        List formalParams = js.getParameters();
                        for(int p = 0, numParams = formalParams.size(); p < numParams
                               && !taskIndexFound; p++) {
                           ParameterInfo formalParam = (ParameterInfo) formalParams.get(p);
                           String name = getPipelineParameterName(js.getName(), j, formalParam);
                           if(name.equals(directoryParam.getName())) {
                              _directoryTaskIndex = j;
                              _directoryParamIndex = p;
                              taskIndexFound = true;

                           }
                        }

                     }
                     File dir = new File(directoryParam.getValue());
                     _files = dir.listFiles(
                        new java.io.FileFilter() {
                           public boolean accept(File f) {
                              return !f.isDirectory()
                                     && !f.getName().startsWith(".")
                                     && !f.getName().endsWith("~");
                           }
                        });
                  }

                  final int directoryTaskIndex = _directoryTaskIndex == -1 ? tasks.size() : _directoryTaskIndex;// index of task that has a directory as input
                  final int directoryParamIndex = _directoryParamIndex;
                  final File[] files = _files;

                  for(int i = 0; i < directoryTaskIndex; i++) {
                     String taskName = ((JobSubmission) tasks.get(i))
                           .getName();
                     AnalysisService svc2 = AnalysisServiceManager.getInstance(analysisService.getName(), username).getAnalysisService(taskName);
                     TaskInfo tinfo2 = svc2.getTaskInfo();
                     ParameterInfo[] actualParams = getPipelineParameters(
                           tasks, i, paramName2ActualParamMap, results,
                           taskName);
                     try {
                        if(TaskLauncher.isVisualizer(svc2)) {
                           TaskLauncher.submitVisualizer(svc2, actualParams, username);
                        } else {
                           results[i] = TaskLauncher.submitAndWaitUntilCompletion(
                                 actualParams, serviceProxy, svc2);
                        }
                     } catch(Exception e) {
                        throw new RunTaskException(e);
                     }
                  }

                  for(int _fileNum = 0, numFiles = files != null ? files.length
                         : 0; _fileNum < numFiles; _fileNum++) {// run all tasks repeatedly that come after the task that takes a folder as input
                     final AnalysisJob[] pipelineResults = (AnalysisJob[]) results.clone();
                     final int fileNum = _fileNum;
                        new Thread() {
                           public void run() {
                              for(int j = directoryTaskIndex; j < tasks.size(); j++) {
                                 String taskName = ((JobSubmission) tasks.get(j)).getName();
                                 AnalysisService svc2 = AnalysisServiceManager.getInstance(analysisService.getName(), username).getAnalysisService(taskName);
                                 TaskInfo tinfo2 = svc2.getTaskInfo();

                                 ParameterInfo[] actualParams = getPipelineParameters(
                                       tasks, j, paramName2ActualParamMap,
                                       pipelineResults, taskName);
                                 if(j == directoryTaskIndex) {
                                    actualParams[directoryParamIndex]
                                          .setAsInputFile();
                                    actualParams[directoryParamIndex]
                                          .setValue(files[fileNum]
                                          .getPath());
                                 }
                                 try {
                                    if(TaskLauncher.isVisualizer(svc2)) {
                                       TaskLauncher.submitVisualizer(svc2,
                                             actualParams, username);
                                    } else {
                                       pipelineResults[j] = TaskLauncher.submitAndWaitUntilCompletion(
                                             actualParams, serviceProxy, svc2);
                                    }
                                 } catch(Exception e) {
                                    throw new RunTaskException(e);
                                 }

                              }
                           }
                        }.start();
                  }

               } catch(Exception e1) {
                  throw new RunTaskException(e1);
               }

            }
         }.start();
   }


   public static boolean isPipeline(TaskInfo tinfo) {
      Map tia = tinfo.getTaskInfoAttributes();
      String serializedModel = (String) tia.get(GPConstants.SERIALIZED_MODEL);
      return ((serializedModel != null) && (serializedModel.trim().length() > 0));
   }


   /**
    * @param  taskName    The task name.
    * @param  taskNumber  The index of the task in the pipeline, starting from 0
    * @param  p           The parameter
    * @return             The pipeline parameter name
    */
   String getPipelineParameterName(String taskName, int taskNumber,
         ParameterInfo p) {
      return taskName + (taskNumber + 1) + "." + p.getName();
   }


   ParameterInfo[] getPipelineParameters(List tasks, int j,
         Map paramName2ActualParamMap, AnalysisJob[] results, String taskName) {
      JobSubmission js = (JobSubmission) tasks.get(j);
      List formalParams = js.getParameters();

      ParameterInfo[] actualParams = new ParameterInfo[formalParams.size()];

      for(int k = 0, length = formalParams.size(); k < length; k++) {
         actualParams[k] = new ParameterInfo();
         ParameterInfo formalParam = (ParameterInfo) formalParams.get(k);
         actualParams[k].setValue(formalParam.getValue());
         actualParams[k].setName(formalParam.getName());

         if(formalParam.isInputFile()) {
            actualParams[k].setAsInputFile();
         }
         Map formalAttributes = formalParam.getAttributes();
         String key = getPipelineParameterName(taskName, j, formalParam);
         ParameterInfo actualParam = (ParameterInfo) paramName2ActualParamMap.get(key);
         String runtimePromptValue = actualParam!=null?actualParam.getValue():null;
         
         if(runtimePromptValue != null) {

            String fileOrUrl = runtimePromptValue;
            URL url = null;
            try {
               url = new URL(fileOrUrl);

            } catch(MalformedURLException mfe) {
            }
            if(url != null && "file".equals(url.getProtocol())) {
               java.io.File f = new java.io.File(url.getFile());
               try {
                  fileOrUrl = f.getCanonicalPath();

               } catch(IOException e1) {
                  throw new RunTaskException(e1);
               }
            }
            actualParams[k].setValue(fileOrUrl);
            java.io.File file = new java.io.File(fileOrUrl);

            if(file.exists()) {
               actualParams[k].setAsInputFile();
            }
         }
         if(formalAttributes != null) {
            String taskNumber = (String) formalAttributes.get(PipelineModel.INHERIT_TASKNAME);
            if(taskNumber != null) {
               String outputFileNumber = (String) formalAttributes.get(PipelineModel.INHERIT_FILENAME);
               int taskNumberInt = Integer.parseInt(taskNumber.trim());
               String url = getOutputFileURL(results[taskNumberInt],
                     outputFileNumber);
               actualParams[k].setValue(url);
               actualParams[k].getAttributes().remove("MODE");
               actualParams[k].getAttributes().remove("TYPE");
            }
         }
      }
      return actualParams;
   }


   String getOutputFileURL(AnalysisJob job, String fileNumber) {
      String fileName = fileNumber;
      try {
         int index = Integer.parseInt(fileNumber) - 1;
         // find the ith output file
         ParameterInfo[] params = job.getJobInfo().getParameterInfoArray();
         int count = 0;
         for(int j = 0, length = params.length; j < length; j++) {
            if(params[j].isOutputFile()) {
               if(index == count) {
                  fileName = params[j].getValue();
                  int slashIndex = fileName.lastIndexOf('/');
                  if(slashIndex == -1) {
                     slashIndex = fileName.lastIndexOf('\\');
                  }
                  if(slashIndex != -1) {
                     fileName = fileName.substring(slashIndex + 1,
                           fileName.length());
                  }
                  break;
               }
               count++;
            }
         }

      } catch(NumberFormatException nfe) {
      }
      String server = job.getSiteName();
      return "http://" + server + "/gp/retrieveResults.jsp?job="
             + job.getJobInfo().getJobNumber() + "&filename=" + fileName;
   }
}
