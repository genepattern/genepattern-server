package org.genepattern.server.eula;

import org.genepattern.webservice.TaskInfo;

public interface GetTaskStrategy {
    /**
     * Implement the strategy for getting the TaskInfo instance for the given task lsid.
     * 
     * @param lsid
     * @return
     */
    TaskInfo getTaskInfo(String lsid);
}
