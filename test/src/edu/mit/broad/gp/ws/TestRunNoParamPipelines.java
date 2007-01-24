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
import java.io.FileFilter;

import java.util.ArrayList;
import java.util.List;
import org.genepattern.data.pipeline.*;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.*;
import org.genepattern.server.*;
import junit.framework.*;

/**
 *  Tests running pipelines with no parameters. Will run all pipelines in a
 *  subdirectory of files/run-tasks/no-params
 *
 * @author    Joshua Gould
 */
public class TestRunNoParamPipelines extends TestCase {

   static File noParameterDir = new File("files" + File.separator + "run-tasks" + File.separator + "no-params");


   public TestRunNoParamPipelines(String name) {
      super(name);
   }


   public static Test suite() {
      TestSuite suite = new TestSuite("Run Pipelines");
      List files = listFiles(noParameterDir, new ZipFileFilter());
      for(int i = 0; i < files.size(); i++) {
         File zipFile = (File) files.get(i);
         suite.addTest(new RunPipelineNoParam("testRunPipeline", zipFile));
      }
      return suite;
   }


   /**
    *  Recursively lists the files in a directory
    *
    * @param  dir     Description of the Parameter
    * @param  filter  Description of the Parameter
    * @return         Description of the Return Value
    */
   public static List listFiles(File dir, FileFilter filter) {
      List result = new ArrayList();
      File[] files = dir.listFiles(filter);
      for(int i = 0; i < files.length; i++) {
         File file = files[i];
         if(!file.isFile()) {
            result.addAll(listFiles(file, filter));
         } else {
            result.add(file);
         }
      }
      return result;
   }


   /**
    *  Description of the Class
    *
    * @author    Joshua Gould
    */
   public static class RunPipelineNoParam extends TestWebService {
      File zipFile;


      public RunPipelineNoParam(String name, File f) {
         super(name);
         this.zipFile = f;
      }



      public void testRunPipeline() {
         try {

            Task task = new Task(zipFile);

            if(!task.zipOfZips) {

               TaskInfo taskInfo = TaskUtil.getTaskInfoFromZip(zipFile);

               String serializedModel = (String) taskInfo.getTaskInfoAttributes().get(GPConstants.SERIALIZED_MODEL);

               if(serializedModel != null && serializedModel.length() > 0) {
                  PipelineModel model = PipelineModel.toPipelineModel(serializedModel);
                  installDependentTasks(model);
               }
            }

            String lsid = installTask(task);
            JobResult jr = gpServer.runAnalysis(lsid, null);
            assertNotNull(zipFile.getPath(), jr);
            assertTrue(zipFile.getPath(), !jr.hasStandardError());
         } catch(Exception e) {
            fail(zipFile.getName() + " produced an error\n" + getStackTrace(e));
         }
      }


      protected void setUp() throws Exception {
         super.setUp();
         deleteAllTasks();

      }

   }


   static class ZipFileFilter implements FileFilter {
      public boolean accept(File f) {
         return f.isDirectory() || f.getName().toLowerCase().endsWith(".zip");
      }
   }

}
