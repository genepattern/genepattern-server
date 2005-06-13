package org.genepattern.gpge.ui.code;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;

import org.genepattern.util.GPConstants;
import org.genepattern.gpge.GenePattern;
import org.genepattern.webservice.*;
import org.genepattern.gpge.ui.maindisplay.CenteredDialog;
import org.genepattern.gpge.ui.tasks.JobModel;
import org.genepattern.gpge.ui.tasks.AnalysisServiceManager;

/**
 *  Utility class for code generation
 *
 * @author    Joshua Gould
 */
public class Util {
   private Util() { }


   /**
    *  Generates code for the given <tt>AnalysisJob</tt> object
    *
    * @param  codeGenerator  the code generator
    * @param  job            the job
    * @param  language       the language
    */
   public static void viewCode(org.genepattern.codegenerator.TaskCodeGenerator codeGenerator, final AnalysisJob job, final String language) {
      JobInfo jobInfo = job.getJobInfo();
      List parameterInfoList = new ArrayList();
      ParameterInfo[] params = jobInfo.getParameterInfoArray();
      for(int i = 0; i < params.length; i++) {
         if(!params[i].isOutputFile()) {
            Object mode = params[i].getAttributes().get(ParameterInfo.MODE);
            if(mode != null && mode.equals(ParameterInfo.CACHED_INPUT_MODE)) {
               String name = JobModel.getJobResultFileName(job, i);
               int jobNumber = JobModel.getJobCreationJobNumber(job, i);
               try {
                  String url = job.getServer() + "/gp/retrieveResults.jsp?job=" + jobNumber + "&filename=" + java.net.URLEncoder.encode(name, "UTF-8");

                  parameterInfoList.add(new ParameterInfo(params[i].getName(), url, ""));
               } catch(java.io.UnsupportedEncodingException x) {
                  x.printStackTrace();
               }
            } else if(params[i].getAttributes().get("client_filename") != null) {
               String clientFile = (String) params[i].getAttributes().get("client_filename");
               parameterInfoList.add(new ParameterInfo(params[i].getName(), clientFile, ""));
            } else {
               parameterInfoList.add(params[i]);
            }

         }
      }

      AnalysisService svc = AnalysisServiceManager.getInstance().getAnalysisService(jobInfo.getTaskLSID());
      String code = null;
      if(svc != null) { // if task exists on server
         TaskInfo taskInfo = svc.getTaskInfo();
         try {
            String serializedModel = (String) taskInfo.getTaskInfoAttributes().get("serializedModel");
            if(serializedModel != null && serializedModel.length() > 0) {
               Map runtimePrompts = new HashMap();
               for(int i = 0; i < parameterInfoList.size(); i++) {
                  ParameterInfo p = (ParameterInfo) parameterInfoList.get(i);
                  if(!p.isOutputFile()) {
                     runtimePrompts.put(p.getName(), p);
                  }
               }

               org.genepattern.data.pipeline.PipelineModel model = org.genepattern.data.pipeline.PipelineModel.toPipelineModel((String) taskInfo.getTaskInfoAttributes().get("serializedModel"));
               List taskInfos = new ArrayList();
               List jobSubmissions = model.getTasks();
               for(int i = 0; i < jobSubmissions.size(); i++) {
                  org.genepattern.data.pipeline.JobSubmission js = (org.genepattern.data.pipeline.JobSubmission) jobSubmissions.get(i);
                  java.util.Arrays.fill(js.getRuntimePrompt(), false);
                  List p = js.getParameters();
                  for(int j = 0; j < p.size(); j++) {
                     ParameterInfo pi = (ParameterInfo) p.get(j);
                     if(pi.getAttributes().get("runTimePrompt") != null) {
                        String key = js.getName() + (i + 1) + "." + pi.getName();
                        ParameterInfo rt = (ParameterInfo) runtimePrompts.get(key);
                        p.set(j, rt);
                     }
                  }
                  model.setLsid((String) taskInfo.getTaskInfoAttributes().get(GPConstants.LSID));
                  model.setUserID(AnalysisServiceManager.getInstance().getUsername());
                  
                  AnalysisService pipelineStep =  AnalysisServiceManager.getInstance().getAnalysisService(js.getLSID());
                
                  if(pipelineStep==null) {
                     throw new IllegalArgumentException();
                     // missing task in pipeline
                  }
                  taskInfos.add(pipelineStep.getTaskInfo());
               }
               code = org.genepattern.codegenerator.AbstractPipelineCodeGenerator.getCode(model,
                     taskInfos,
                     AnalysisServiceManager.getInstance().getServer(),
                     language);

            }
         } catch(Exception e) {
            e.printStackTrace();
         }
      }


      if(code == null) {
         code = codeGenerator.generateTask(job, (ParameterInfo[]) parameterInfoList.toArray(new ParameterInfo[0]));
      }
      JDialog dialog = new CenteredDialog(GenePattern.getDialogParent());
      dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
      String title = language + " code for " + jobInfo.getTaskName();
      if(jobInfo.getJobNumber()!=-1) {
         title += ", job " + jobInfo.getJobNumber();
      }
      dialog.setTitle(title);
      JTextArea textArea = new JTextArea(code);
      textArea.setLineWrap(true);
      textArea.setEditable(false);
      JScrollPane sp = new JScrollPane(textArea);
      dialog.getContentPane().add(sp, BorderLayout.CENTER);
      Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
      dialog.setSize(screenSize.width / 2, screenSize.height / 2);
      dialog.show();
   }
}
