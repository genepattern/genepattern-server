/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.eula;

import org.apache.log4j.Logger;
import org.genepattern.server.database.HibernateSessionManager;
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
    
    private final HibernateSessionManager mgr;
    
    /** should pass in Hibernate session */
    public GetTaskStrategyDefault() {
        this(org.genepattern.server.database.HibernateUtil.instance());
    }
    public GetTaskStrategyDefault(final HibernateSessionManager mgr) {
        this.mgr=mgr;
    }
    
    @Override
    public TaskInfo getTaskInfo(final String lsid) {
        //must be in a DB transaction before using TaskInfoCache
        boolean isInTransaction=mgr.isInTransaction();
        try {
            mgr.beginTransaction();
            TaskInfo taskInfo=TaskInfoCache.instance().getTask(mgr, lsid);
            return taskInfo;
        }
        catch (Throwable t) {
            log.error("Unexpected error getting taskInfo for lsid="+lsid, t);
            mgr.closeCurrentSession();
            return null;
        }
        finally {
            if (!isInTransaction) {
                mgr.closeCurrentSession();
            }
        }
    }
}
