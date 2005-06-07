package edu.mit.broad.gp.ws;
import java.io.File;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.genepattern.webservice.*;
import org.genepattern.util.GPConstants;
import junit.framework.*;

/**
 *  Tests the functionality of the TaskIntegrator web service
 *
 * @author    Joshua Gould
 */
public class TestTaskIntegrator extends TestWebService {

   public TestTaskIntegrator(String name) {
      super(name);
   }

   // TaskIntegrator.deleteFiles
   public void testDeleteFiles(String lsid, String[] fileNames)
          throws Exception {
      deleteAllTasks();
      installTask(PREPROCESS_ZERO);
      taskIntegratorProxy.deleteFiles(PREPROCESS_ZERO.lsid, PREPROCESS_ZERO_FILE_NAMES);
      List installedNames = Arrays.asList(taskIntegratorProxy.getSupportFileNames(PREPROCESS_ZERO.lsid));
      for(int i = 0; i < PREPROCESS_ZERO_FILE_NAMES.length; i++) {
         assertTrue(!installedNames.contains(PREPROCESS_ZERO_FILE_NAMES[i]));
      }

   }
   
   public void testDeleteTask() throws Exception {
      deleteAllTasks();
      for(int i = 0; i < 10; i++) {
         installTask(PREPROCESS_ZERO);
      }
      
      TaskInfo[] tasks = adminProxy.getAllTasks();
      assertTrue(taskArrayToString(tasks), tasks.length==1);
   }
   


   public void testDeleteBadFiles()
          throws Exception {
      deleteAllTasks();
      installTask(PREPROCESS_ZERO);
      try {
         taskIntegratorProxy.deleteFiles(PREPROCESS_ZERO.lsid, new String[]{"XXX"});
         fail();
      } catch(Exception e) {
         System.out.println("testDeleteBadFiles:" + e.getMessage());// FIXME
      }
   }


   public void testInstallTask1() throws Exception {
      deleteAllTasks();
      File[] files = null;
      String description = "desc";
      ParameterInfo[] parameterInfoArray = null;
      String assignedLSID = taskIntegratorProxy.modifyTask(GPConstants.ACCESS_PUBLIC, "test", description, parameterInfoArray, createTaskAttributes(), files);
      TaskInfo task = adminProxy.getTask(assignedLSID);
      assertTrue(task != null);
      assertTrue(task.getTaskInfoAttributes().get("privacy").equals("public"));
   }



   //  modifyTask does not use previously defined attributes, is this correct ?
   public void testInstallTask2() throws Exception {
      deleteAllTasks();
      File[] files = null;
      ParameterInfo[] parameterInfoArray = null;
      String lsid1 = taskIntegratorProxy.modifyTask(GPConstants.ACCESS_PUBLIC, "test", "desc", parameterInfoArray, createTaskAttributes(), files);
      HashMap map = new HashMap();
      map.put("language", "Java");
      map.put("LSID", lsid1);
      String lsid2 = taskIntegratorProxy.modifyTask(GPConstants.ACCESS_PUBLIC, "test", "desc", parameterInfoArray, map, files);
      TaskInfo task = adminProxy.getTask(lsid2);
      assertTrue(task != null);
      assertTrue(task.getTaskInfoAttributes().get("language").equals("Java"));
      assertTrue(task.getTaskInfoAttributes().get("commandLine").equals("xxx"));
   }

   // getSupportFiles
   public void testGetSupportFiles() throws Exception {
      File destinationDirectory = new File("temp");
      try {
         installTask(PREPROCESS_ZERO);
         destinationDirectory.mkdir();
         taskIntegratorProxy.getSupportFiles(PREPROCESS_ZERO.lsid, PREPROCESS_ZERO_FILE_NAMES, destinationDirectory);
         for(int i = 0; i < PREPROCESS_ZERO_FILE_NAMES.length; i++) {
            File f = new File(destinationDirectory, PREPROCESS_ZERO_FILE_NAMES[i]);
            assertTrue(f.exists());
            assertTrue(f.length() > 0);
         }
      } finally {
         destinationDirectory.delete();
      }
   }


   public void testBadGetSupportFiles() throws Exception {
      installTask(PREPROCESS_ZERO);
      File destinationDirectory = new File("temp");
      destinationDirectory.mkdir();
      try {
         taskIntegratorProxy.getSupportFiles(PREPROCESS_ZERO.lsid, new String[]{"XX"}, destinationDirectory);
         fail();
      } catch(Exception e) {
         return;
      } finally {
         destinationDirectory.delete();
      }
   }

   // TaskIntegrator.importZipFromURL
   public void testImportZipFromBadURL() {
      try {
         deleteAllTasks();
         taskIntegratorProxy.importZipFromURL("http://broad.mit.edu", GPConstants.ACCESS_PUBLIC);
         fail("An exception should have been thrown");
      } catch(Exception e) {// FIXME
         System.out.println("testImportZipFromBadURL:" + e.getMessage());
      }
   }


   // TaskIntegrator.importZip
   public void testImportZipFromFile()
          throws Exception {
      deleteAllTasks();
      String lsid = installTask(FOREIGN_PREPROCESS);
      TaskInfo task = adminProxy.getTask(FOREIGN_PREPROCESS.lsid);
      assertTrue(task != null);
      assertTrue("userid is not " + userName + ": " + task.getTaskInfoAttributes().get("userid").toString(), task.getTaskInfoAttributes().get("userid").equals(userName));
      assertTrue(task.getTaskInfoAttributes().get(GPConstants.LSID).toString(), task.getTaskInfoAttributes().get(GPConstants.LSID).equals(FOREIGN_PREPROCESS.lsid));
      assertTrue(lsid.equals(FOREIGN_PREPROCESS.lsid));
   }

   // TaskIntegrator.importZipFromURL
   public void testImportZipFromURL() throws Exception {
      deleteAllTasks();
      installTask(PREPROCESS_ZERO, GPConstants.ACCESS_PRIVATE);
      assertOnlyTask(PREPROCESS_ZERO);
      
      TaskInfo task = adminProxy.getTask(PREPROCESS_ZERO.lsid);
      assertTrue(task!=null);
      List installedNames = Arrays.asList(taskIntegratorProxy.getSupportFileNames(PREPROCESS_ZERO.lsid));

      for(int i = 0; i < PREPROCESS_ZERO_FILE_NAMES.length; i++) {
         assertTrue("missing " + PREPROCESS_ZERO_FILE_NAMES[i], installedNames.contains(PREPROCESS_ZERO_FILE_NAMES[i]));
      }
      assertTrue(task.getTaskInfoAttributes().get("privacy").equals("private"));
      assertTrue(task.getTaskInfoAttributes().get("userid").equals(userName));
      assertTrue(task.getTaskInfoAttributes().get(GPConstants.LSID).equals(PREPROCESS_ZERO.lsid));
   }


   // TaskIntegrator.exportToZip
   public void testExportToZip()
          throws Exception {
      deleteAllTasks();
      installTask(PREPROCESS_ZERO);
      File temp = new File("temp.zip");
      taskIntegratorProxy.exportToZip(PREPROCESS_ZERO.lsid, temp);
      assertTrue(temp.length() > 0);
      temp.delete();
   }


   public void testExportToZipNonExistantTask() throws Exception {
      deleteAllTasks();
      installTask(PREPROCESS_ZERO);
      File temp = new File("temp.zip");
      try {
         taskIntegratorProxy.exportToZip("xxxx", temp);
         fail();
      } catch(Exception e) {// FIXME
         System.out.println("testExportToZipNonExistantTask:" + e.getMessage());
      } finally {
         temp.delete();
      }
   }
   
   public void testImportDelete() throws Exception {
      deleteAllTasks();
      installTask(PREPROCESS_ONE);
      assertOnlyTask(PREPROCESS_ONE);
      
      deleteAllTasks();
      installTask(PREPROCESS_ONE);
      installTask(PREPROCESS_ONE);
      assertOnlyTask(PREPROCESS_ONE);
      
   }
   
   public void testImportDelete2() throws Exception {
      deleteAllTasks();
      installTask(FOREIGN_PREPROCESS);
      assertOnlyTask(FOREIGN_PREPROCESS);
      
      deleteAllTasks();
      installTask(FOREIGN_PREPROCESS);
      installTask(FOREIGN_PREPROCESS);
      assertOnlyTask(FOREIGN_PREPROCESS);
     
   }
   
   public void testImportDelete3() throws Exception {
      deleteAllTasks();
      installTask(FOREIGN_PREPROCESS, GPConstants.ACCESS_PRIVATE);
      assertOnlyTask(FOREIGN_PREPROCESS);
      taskIntegratorProxy.deleteTask(FOREIGN_PREPROCESS.name);
      assertTrue(adminProxy.getAllTasks().length==0);
   }


   private HashMap createTaskAttributes() {

      HashMap taskAttributes = new HashMap();
      taskAttributes.put("commandLine", "xxx");
      taskAttributes.put("name", "test");
      taskAttributes.put("privacy", "private");
      taskAttributes.put("os", "any");
      taskAttributes.put("cpuType", "any");
      taskAttributes.put("author", "xx");
      taskAttributes.put("userid", "xx");
      taskAttributes.put("taskType", "any");
      taskAttributes.put("language", "any");
      taskAttributes.put("serializedModel", "");
      taskAttributes.put("pipelineModel", "");
      taskAttributes.put("version", "");
      taskAttributes.put("JVMLevel", "");
      taskAttributes.put("quality", "development");
      return taskAttributes;
   }

}
