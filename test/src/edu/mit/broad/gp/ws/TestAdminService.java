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

import org.genepattern.util.LSID;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.*;

import javax.activation.DataHandler;
import junit.framework.*;

/**
 *  Tests the Admin service
 *
 * @author    Joshua Gould
 */
public class TestAdminService extends TestWebService {

   public TestAdminService(String name) {
      super(name);
   }
   
    public void testGetServiceInfo()
          throws Exception {
      Map serviceInfo = adminProxy.getServiceInfo();
      assertTrue(serviceInfo.get("genepattern.version") != null);
      assertTrue(serviceInfo.get("lsid.authority") != null);
   }

   

// AdminService.getTask
   public void testGetNonExistantTask() throws Exception {
      deleteAllTasks();
      TaskInfo task = adminProxy.getTask("XXX");
      assertTrue(task == null);

   }


   public void testGetTaskByLSID() throws Exception {
      deleteAllTasks();
      installTask(PREPROCESS_ZERO);
      installTask(PREPROCESS_ONE);
      TaskInfo task = adminProxy.getTask(PREPROCESS_ONE.lsid);
      assertTrue(task.getTaskInfoAttributes().get(GPConstants.LSID).equals(PREPROCESS_ONE.lsid));
   }


   public void testGetClosestTaskByName1() throws Exception {
      deleteAllTasks();
      installTask(PREPROCESS_ZERO);
      installTask(PREPROCESS_ONE);
      TaskInfo task = adminProxy.getTask("PreprocessDataset");
      assertTrue(task.getTaskInfoAttributes().get(GPConstants.LSID).equals(PREPROCESS_ONE.lsid));
   }


   public void testGetClosestTaskByName2() throws Exception {
      deleteAllTasks();
      installTask(PREPROCESS_ZERO);
      installTask(PREPROCESS_ONE);
      installTask(FOREIGN_PREPROCESS);
      TaskInfo task = adminProxy.getTask("PreprocessDataset");
      assertTrue(task.getTaskInfoAttributes().get(GPConstants.LSID).equals(PREPROCESS_ONE.lsid));
   }


   public void testGetClosestTaskByBaseLSID() throws Exception {
      deleteAllTasks();
      installTask(PREPROCESS_ZERO);
      installTask(PREPROCESS_ONE);
      TaskInfo task = adminProxy.getTask(new LSID(PREPROCESS_ONE.lsid).toStringNoVersion());
      assertTrue(task.getTaskInfoAttributes().get(GPConstants.LSID).equals(PREPROCESS_ONE.lsid));
   }

   // AdminService.getLatestTasks
   public void testGetLatestTasks()
          throws Exception {
      deleteAllTasks();
      installTask(PREPROCESS_ZERO);
      installTask(PREPROCESS_ONE);
      TaskInfo[] tasks = adminProxy.getLatestTasks();
      assertTrue(tasks.length == 1);
      assertTrue(tasks[0].getTaskInfoAttributes().get(GPConstants.LSID).equals(PREPROCESS_ONE.lsid));
   }


   public void testGetLatestTasks2()
          throws Exception {
      deleteAllTasks();
      installTask(PREPROCESS_ZERO);
      installTask(PREPROCESS_ONE);
      installTask(FOREIGN_PREPROCESS);
      TaskInfo[] tasks = adminProxy.getLatestTasks();
      assertTrue(tasks.length == 2);
      assertTrue(containsTask(tasks, FOREIGN_PREPROCESS.lsid));
      assertTrue(containsTask(tasks, PREPROCESS_ONE.lsid));
   }
   
    public void testGetLatestTasks3()
          throws Exception {
      deleteAllTasks();
      installTask(PREPROCESS_ZERO);
      installTask(PREPROCESS_ONE);
      installTask(FOREIGN_PREPROCESS);
      String localLSID = taskIntegratorProxy.cloneTask(FOREIGN_PREPROCESS.lsid, FOREIGN_PREPROCESS.name);
      TaskInfo[] tasks = adminProxy.getLatestTasks();
      assertTrue(tasks.length == 3);
      assertTrue(containsTask(tasks, FOREIGN_PREPROCESS.lsid));
      assertTrue(containsTask(tasks, PREPROCESS_ONE.lsid));
      assertTrue(containsTask(tasks, localLSID));
   }
   
  


   // AdminService.getAllTasks
   public void testGetAllTasks()
          throws Exception {
      deleteAllTasks();
      installTask(PREPROCESS_ZERO);
      installTask(PREPROCESS_ONE);
      TaskInfo[] tasks = adminProxy.getAllTasks();
      assertTrue(tasks.length == 2);
   }


   public void testGetLatestTasksByName1()
                       throws Exception {
      deleteAllTasks();
      installTask(PREPROCESS_ZERO);
      installTask(PREPROCESS_ONE);
      TaskInfo[] tasks = adminProxy.getLatestTasksByName();
      assertTrue(tasks.length==1);
      assertTrue(containsTask(tasks, PREPROCESS_ONE.lsid));
   }
   
   public void testGetLatestTasksByName2()
                       throws  Exception {
      deleteAllTasks();
      installTask(PREPROCESS_ZERO);
      installTask(PREPROCESS_ONE);
      installTask(FOREIGN_PREPROCESS);
      TaskInfo[] tasks = adminProxy.getLatestTasksByName();
      assertTrue(tasks.length==1);
      assertTrue(containsTask(tasks, PREPROCESS_ONE.lsid));
   }
   
   public void testGetLatestTasksByName3()
                       throws  Exception {
      deleteAllTasks();
      installTask(PREPROCESS_ZERO);
      installTask(PREPROCESS_ONE);
      installTask(FOREIGN_PREPROCESS);
      String localLSID = taskIntegratorProxy.cloneTask(FOREIGN_PREPROCESS.lsid, FOREIGN_PREPROCESS.name);
      TaskInfo[] tasks = adminProxy.getLatestTasksByName();
      assertTrue(tasks.length==1);
      assertTrue(containsTask(tasks, localLSID));
   }
	
   

   public void testGetServerLog() throws Exception{
      DataHandler h = adminProxy.getServerLog();
      File f = new File(h.getName());
      try {
         assertTrue(f.length() > 0);
      } finally {
         f.delete();
      }
   }

   public void testGetGenePatternLog() throws Exception{
      DataHandler h = adminProxy.getGenePatternLog();
      File f = new File(h.getName());
      try {
         assertTrue(f.length() > 0);
      } finally {
         f.delete();
      }
   }

   public void testGetLSIDToVersionsMap() throws Exception {
      deleteAllTasks();
      installTask(PREPROCESS_ZERO);
      installTask(PREPROCESS_ONE);
      installTask(FOREIGN_PREPROCESS);
      Map map = adminProxy.getLSIDToVersionsMap();
      List versions = (List) map.get(new LSID(PREPROCESS_ZERO.lsid).toStringNoVersion());
      assertTrue(map.size()==2);
      assertTrue(versions.size()==2);
      assertTrue(versions.contains(new LSID(PREPROCESS_ZERO.lsid).getVersion()));
      assertTrue(versions.contains(new LSID(PREPROCESS_ONE.lsid).getVersion()));
      
   }
                            
   protected boolean containsTask(TaskInfo[] tasks, String lsid) {
      for(int i = 0; i < tasks.length; i++) {
         if(tasks[i].getTaskInfoAttributes().get(GPConstants.LSID).equals(lsid)) {
            return true;
         }
      }
      return false;
   }
}
