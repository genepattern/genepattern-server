package org.genepattern.server.webservice.server.dao;

import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.webservice.*;
import org.hibernate.Transaction;

public class AdminDataService extends BaseService {

    private static Logger log = Logger.getLogger(AnalysisDataService.class);

    private static AdminDataService theInstance = null;

    private AdminDAO adminDAO = new AdminDAO();
    private AnalysisDAO analysisDAO = new AnalysisDAO();

    public static synchronized AdminDataService getInstance() {
        if (theInstance == null) {
            theInstance = new AdminDataService();
        }
        return theInstance;

    }

    public TaskInfo getTask(int taskId) throws AdminDAOSysException {
        Transaction transaction = null;
        try {
            if (!getSession().getTransaction().isActive()) {
                transaction = getSession().beginTransaction();
            }

            TaskInfo task = adminDAO.getTask(taskId);

            if (transaction != null) {
                transaction.commit();
            }

            return task;
        }
        catch (Exception e) {
            getSession().getTransaction().rollback();
            log.error(e);
            throw new OmnigeneException(e.getMessage());
        }
        finally {
            cleanupSession();
        }

    }

    public TaskInfo getTask(String lsidOrTaskName, String username) throws OmnigeneException {
        Transaction transaction = null;
        try {
            if (!getSession().getTransaction().isActive()) {
                transaction = getSession().beginTransaction();
            }

            TaskInfo task = adminDAO.getTask(lsidOrTaskName, username);

            if (transaction != null) {
                transaction.commit();
            }

            return task;
        }
        catch (Exception e) {
            getSession().getTransaction().rollback();
            log.error(e);
            throw new OmnigeneException(e.getMessage());
        }
        finally {
            cleanupSession();
        }

    }

    public int getTaskId(String lsidOrTaskName, String username) throws OmnigeneException {
        Transaction transaction = null;
        try {
            if (!getSession().getTransaction().isActive()) {
                transaction = getSession().beginTransaction();
            }

            TaskInfo task = adminDAO.getTask(lsidOrTaskName, username);
            int taskId = (task != null ? task.getID() : -1);

            if (transaction != null) {
                transaction.commit();
            }

            return taskId;
        }
        catch (Exception e) {
            getSession().getTransaction().rollback();
            log.error(e);
            throw new OmnigeneException(e.getMessage());
        }
        finally {
            cleanupSession();
        }

    }

    public TaskInfo[] getAllTasksForUser(String username) throws AdminDAOSysException {
        Transaction transaction = null;
        try {
            if (!getSession().getTransaction().isActive()) {
                transaction = getSession().beginTransaction();
            }

            TaskInfo[] tasks = adminDAO.getAllTasksForUser(username);

            if (transaction != null) {
                transaction.commit();
            }

            return tasks;
        }
        catch (Exception e) {
            getSession().getTransaction().rollback();
            log.error(e);
            throw new OmnigeneException(e.getMessage());
        }
        finally {
            cleanupSession();
        }

    }

    public TaskInfo[] getAllTasks() throws AdminDAOSysException {
        Transaction transaction = null;
        try {
            if (!getSession().getTransaction().isActive()) {
                transaction = getSession().beginTransaction();
            }

            TaskInfo[] tasks = adminDAO.getAllTasks();

            if (transaction != null) {
                transaction.commit();
            }

            return tasks;
        }
        catch (Exception e) {
            getSession().getTransaction().rollback();
            log.error(e);
            throw new OmnigeneException(e.getMessage());
        }
        finally {
            cleanupSession();
        }

    }

    public TaskInfo[] getLatestTasks(String username) throws AdminDAOSysException {
        Transaction transaction = null;
        try {
            if (!getSession().getTransaction().isActive()) {
                transaction = getSession().beginTransaction();
            }

            TaskInfo[] tasks = adminDAO.getLatestTasks( username );

            if (transaction != null) {
                transaction.commit();
            }

            return tasks;
        }
        catch (Exception e) {
            getSession().getTransaction().rollback();
            log.error(e);
            throw new OmnigeneException(e.getMessage());
        }
        finally {
            cleanupSession();
        }

    }

    public TaskInfo[] getLatestTasksByName(String username) throws AdminDAOSysException {
        Transaction transaction = null;
        try {
            if (!getSession().getTransaction().isActive()) {
                transaction = getSession().beginTransaction();
            }

            TaskInfo[] tasks = adminDAO.getLatestTasksByName( username );

            if (transaction != null) {
                transaction.commit();
            }

            return tasks;
        }
        catch (Exception e) {
            getSession().getTransaction().rollback();
            log.error(e);
            throw new OmnigeneException(e.getMessage());
        }
        finally {
            cleanupSession();
        }

    }
    
    public List getTasks(String userID) {
        Transaction transaction = null;
        try {
            if (!getSession().getTransaction().isActive()) {
                transaction = getSession().beginTransaction();
            }

            TaskInfo[] taskArray = (userID == null ? adminDAO.getAllTasks() : adminDAO.getAllTasksForUser(userID));

            List tasks = Arrays.asList(taskArray);

            if (transaction != null) {
                transaction.commit();
            }

            return tasks;
        }
        catch (Exception e) {
            getSession().getTransaction().rollback();
            log.error(e);
            throw new OmnigeneException(e.getMessage());
        }
        finally {
            cleanupSession();
        }

    }
 

    /**
     * To create a new regular task
     * 
     * @param taskName
     * @param user_id
     * @param access_id
     * @param description
     * @param parameter_info
     * @throws OmnigeneException
     * @return task ID
     */
    public int addNewTask(String taskName, String user_id, int access_id, String description, String parameter_info,
            String taskInfoAttributes) {
        Transaction transaction = null;
        try {
            if (!getSession().getTransaction().isActive()) {
                transaction = getSession().beginTransaction();
            }

            int taskId = analysisDAO.addNewTask(taskName, user_id, access_id, description, parameter_info,
                    taskInfoAttributes);

            if (transaction != null) {
                transaction.commit();
            }

            return taskId;
        }
        catch (Exception e) {
            getSession().getTransaction().rollback();
            log.error(e);
            throw new OmnigeneException(e.getMessage());
        }
        finally {
            cleanupSession();
        }

    }

    /**
     * Updates task description and parameters
     * 
     * @param taskID
     *            task ID
     * @param description
     *            task description
     * @param parameter_info
     *            parameters as a xml string
     * @return No. of updated records
     * @throws OmnigeneException
     * @throws RemoteException
     */
    public int updateTask(int taskId, String taskDescription, String parameter_info, String taskInfoAttributes,
            String user_id, int access_id) throws OmnigeneException {
        Transaction transaction = null;
        try {
            if (!getSession().getTransaction().isActive()) {
                transaction = getSession().beginTransaction();
            }

            int updateCount = analysisDAO.updateTask(taskId, taskDescription, parameter_info, taskInfoAttributes,
                    user_id, access_id);

            if (transaction != null) {
                transaction.commit();
            }

            return updateCount;
        }
        catch (Exception e) {
            getSession().getTransaction().rollback();
            log.error(e);
            throw new OmnigeneException(e.getMessage());
        }
        finally {
            cleanupSession();
        }

    }

    /**
     * Updates task parameters
     * 
     * @param taskID
     *            task ID
     * @param parameter_info
     *            parameters as a xml string
     * @throws OmnigeneException
     * @throws RemoteException
     * @return No. of updated records
     */
    public int updateTask(int taskId, String parameter_info, String taskInfoAttributes, String user_id, int access_id)
            throws OmnigeneException {
        Transaction transaction = null;
        try {
            if (!getSession().getTransaction().isActive()) {
                transaction = getSession().beginTransaction();
            }

            int updateCount = analysisDAO.updateTask(taskId, parameter_info, taskInfoAttributes, user_id, access_id);
            if (transaction != null) {
                transaction.commit();
            }

            return updateCount;
        }
        catch (Exception e) {
            getSession().getTransaction().rollback();
            log.error(e);
            throw new OmnigeneException(e.getMessage());
        }

    }

    /**
     * To remove registered task based on task ID
     * 
     * @param taskID
     * @throws OmnigeneException
     * @throws RemoteException
     * @return No. of updated records
     */
    public int deleteTask(int taskID) throws OmnigeneException {

        Transaction transaction = null;
        try {
            if (!getSession().getTransaction().isActive()) {
                transaction = getSession().beginTransaction();
            }

            int count = adminDAO.deleteTask(taskID);

            if (transaction != null) {
                transaction.commit();
            }

            return count;
        }
        catch (Exception e) {
            getSession().getTransaction().rollback();
            log.error(e);
            throw new OmnigeneException(e.getMessage());
        }
        finally {
            cleanupSession();
        }

    }


    public SuiteInfo getSuite(String taskId) throws AdminDAOSysException {
        Transaction transaction = null;
        try {
            if (!getSession().getTransaction().isActive()) {
                transaction = getSession().beginTransaction();
            }

            SuiteInfo suite = adminDAO.getSuite( taskId );

            if (transaction != null) {
                transaction.commit();
            }

            return suite;
        }
        catch (Exception e) {
            getSession().getTransaction().rollback();
            log.error(e);
            throw new OmnigeneException(e.getMessage());
        }
        finally {
            cleanupSession();
        }

    }

    public SuiteInfo[] getLatestSuites() throws AdminDAOSysException {
        Transaction transaction = null;
        try {
            if (!getSession().getTransaction().isActive()) {
                transaction = getSession().beginTransaction();
            }

            SuiteInfo [] suites = adminDAO.getLatestSuites();

            if (transaction != null) {
                transaction.commit();
            }

            return suites;
        }
        catch (Exception e) {
            getSession().getTransaction().rollback();
            log.error(e);
            throw new OmnigeneException(e.getMessage());
        }
        finally {
            cleanupSession();
        }

    }

    public SuiteInfo[] getLatestSuites(String userName) throws AdminDAOSysException {
        Transaction transaction = null;
        try {
            if (!getSession().getTransaction().isActive()) {
                transaction = getSession().beginTransaction();
            }

            SuiteInfo [] suites = adminDAO.getLatestSuites( userName );

            if (transaction != null) {
                transaction.commit();
            }

            return suites;
        }
        catch (Exception e) {
            getSession().getTransaction().rollback();
            log.error(e);
            throw new OmnigeneException(e.getMessage());
        }
        finally {
            cleanupSession();
        }

    }

    public SuiteInfo[] getAllSuites() throws AdminDAOSysException {
        Transaction transaction = null;
        try {
            if (!getSession().getTransaction().isActive()) {
                transaction = getSession().beginTransaction();
            }

            SuiteInfo [] suites = adminDAO.getAllSuites();

            if (transaction != null) {
                transaction.commit();
            }

            return suites;
        }
        catch (Exception e) {
            getSession().getTransaction().rollback();
            log.error(e);
            throw new OmnigeneException(e.getMessage());
        }
        finally {
            cleanupSession();
        }
    }

    public SuiteInfo[] getAllSuites(String userName) throws AdminDAOSysException {
        Transaction transaction = null;
        try {
            if (!getSession().getTransaction().isActive()) {
                transaction = getSession().beginTransaction();
            }

            SuiteInfo [] suites = adminDAO.getAllSuites( userName );

            if (transaction != null) {
                transaction.commit();
            }

            return suites;
        }
        catch (Exception e) {
            getSession().getTransaction().rollback();
            log.error(e);
            throw new OmnigeneException(e.getMessage());
        }
        finally {
            cleanupSession();
        }
    }

    public SuiteInfo[] getSuiteMembership(String taskLsid) throws AdminDAOSysException {
        Transaction transaction = null;
        try {
            if (!getSession().getTransaction().isActive()) {
                transaction = getSession().beginTransaction();
            }

            SuiteInfo [] suites = adminDAO.getSuiteMembership( taskLsid );

            if (transaction != null) {
                transaction.commit();
            }

            return suites;
        }
        catch (Exception e) {
            getSession().getTransaction().rollback();
            log.error(e);
            throw new OmnigeneException(e.getMessage());
        }
        finally {
            cleanupSession();
        }
    }

}
