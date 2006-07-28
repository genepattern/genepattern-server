package org.genepattern.server.webservice.server.dao;

import java.util.Map;

import org.genepattern.webservice.SuiteInfo;
import org.genepattern.webservice.TaskInfo;

import junit.framework.TestCase;

public class AdminHSQLDAOTest extends TestCase {

    private AnalysisHypersonicDAO analysisDao;
    private static int PUBLIC_ACCESS_ID = 1;

    private AdminHSQLDAO dao;

    protected void setUp() throws Exception {
        dao = new AdminHSQLDAO();
        analysisDao = new AnalysisHypersonicDAO();
        super.setUp();
    }

    /**
     * @see AdminHSQLDAO#getTaskId(String, String)
     */
    public void testGetTaskId() throws Exception {
        int expectedTaskId = 1;
        
        TaskInfo task = dao.getTask(expectedTaskId);
        int taskId = dao.getTaskId(task.getName(), task.getUserId());
        assertEquals(expectedTaskId, taskId);
        
        int noTaskId = dao.getTaskId("no task with this name", "");
        assertEquals(-1, noTaskId);

    }

    /**
     * @see AdminHSQLDAO#getAllTasks()
     */
    public void testGetAllTasks()  throws Exception {
        
        TaskInfo [] tasks = dao.getAllTasks();
        assertTrue(tasks.length > 0);

    }

    /**
     * @see AdminHSQLDAO#getAllTasks(String)
     */
    public void testGetAllTasksByUsername()  throws Exception {
        String username = "twomey";
        TaskInfo [] tasks = dao.getAllTasks(username);
        assertTrue(tasks.length > 0);
        for(int i=0; i<tasks.length; i++) {
            String userId = tasks[i].getUserId();
            assertTrue((userId != null && userId.equals(username)) ||  tasks[i].getAccessId() == PUBLIC_ACCESS_ID);
        }

    }

    /**
     * @see AdminHSQLDAO#getLatestTasksByName(String) 
     */
    public void testGetLatestTasksByName() throws Exception {
        String username = "twomey";
        TaskInfo [] tasks = dao.getLatestTasksByName(username);
        assertTrue(tasks.length > 0);
        for(int i=0; i<tasks.length; i++) {
            String userId = tasks[i].getUserId();
            assertTrue((userId != null && userId.equals(username)) ||  tasks[i].getAccessId() == PUBLIC_ACCESS_ID);
        }
    }

    /**
     * @see AdminHSQLDAO#getLatestTasks(String)
     */
    public void testGetLatestTasks() throws Exception {
        String username = "twomey";
        TaskInfo [] tasks = dao.getLatestTasks(username);
        assertTrue(tasks.length > 0);
        for(int i=0; i<tasks.length; i++) {
            String userId = tasks[i].getUserId();
            assertTrue((userId != null && userId.equals(username)) ||  tasks[i].getAccessId() == PUBLIC_ACCESS_ID);
        }
   }


    /**
     * @see AdminHSQLDAO#getSuite(String)
     */
    public void testGetSuite() throws Exception  {
        String lsid = "";
        SuiteInfo suite = dao.getSuite(lsid);
        assertNotNull(suite);
        assertEquals(lsid, suite.getLsid());

    }

    /**
     * @see AdminHSQLDAO#getLatestSuites()
     */
    public void testGetLatestSuites() throws Exception {
        SuiteInfo [] suites = dao.getLatestSuites();
        assertTrue(suites.length > 0);

    }

    /**
     * @see AdminHSQLDAO#getLatestSuites(String)
     */
    public void testGetLatestSuitesByUser() throws Exception {
        String user = "towmey";
        SuiteInfo [] suites = dao.getLatestSuites(user);
        assertTrue(suites.length > 0);

    }

    /**
     * @see AdminHSQLDAO#getAllSuites()
     */
    public void testGetAllSuites() throws Exception {
        SuiteInfo [] suites = dao.getLatestSuites();
        assertTrue(suites.length > 0);
    }

    /**
     * @see AdminHSQLDAO#getAllSuites(String)
     */
    public void testGetAllSuitesByUser() throws Exception {
        String user = "towmey";
        SuiteInfo [] suites = dao.getAllSuites(user);
        assertTrue(suites.length > 0);
    }

    /**
     * AdminHSQLDAO#getSuiteMembership(String)
     */
    public void testGetSuiteMembershipByLsid() throws Exception {
        String lsid = "";
        SuiteInfo [] suites = dao.getSuiteMembership(lsid);
        assertTrue(suites.length > 0);

    }

    /**
     * @see AdminHSQLDAO#getSchemaProperties()
     */
    public void testGetSchemaProperties() {
        
        Map props = dao.getSchemaProperties();

    }



}
