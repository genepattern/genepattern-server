/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.server.webservice.server.local;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.genepattern.util.LSID;
import org.genepattern.webservice.SuiteInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.WebServiceException;

public interface IAdminClient {

    public abstract List<String> getVersions(LSID lsid);

    /**
     * Gets all tasks in the database regardless of privacy or owner. Should only be called for a user with moduleAdmin
     * priveleges.
     * 
     * @return All tasks in the database.
     */
    public abstract TaskInfo[] getAllTasksForModuleAdmin();

    /**
     * Gets all tasks in the database owned by the username passed to the constructor.
     * 
     * @return The tasks.
     */
    public abstract TaskInfo[] getTasksOwnedBy();

    public abstract TreeMap<String, TaskInfo> getTaskCatalogByLSID(
            Collection<TaskInfo> tasks);

    public abstract TreeMap<String, TaskInfo> getTaskCatalogByLSID()
            throws WebServiceException;

    public abstract Collection<TaskInfo> getTaskCatalog()
            throws WebServiceException;

    public abstract Collection<TaskInfo> getLatestTasks()
            throws WebServiceException;

    /**
     * return a map keyed by taskType with values being alphabetically sorted list of the tasks that are part of that
     * type
     */
    public abstract Map<String, Collection<TaskInfo>> getLatestTasksByType()
            throws WebServiceException;

    public abstract TaskInfo getTask(String lsid) throws WebServiceException;

    public abstract SuiteInfo getSuite(String lsid) throws WebServiceException;

    public abstract SuiteInfo[] getAllSuites() throws WebServiceException;

    public abstract String getUserProperty(String key);

}
