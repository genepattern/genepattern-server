package org.genepattern.server.webservice.server.dao;

import org.genepattern.util.GPConstants;
import org.genepattern.webservice.*;
import org.genepattern.webservice.SuiteInfo;
import org.genepattern.webservice.TaskInfo;

import junit.framework.TestCase;


public class AdminDataServiceTest extends ServiceTestBase {

    AdminDataService service = AdminDataService.getInstance();


    /*
     * Test method for 'org.genepattern.server.webservice.server.service.AdminDataService.getTaskId(String, String)'
     */
    public void testGetTaskId() throws Exception {
        int expectedTaskId = 46;
        
        TaskInfo task = service.getTask(expectedTaskId);
        assertNotNull(task);
        int taskId = service.getTaskId(task.getName(), task.getUserId());
        assertEquals(expectedTaskId, taskId);
        
 
    }


    /**
     * 
     */
    public void testGetAllTasks()  throws Exception {
        
        TaskInfo [] tasks = service.getAllTasks();
        assertTrue(tasks.length > 0);

    }

    /**
     * 
     */
    public void testGetAllTasksByUsername()  throws Exception {
        String username = "twomey";
        TaskInfo [] tasks = service.getAllTasksForUser(username);
        assertTrue(tasks.length > 0);
        for(int i=0; i<tasks.length; i++) {
            String userId = tasks[i].getUserId();

            assertTrue((userId != null && userId.equals(username)) ||  tasks[i].getAccessId() == GPConstants.ACCESS_PUBLIC);
        }

    }

    /**
     * 
     */
    public void testGetLatestTasksByName() throws Exception {
        String username = "twomey";
        TaskInfo [] tasks = service.getLatestTasksByName(username);
        assertTrue(tasks.length > 0);
        for(int i=0; i<tasks.length; i++) {
            String userId = tasks[i].getUserId();
            assertTrue((userId != null && userId.equals(username)) ||  tasks[i].getAccessId() == GPConstants.ACCESS_PUBLIC);
        }
    }

    /**
     * 
     */
    public void testGetLatestTasks() throws Exception {
        String username = "twomey";
        TaskInfo [] tasks = service.getLatestTasks(username);
        assertTrue(tasks.length > 0);
        for(int i=0; i<tasks.length; i++) {
            String userId = tasks[i].getUserId();
            assertTrue((userId != null && userId.equals(username)) ||  tasks[i].getAccessId() == GPConstants.ACCESS_PUBLIC);
        }
   }

    /**
     * @see AnalysisDAO#addNewTask(String,String,int,String,String,String)
     */
    public void testAddNewTask() throws Exception {

        TaskInfo task = service.getTask(1); // Get a task to copy
        TaskInfoAttributes tia = getTestTaskInfoAttributes();
        String taskName = "test";
        int newTaskId = service.addNewTask(taskName, task.getUserId(), task.getAccessId(), task.getDescription(), task
                .getParameterInfo(), tia.encode());

        TaskInfo newTask = service.getTask(newTaskId);
        assertNotNull(newTask);
        assertEquals(newTaskId, newTask.getID());
        assertEquals(taskName, newTask.getName());
        assertEquals(task.getUserId(), newTask.getUserId());
        assertEquals(task.getDescription(), newTask.getDescription());
        assertEquals(task.getParameterInfo(), newTask.getParameterInfo());

        for (Object key : tia.keySet()) {
            assertEquals(tia.get(key), newTask.getTaskInfoAttributes().get(key));
        }

    }

    /**
     * 
     */
    public void testUpdateTask() throws Exception {

        try {
            TaskInfo task = service.getTask(1);
            TaskInfoAttributes tia = new TaskInfoAttributes();

            service.updateTask(task.getID(), null, null, null, null, task.getAccessId());
            service.updateTask(task.getID(), task.getDescription(), task.getParameterInfo(), task
                    .giveTaskInfoAttributes().encode(), task.getUserId(), task.getAccessId());
        }
        catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());

        }

    }

    /**
     * 
     */
    public void testUpdateTask1() throws Exception {
        TaskInfo task = service.getTask(1);
        TaskInfoAttributes tia = getTestTaskInfoAttributes();

        service.updateTask(task.getID(), null, null, null, task.getAccessId());

        service.updateTask(task.getID(), task.getParameterInfo(), tia.encode(), task.getUserId(), task.getAccessId());
    }


    /**
     * 
     */
    public void testGetSuite() throws Exception  {
        String lsid = "urn:lsid:broad.mit.edu:genepatternsuites:1:1";
        SuiteInfo suite = service.getSuite(lsid);
        assertNotNull(suite);
        assertEquals(lsid, suite.getLsid());

    }

    /**
     * 
     */
    public void testGetLatestSuites() throws Exception {
        SuiteInfo [] suites = service.getLatestSuites();
        assertTrue(suites.length > 0);

    }

    /**
     * 
     */
    public void testGetLatestSuitesByUser() throws Exception {
        String user = "jlerner@broad.mit.edu";
        SuiteInfo [] suites = service.getLatestSuites(user);
        assertTrue(suites.length > 0);

    }

    /**
     * 
     */
    public void testGetAllSuitesByUser() throws Exception {
        String user = "jlerner@broad.mit.edu";
        SuiteInfo [] suites = service.getAllSuites(user);
        assertTrue(suites.length > 0);
    }

    /**
     * AdminHSQLservice#getSuiteMembership(String)
     */
    public void testGetSuiteMembershipByLsid() throws Exception {
        String taskLSID = "";
        SuiteInfo [] suites = service.getSuiteMembership(taskLSID);
        assertTrue(suites.length > 0);

    }
    


    /*
     * Test method for 'org.genepattern.server.webservice.server.service.AdminDataService.getAllSuites()'
     */

    public void testGetAllSuites() throws Exception {
        SuiteInfo [] suites = service.getLatestSuites();
        assertTrue(suites.length > 0);
    }


    
    /**
     * 
     * @throws Exception
     */
    public void testDeleteTask() throws Exception {

        int taskId = 1;
        TaskInfo task = service.getTask(taskId);
        assertNotNull(task);

        int count = service.getTasks("twomey").size();
        service.deleteTask(taskId);
        int postCount = service.getTasks("twomey").size();
        assertEquals(count - 1, postCount);
    }


}
