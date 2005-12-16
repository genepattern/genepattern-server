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

import java.util.List;
import java.util.Map;
import org.genepattern.data.pipeline.*;
import org.genepattern.util.LSID;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.*;
import org.genepattern.client.*;
import org.genepattern.server.*;
import junit.framework.*;

/**
 *  Tests the functionality of the AdminService and TaskIntegrator web services
 *
 * @author    Joshua Gould
 */
public abstract class TestWebService extends TestCase {
   protected AdminProxy adminProxy;
   protected TaskIntegratorProxy taskIntegratorProxy;
   protected GPServer gpServer;
   
   protected final static Task PREPROCESS_ZERO = new Task("PreprocessDataset", "urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00020:0");
   protected final static String[] PREPROCESS_ZERO_FILE_NAMES = {"broad-cg.jar", "jakarta-oro-2.0.8.jar", "Jama-1.0.1.jar", "Preprocess.jar", "PreprocessDataset.pdf", "trove.jar"};

   protected final static Task PREPROCESS_ONE = new Task("PreprocessDataset", "urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00020:1");

   protected final static File BASE_FILE_DIR = new File("files");

   protected final static Task FOREIGN_PREPROCESS = new Task(new File(BASE_FILE_DIR, "PreprocessDatasetForeign.zip"));

   protected String userName = "GenePattern";
   protected String server = "http://127.0.0.1:8080";


   public TestWebService(String name) {
      super(name);
   }


   public TestWebService() {
      super();
   }

   public static String getStackTrace(Exception e) {
      java.io.StringWriter sw = new java.io.StringWriter();
      java.io.PrintWriter pw = new java.io.PrintWriter(sw);
      e.printStackTrace(pw);
      pw.close();
      return sw.toString();
   }

   protected String taskArrayToString(TaskInfo[] tasks) {
      StringBuffer buf = new StringBuffer();
      if(tasks != null) {
         for(int i = 0; i < tasks.length; i++) {
            buf.append(tasks[i].getName());
            buf.append("\t");
            buf.append(tasks[i].getTaskInfoAttributes().get(GPConstants.LSID));
            buf.append("\n");
         }
      }
      return buf.toString();
   }
   
   protected void printTasks() throws Exception {
      TaskInfo[] tasks = adminProxy.getAllTasks();
      if(tasks != null) {
         for(int i = 0; i < tasks.length; i++) {
            System.out.println(tasks[i].getTaskInfoAttributes().get(GPConstants.LSID));
         }
      }
   }


   protected String installTask(Task task, int privacy) throws Exception {
      if(task.file == null) {
         return taskIntegratorProxy.importZipFromURL(task.getDownloadURL(), privacy);
      } else {
         return taskIntegratorProxy.importZip(task.file, privacy);
         
      }
   }


   protected String installTask(Task task) throws Exception {
      return installTask(task, GPConstants.ACCESS_PUBLIC);
   }


   protected void assertOnlyTask(Task task) throws Exception {
      TaskInfo[] tasks = adminProxy.getAllTasks();
      assertTrue(taskArrayToString(tasks), tasks.length==1);
      assertTrue(taskArrayToString(tasks), tasks[0].getTaskInfoAttributes().get(GPConstants.LSID).equals(task.lsid));
   }
   

   protected void deleteAllTasks()  {
      TaskInfo[] tasks = null;
      try {
         tasks = adminProxy.getAllTasks();
      } catch(Exception e) {
           
      }
      if(tasks != null) {
         for(int i = 0; i < tasks.length; i++) {
            try {
               taskIntegratorProxy.deleteTask((String)tasks[i].getTaskInfoAttributes().get(GPConstants.LSID));
            } catch(Exception e) {
               e.printStackTrace();
            }
         }
      }
      try {
         tasks = adminProxy.getAllTasks();
         assertTrue(taskArrayToString(tasks), tasks.length == 0);
      } catch(Exception e){}
      
   }


   protected void installDependentTasks(PipelineModel model) throws Exception {
     
      List tasks = model.getTasks();
      for(int i = 0; i < tasks.size(); i++) {
         JobSubmission js = (JobSubmission) tasks.get(i);
         TaskInfo task = adminProxy.getTask(js.getLSID());
         if(task == null) {
            installTask(new Task(js.getName(), js.getLSID()));
         }
      }
   }


   protected void setUp() throws Exception {
      adminProxy = new AdminProxy(server, userName);
      taskIntegratorProxy = new TaskIntegratorProxy(server, userName);
      gpServer = new GPServer(server, userName);
   }


   /**
    *  Description of the Class
    *
    * @author    Joshua Gould
    */
   public static class Task {
      String name, lsid;
      File file;
      boolean zipOfZips = false;
      private final static String BASE_FTP_URL = "ftp://ftp.broad.mit.edu/pub/genepattern/modules/";


      Task(File zipFile) {
         try {
            if(TaskUtil.isZipOfZips(zipFile)) {
               zipOfZips = true;
               TaskInfo[] tasks = TaskUtil.getTaskInfosFromZipOfZips(zipFile);
               Map props = tasks[0].getTaskInfoAttributes();
               name = tasks[0].getName();
               lsid = (String) props.get("LSID");
            } else {
               TaskInfo task = TaskUtil.getTaskInfoFromZip(zipFile);
               Map props = task.getTaskInfoAttributes();
               name = task.getName();
               lsid = (String) props.get("LSID");
            }


            this.file = zipFile;
         } catch(java.io.IOException ioe) {
            throw new RuntimeException(ioe);
         }
      }


      Task(String name, String lsid) {
         this.name = name;
         this.lsid = lsid;
      }


      private String getDownloadURL() {
         try {
            LSID _lsid = new LSID(lsid);

            return BASE_FTP_URL + name + "/broad.mit.edu%3Acancer.software.genepattern.module.analysis/" + _lsid.getVersion() + "/" + name + ".zip";
         } catch(Exception e) {
            e.printStackTrace();
         }
         return null;
      }
   }

}
