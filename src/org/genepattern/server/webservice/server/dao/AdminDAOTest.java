package org.genepattern.server.webservice.server.dao;

import java.util.Arrays;
import java.util.List;

import org.genepattern.util.GPConstants;
import org.genepattern.webservice.*;

import junit.framework.TestCase;


public class AdminDAOTest extends DAOTestCase {

    private AdminDAO dao;


    @Override
    protected void setUp() throws Exception {
        super.setUp();
        dao = new AdminDAO();
    }


    /*
     * Test method for 'org.genepattern.server.webservice.server.service.AdminDataService.getTaskId(String, String)'
     */
    public void testGetTask() throws Exception {
        int expectedTaskId = 46;
        
        TaskInfo task = dao.getTask(expectedTaskId);
        assertNotNull(task);
        assertEquals(expectedTaskId, task.getID());
        
 
    }


    /**
     * 
     */
    public void testGetAllTasks()  throws Exception {
        
        TaskInfo [] tasks = dao.getAllTasks();
        assertTrue(tasks.length > 0);

    }

    /**
     * 
     */
    public void testGetAllTasksByUsername()  throws Exception {
        String username = "twomey";
        TaskInfo [] tasks = dao.getAllTasksForUser(username);
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
        TaskInfo [] tasks = dao.getLatestTasksByName(username);
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
        TaskInfo [] tasks = dao.getLatestTasks(username);
        assertTrue(tasks.length > 0);
        for(int i=0; i<tasks.length; i++) {
            String userId = tasks[i].getUserId();
            assertTrue((userId != null && userId.equals(username)) ||  tasks[i].getAccessId() == GPConstants.ACCESS_PUBLIC);
        }
   }



    /**
     * 
     */
    public void testGetSuite() throws Exception  {
        String lsid = "urn:lsid:broad.mit.edu:genepatternsuites:1:1";
        SuiteInfo suite = dao.getSuite(lsid);
        assertNotNull(suite);
        assertEquals(lsid, suite.getLsid());

    }

    /**
     * 
     */
    public void testGetLatestSuites() throws Exception {
        SuiteInfo [] suites = dao.getLatestSuites();
        assertTrue(suites.length > 0);

    }

    /**
     * 
     */
    public void testGetLatestSuitesByUser() throws Exception {
        String user = "jlerner@broad.mit.edu";
        SuiteInfo [] suites = dao.getLatestSuites(user);
        assertTrue(suites.length > 0);

    }

    /**
     * 
     */
    public void testGetAllSuitesByUser() throws Exception {
        String user = "jlerner@broad.mit.edu";
        SuiteInfo [] suites = dao.getAllSuites(user);
        assertTrue(suites.length > 0);
    }

    /**
     * AdminHSQLservice#getSuiteMembership(String)
     */
    public void testGetSuiteMembershipByLsid() throws Exception {
        String taskLSID = "";
        SuiteInfo [] suites = dao.getSuiteMembership(taskLSID);
        assertTrue(suites.length > 0);

    }
    


    /*
     * Test method for 'org.genepattern.server.webservice.server.service.AdminDataService.getAllSuites()'
     */

    public void testGetAllSuites() throws Exception {
        SuiteInfo [] suites = dao.getLatestSuites();
        assertTrue(suites.length > 0);
    }




    public void testGetTasks() {
        String userID = "twomey";
        TaskInfo[] taskArray = (userID == null ? dao.getAllTasks() : dao.getAllTasksForUser(userID));
        List tasks = Arrays.asList(taskArray);
        this.assertTrue(tasks.size() > 0);
    }
 


    /**
     * @see AnalysisHypersonicDAO#addNewTask(String,String,int,String,String,String)
     */
    public void testAddNewTask() throws Exception {

        TaskInfo task = dao.getTask(1); // Get a task to copy
        TaskInfoAttributes tia = getTestTaskInfoAttributes();
        String taskName = "test";
        int newTaskId = (new AnalysisDAO()).addNewTask(taskName, task.getUserId(), task.getAccessId(), task.getDescription(), task
                .getParameterInfo(), tia.encode());

        TaskInfo newTask = dao.getTask(newTaskId);
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

}
