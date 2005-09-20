package org.genepattern.server.dbloader;

import java.rmi.RemoteException;

import org.genepattern.server.webservice.server.AnalysisJobDataSource;
import org.genepattern.server.util.BeanReference;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.OmnigeneException;
import org.genepattern.webservice.ParameterFormatConverter;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfoAttributes;

/**
 * <p>
 * Title: DBLoader.java
 * </p>
 * <p>
 * Description: This class implements task loading (add and update) and
 * deletion. All task dbloader class will extends this class and implement setup
 * method to setup task name, description, class name and
 * <code>ParameterInfo<code> array </p>
 * @author Hui Gong
 * @version 1.0
 */

public abstract class DBLoader {
	protected String _name;

	protected String _taskDescription;

	protected ParameterInfo[] _params;

	protected String _taskInfoAttributes;

	public static int CREATE = 1;

	public static int UPDATE = 2;

	public static int DELETE = 3;

	protected String user_id;

	protected int access_id = 1;

	/**
	 * Constructor
	 */
	public DBLoader() {
		setup();
	}

	/**
	 * abstract method waiting to be implemented, task name, description, class
	 * name and parameters should be initialized in here.
	 */
	public abstract void setup();

	/**
	 * loads an analysis task into the database, it will add a new task if the
	 * task doesn't exist; and it will update the task description and
	 * parameters if the task is already in the database.
	 * 
	 * @throws OmnigeneException
	 */
	public void load() throws OmnigeneException {
		String lsid = getLSIDOrName();

		int taskID = -1;
		AnalysisJobDataSource ds = getDataSource();
		try {
			// search for an existing task with the same name
			taskID = getTaskIDByName(lsid, user_id);
		} catch (OmnigeneException e) {
			//this is a new task, no taskID exists
			// do nothing
		} catch (RemoteException re) {
			throw new OmnigeneException("Unable to load the task: "
					+ re.getMessage());
		}
		ParameterFormatConverter pfc = new ParameterFormatConverter();
		String parameter_info = pfc.getJaxbString(this._params);
		//task doesn't exist
		if (taskID < 0) {
			//create new task
			int id;
			try {
				id = ds.addNewTask(this._name, this.user_id, this.access_id,
						this._taskDescription, parameter_info,
						this._taskInfoAttributes);
				System.out.println(this._name + " has been created with id "
						+ id);
			} catch (Exception e) {
				throw new OmnigeneException("Unable to create new task! "
						+ e.getMessage());
			}
			//start new analysis task
			try {
				// System.out.println("Starting task: "+this._classname+" ...");
				ds.startNewTask(id);
				// System.out.println(this._classname+ " started");
			} catch (Exception e) {
				throw new OmnigeneException("Unable to start up new task."
						+ e.getMessage());
			}
		}
		//task exist, update task
		else {
			try {
				ds.updateTask(taskID, this._taskDescription, parameter_info,
						this._taskInfoAttributes, user_id, access_id);
				System.out.println(this._name + " has been updated.");
			} catch (Exception e) {
				throw new OmnigeneException("Unable to update task"
						+ e.getMessage());
			}
		}

	}

	/**
	 * Creates a new task in the analysis database
	 * 
	 * @throws OmnigeneException
	 */
	public void create() throws OmnigeneException {
		AnalysisJobDataSource ds = getDataSource();
		ParameterFormatConverter pfc = new ParameterFormatConverter();
		String parameter_info = pfc.getJaxbString(this._params);
		/*
		 * int taskID = -1; try{ // search for an existing task with the same
		 * name taskID = getTaskIDByName(_name, user_id); }
		 * catch(OmnigeneException e){ //this is a new task, no taskID exists //
		 * do nothing } catch(RemoteException re){ throw new
		 * OmnigeneException("Unable to load the task: "+re.getMessage()); }
		 * 
		 * if(taskID != -1) { update(); return; //throw new
		 * OmnigeneException("Unable to create a new task: " + _name + " already
		 * exists."); }
		 */

		int id;
		try {
			id = ds.addNewTask(this._name, this.user_id, this.access_id,
					this._taskDescription, parameter_info,
					this._taskInfoAttributes);
			//            System.out.println(this._name+" is created with id "+id);
		} catch (Exception e) {
			throw new OmnigeneException("Unable to create new task! "
					+ e.getMessage());
		}
		//start new analysis task
		try {
			// System.out.println("Starting task: "+this._classname+" ...");
			ds.startNewTask(id);
			// System.out.println(this._classname+ " started");
		} catch (Exception e) {
			throw new OmnigeneException("Unable to start up new task."
					+ e.getMessage());
		}
	}

	private String getLSIDOrName() {
		String lsid = null;
		if (_taskInfoAttributes == null
				|| _taskInfoAttributes.trim().equals("")) {
			return _name;
		}
		try {
			lsid = (String) TaskInfoAttributes.decode(_taskInfoAttributes).get(
					GPConstants.LSID);
		} catch (Throwable t) {
		}
		if (lsid == null || lsid.trim().equals("")) {
			lsid = _name;
		}
		return lsid;
	}

	/**
	 * Updates an existing task
	 * 
	 * @throws OmnigeneException
	 */
	public void update() throws OmnigeneException {
		String lsid = getLSIDOrName();
		AnalysisJobDataSource ds = getDataSource();
		int taskID = -1;
		try {
			// search for an existing task with the same name
			taskID = getTaskIDByName(lsid, user_id);
		} catch (OmnigeneException e) {
			//this is a new task, no taskID exists
			// do nothing
		} catch (RemoteException re) {
			throw new OmnigeneException("Unable to load the task: "
					+ re.getMessage());
		}
		if (taskID == -1) {
			create();
			return;
		}

		ParameterFormatConverter pfc = new ParameterFormatConverter();
		String parameter_info = pfc.getJaxbString(this._params);
		try {

			ds.updateTask(taskID, this._taskDescription, parameter_info,
					this._taskInfoAttributes, user_id, access_id);
			System.out.println(this._name + " updated.");
		} catch (Exception e) {
			throw new OmnigeneException("Unable to update " + _name + ": "
					+ e.getMessage());
		}
	}

	/**
	 * Deletes a task from the database
	 * 
	 * @throws OmnigeneException,
	 *             RemoteException
	 */
	public void delete() throws OmnigeneException, RemoteException {
		int taskID;
		AnalysisJobDataSource ds = getDataSource();
		String lsid = getLSIDOrName();
		taskID = getTaskIDByName(lsid, user_id);
		if (taskID == -1) {
			throw new OmnigeneException("Unable to find the task to delete.");
		}
		//stop the task before delete it
		try {
			System.out.println("Stopping task " + this._name);
			ds.stopTask(taskID);
			System.out.println("Task " + this._name + " stopped.");
		} catch (RemoteException e) {
			throw new OmnigeneException("Unable to stop " + this._name + ". "
					+ e.getMessage());
		}
		try {
			ds.deleteTask(taskID);
			System.out
					.println("Task " + this._name + " deleted from database.");
		} catch (Exception e) {
			throw new OmnigeneException("Unable to delete " + this._name + ". "
					+ e.getMessage());
		}
	}

	/**
	 * Run the <code>DBLoader<code> class based on the type
	 * @param type the type of action: DBLoader.CREATE, DBLoader.UPDATE and DBLoader.DELETE
	 * @throws OmnigeneException
	 */
	public void run(int type) throws OmnigeneException {
		try {
			if (type == DBLoader.CREATE) {
				create();
			} else if (type == DBLoader.UPDATE) {
				update();
			} else if (type == DBLoader.DELETE) {
				delete();
			} else {
				throw new OmnigeneException(
						"Unable to run the loader, running type should be specified: 1 (create), 2 (update) and 3 (delete)");
			}
		} catch (RemoteException re) {
			throw new OmnigeneException(re.getMessage());
		}
	}

	private static AnalysisJobDataSource getDataSource()
			throws OmnigeneException {
		AnalysisJobDataSource ds;
		try {
			ds = BeanReference.getAnalysisJobDataSourceEJB();
		} catch (Exception e) {
			throw new OmnigeneException(
					"Unable to find analysisJobDataSource: " + e.getMessage());
		}
		return ds;
	}

	// search for an existing task with the same name
	public int getTaskIDByName(String name, String user_id)
			throws OmnigeneException, RemoteException {
		AnalysisJobDataSource ds = getDataSource();
		int taskID = ds.getTaskIDByName(name, user_id);
		return taskID;
	}
}