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


package edu.mit.broad.gp.ws;

import java.io.File;


import org.genepattern.data.pipeline.*;
import org.genepattern.webservice.*;
import org.genepattern.server.*;
import org.genepattern.server.genepattern.*;
import junit.framework.*;

/**
 *  Tests running tasks.
 *
 * @author    Joshua Gould
 */
public class TestRunTasks
       extends TestWebService {
   static String sep = File.separator;


   public TestRunTasks(String name) {
      super(name);
   }


   /**
    *  Runs a 1.3 pipeline with a prompt when run file param
    *
    * @exception  Exception  Description of the Exception
    */
   public void testPromptWhenRunFileParam()
          throws Exception {
      runTask(new File("files" + sep + "run-tasks" + sep + "params" + sep +
            "1.3" + sep +
            "pipeline_with_prompt_when_run_file_param.zip"),
            new Parameter[]{
            new Parameter("ConvertLineEndings1.input.filename",
            "http://www.cnn.com")
            });
   }


   /**
    *  Runs a 1.2.1 task that has no parameters. The task is an echo task that
    *  prints test to stdout.
    *
    * @exception  Exception  Description of the Exception
    */
   public void test1_2_1Task()
          throws Exception {
      runTask(new File("files" + sep + "run-tasks" + sep + "params" + sep +
            "1.2.1" + sep + "1.2.1_task.zip"), null);
   }


   /**
    *  Runs a 1.3 pipeline with a prompt when run param that is not a file
    *
    * @exception  Exception  Description of the Exception
    */
   public void testPromptWhenRunParam()
          throws Exception {
      JobResult r = runTask(new File("files" + sep + "run-tasks" + sep +
            "params" + sep + "1.3" + sep +
            "pipeline_with_prompt_when_run_param.zip"),
            new Parameter[]{
            new Parameter("ConvertLineEndings1.output.file", "out.txt")
            });
      assertTrue("" + r.getJobNumber(),
            java.util.Arrays.asList(r.getOutputFileNames()).contains(
            "out.txt"));
   }
   
   /** 
   * Runs a pipeline that has the task ConvertLineEndings. The pipeline should fail because ConvertLineEndings is not installed on the server.
   */
   public void testPipelineWithMissingTasks() throws Exception {
    JobResult r = runTask(new File("files" + sep + "run-tasks" + sep +
            "params" + sep + "1.3" + sep +
            "pipeline_with_missing_task.zip"),
           null, false, false);
           assertTrue("" + r.getJobNumber(), r.hasStandardError()); 
    }


   private JobResult runTask(File file, Parameter[] params) 
          throws Exception {
           return runTask(file, params, true, true);  
   }
             
   private JobResult runTask(File file, Parameter[] params, boolean installDependentTasks, boolean failIfStandardErr)
          throws Exception {
      deleteAllTasks();
      if(!file.exists()) {
         fail("File " + file.getCanonicalPath() + " not found.");
      }
      Task task = new Task(file);
      if(!task.zipOfZips) {
         TaskInfo taskInfo = TaskUtil.getTaskInfoFromZip(file);
         String serializedModel = (String) taskInfo.getTaskInfoAttributes().get(
               GenePatternAnalysisTask.SERIALIZED_MODEL);
         if(installDependentTasks && serializedModel != null && serializedModel.length() > 0) {
            PipelineModel model = PipelineModel.toPipelineModel(serializedModel);
            installDependentTasks(model);
         }
      }
      String lsid = installTask(task);
      JobResult jr = gpServer.runAnalysis(lsid, params);
      assertNotNull(jr);
      if(failIfStandardErr) {
         assertTrue("" + jr.getJobNumber(), !jr.hasStandardError());
      }
      return jr;
   }
}
