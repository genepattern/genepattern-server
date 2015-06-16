/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
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
