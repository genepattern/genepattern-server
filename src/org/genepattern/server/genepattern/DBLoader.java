/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.server.genepattern;

import java.rmi.RemoteException;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.cm.CategoryUtil;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.taskinstall.InstallInfo;
import org.genepattern.server.taskinstall.RecordInstallInfoToDb;
import org.genepattern.server.webservice.server.dao.AdminDAO;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.webservice.OmnigeneException;
import org.genepattern.webservice.ParameterFormatConverter;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
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
    static Logger log = Logger.getLogger(DBLoader.class);

    public static int CREATE = 1;
    public static int UPDATE = 2;
    public static int DELETE = 3;

    protected String _name;
    protected String _taskDescription;
    protected ParameterInfo[] _params;
    protected String _taskInfoAttributes;
    protected String user_id;
    protected int access_id = 1;
    
    //task installation details
    private InstallInfo installInfo;
    
    public void setInstallInfo(final InstallInfo taskInstallInfo) {
        this.installInfo=taskInstallInfo;
    }

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
     * 
     * @throws OmnigeneException
     * @deprecated - I can find no evidence that this method is used anymore (circa GP 3.6.0) - pcarr
     */
    public void load() throws OmnigeneException {
        log.error("Unexpected call to deprecated method, load()");
    }

    /**
     * Creates a new task in the analysis database
     * 
     * @throws OmnigeneException
     */
    public void create() throws OmnigeneException {
        log.debug("creating task, _name="+_name);
        AnalysisDAO ds = new AnalysisDAO();
        String parameter_info = ParameterFormatConverter.getJaxbString(this._params);

        int id;
        try {
            id = ds.addNewTask(this._name, this.user_id, this.access_id, this._taskDescription, parameter_info, this._taskInfoAttributes);
        }
        catch (Exception e) {
            throw new OmnigeneException("Unable to create new task! " + e.getMessage());
        } 
        try {
            recordInstallInfo();
        }
        catch (Throwable t) {
            log.error("Server exception recording installation details for task id="+id+", name="+_name, t);
        }
    }

    private String getLSIDOrName() {
        String lsid = null;
        if (_taskInfoAttributes == null || _taskInfoAttributes.trim().equals("")) {
            return _name;
        }
        try {
            lsid = (String) TaskInfoAttributes.decode(_taskInfoAttributes).get(GPConstants.LSID);
        }
        catch (Throwable t) {
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
        log.debug("updating task, _name="+_name);
        String lsid = getLSIDOrName();
        AnalysisDAO ds = new AnalysisDAO();
        int taskID = -1;
        try {
            // search for an existing task with the same name
            taskID = getTaskIDByName(lsid, user_id);
        }
        catch (OmnigeneException e) {
            // this is a new task, no taskID exists do nothing
        }
        catch (RemoteException re) {
            throw new OmnigeneException("Unable to load the task: " + re.getMessage());
        }
        if (taskID == -1) {
            create();
            return;
        }

        String parameter_info = ParameterFormatConverter.getJaxbString(this._params);
        try {
            ds.updateTask(taskID, this._taskDescription, parameter_info, this._taskInfoAttributes, user_id, access_id);
            log.info("Task (taskId="+taskID+") " + this._name + " updated.");
        }
        catch (Exception e) {
            throw new OmnigeneException("Unable to update " + _name + ": " + e.getMessage());
        }
        recordInstallInfo();
    }

    /**
     * Deletes a task from the database
     * 
     * @throws OmnigeneException, RemoteException
     */
    public void delete() throws OmnigeneException, RemoteException {
        log.debug("deleting task, _name="+_name);
        int taskID;
        AdminDAO ds = new AdminDAO();
        String lsid = getLSIDOrName();
        taskID = getTaskIDByName(lsid, user_id);
        if (taskID == -1) {
            throw new OmnigeneException("Unable to find the task to delete.");
        }
        try {
            ds.deleteTask(taskID);
            log.info("Task (taskId="+taskID+") " + this._name + " deleted from database.");
        }
        catch (Exception e) {
            throw new OmnigeneException("Unable to delete " + this._name + ". " + e.getMessage());
        }
        deleteInstallInfo();
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
            }
            else if (type == DBLoader.UPDATE) {
                update();
            }
            else if (type == DBLoader.DELETE) {
                delete();
            }
            else {
                throw new OmnigeneException(
                        "Unable to run the loader, running type should be specified: 1 (create), 2 (update) and 3 (delete)");
            }
        }
        catch (RemoteException re) {
            throw new OmnigeneException(re.getMessage());
        }
    }


    // search for an existing task with the same name
    static public int getTaskIDByName(String name, String user_id) throws OmnigeneException, RemoteException {
        log.debug("getTaskIDByName...\n\tname="+name+"\tuser_id="+user_id);
        AdminDAO ds = new AdminDAO();
        TaskInfo taskInfo = ds.getTask(name, user_id);
        if (log.isDebugEnabled()) {
            if (taskInfo == null) {
                log.debug("\ttaskInfo == null");
            }
            else {
                log.debug("\ttaskId="+taskInfo.getID());
            }
        }
        return (taskInfo == null ? -1 : taskInfo.getID());
    }
    
    ////////
    // helper methods for recording installation details into the DB
    ///////
    private LSID getLSIDOrNull() {
        final String lsidStr=getLSIDOrName();
        try {
            LSID lsid=new LSID(lsidStr);
            return lsid;
        }
        catch (Throwable t) {
            log.debug(t);
            return null;
        }
    }

    private void recordInstallInfo() {
        final LSID lsid=getLSIDOrNull();
        if (lsid==null) {
            log.error("Error recording installation details for task="+getLSIDOrName()+", lsid not set");
            return;
        }
        log.debug("installed new task "+lsid.toString());
        if (installInfo == null) {
            log.debug("creating new TaskInstallInfo()");
            installInfo=new InstallInfo();
        }
        
        //initialize values
        installInfo.setLsid(lsid);
        installInfo.setUserId(user_id);
        installInfo.setDateInstalled(new Date());
        
        //TODO: optionally record zip file path
        //TODO: optionally record previous lsid
        //TODO: optionally record libdir path
         
        log.debug("    from source: "+installInfo.getType());
        log.debug("    as user: "+installInfo.getUserId());
        log.debug("    on date: "+installInfo.getDateInstalled());
        if (installInfo.getRepositoryUrl() != null) {
            log.debug("    repository: "+installInfo.getRepositoryUrl());
        } 
        
        try {
            log.debug("initializing categories...");
            initCategories();
            log.debug("saving to db ...");
            new RecordInstallInfoToDb(HibernateUtil.instance()).save(installInfo);
            log.debug("done!");
        }
        catch (Throwable t) {
            log.error("failed! ", t);
        }
    }
    
    protected void initCategories() {
        TaskInfoAttributes tia=TaskInfoAttributes.decode(_taskInfoAttributes);
        List<String> categoryNames=CategoryUtil.getCategoriesFromManifest(tia);
        for(final String name : categoryNames) {
            installInfo.addCategory(name);
        }
    }
    
    private void deleteInstallInfo() {
        final LSID lsid=getLSIDOrNull();
        if (lsid==null) {
            log.error("Error deleting installation record for task="+getLSIDOrName()+", lsid not set");
            return;
        }
        else {
            log.debug("deleted task "+lsid.toString());
            //TODO: remove record from DB
        }
        log.debug("deleting from db ...");
        try {
            int numDeleted=new RecordInstallInfoToDb(HibernateUtil.instance()).delete(lsid.toString());
            log.debug("done! numDeleted="+numDeleted);
        }
        catch (Throwable t) {
            log.error("failed! ", t);
        }
    }
}
