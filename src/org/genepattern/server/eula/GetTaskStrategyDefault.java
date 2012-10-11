package org.genepattern.server.eula;

import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoCache;

/**
 * The default strategy for getting a a TaskInfo instance from an lsid, is to 
 * use the TaskInfoCache.
 * 
 * @author pcarr
 */
public class GetTaskStrategyDefault implements GetTaskStrategy {
    //@Override
    public TaskInfo getTaskInfo(final String lsid) {
        return TaskInfoCache.instance().getTask(lsid);
    }
}
