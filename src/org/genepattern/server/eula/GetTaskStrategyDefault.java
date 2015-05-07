/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.eula;

import org.apache.log4j.Logger;
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
    private static final Logger log = Logger.getLogger(GetTaskStrategyDefault.class);
    @Override
    public TaskInfo getTaskInfo(final String lsid) {
        //must be in a DB transaction before using TaskInfoCache
        boolean isInTransaction=HibernateUtil.isInTransaction();
        if (log.isDebugEnabled()) {
            log.debug("load taskInfo from DB for lsid="+lsid+", isInTransaction="+isInTransaction);
        }
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
