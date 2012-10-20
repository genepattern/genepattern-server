package org.genepattern.server.eula;

import org.genepattern.server.database.HibernateUtil;
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
        //must be in a DB transaction before using TaskInfoCache
        boolean isInTransaction=HibernateUtil.isInTransaction();
        try {
            HibernateUtil.beginTransaction();
            TaskInfo taskInfo=TaskInfoCache.instance().getTask(lsid);
            return taskInfo;
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }
}
