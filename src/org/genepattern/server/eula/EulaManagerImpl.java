package org.genepattern.server.eula;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.eula.dao.RecordEulaToDb;
import org.genepattern.webservice.TaskInfo;

/**
 * Methods for managing End-user license agreements (EULA) for GenePattern modules.
 * 
 * @author pcarr
 */
public class EulaManagerImpl implements IEulaManager {
    public static Logger log = Logger.getLogger(EulaManagerImpl.class);

    //TODO: use Strategy pattern for this method
    private GetEulaFromTask getEulaFromTask = null;
    /* (non-Javadoc)
     * @see org.genepattern.server.eula.IEulaManager#setGetEulaFromTask(org.genepattern.server.eula.GetEulaFromTask)
     */
    public void setGetEulaFromTask(GetEulaFromTask impl) {
        this.getEulaFromTask=impl;
    }
    private GetEulaFromTask getGetEulaFromTask() {
        //allow for dependency injection, via setGetEulaFromTask
        if (getEulaFromTask != null) {
            return getEulaFromTask;
        }
        
        //otherwise, hard-coded rule        
        //option 1: license= in manifest
        //return new GetEulaFromTaskImpl01();
        //option 2: support file named '*license*' in tasklib
        return new GetEulaAsSupportFile();
    }
    
    private GetTaskStrategy getTaskStrategy = null;
    /* (non-Javadoc)
     * @see org.genepattern.server.eula.IEulaManager#setGetTaskStrategy(org.genepattern.server.eula.GetTaskStrategy)
     */
    public void setGetTaskStrategy(GetTaskStrategy impl) {
        this.getTaskStrategy=impl;
    }


    private RecordEula recordEulaStrategy = null;
    /* (non-Javadoc)
     * @see org.genepattern.server.eula.IEulaManager#setRecordEulaStrategy(org.genepattern.server.eula.RecordEula)
     */
    public void setRecordEulaStrategy(final RecordEula impl) {
        this.recordEulaStrategy=impl;
    }
    //factory method for getting the method for recording the EULA
    private RecordEula getRecordEula(EulaInfo eulaInfo) {
        if (recordEulaStrategy != null) {
            return recordEulaStrategy;
        }
        //for debugging, the RecordEulaStub can be used
        //return RecordEulaStub.instance(); 
        //TODO: if necessary, remote record to external web service
        return new RecordEulaToDb();
    }
    
    /* (non-Javadoc)
     * @see org.genepattern.server.eula.IEulaManager#requiresEula(org.genepattern.server.config.ServerConfiguration.Context)
     */
    public boolean requiresEula(Context taskContext) {
        final List<EulaInfo> notYetAgreed = getEulaInfos(taskContext,false);
        if (notYetAgreed.size()>0) {
            return true;
        }
        return false;
    }
    
    /* (non-Javadoc)
     * @see org.genepattern.server.eula.IEulaManager#getAllEulaForModule(org.genepattern.server.config.ServerConfiguration.Context)
     */
    public List<EulaInfo> getAllEulaForModule(final Context taskContext) {
        List<EulaInfo> eulaInfos = getEulaInfos(taskContext, true);
        return eulaInfos;
    }

    /* (non-Javadoc)
     * @see org.genepattern.server.eula.IEulaManager#getPendingEulaForModule(org.genepattern.server.config.ServerConfiguration.Context)
     */
    public List<EulaInfo> getPendingEulaForModule(final Context taskContext) {
        List<EulaInfo> eulaInfos = getEulaInfos(taskContext, false);
        return eulaInfos;
    }
    
    /* (non-Javadoc)
     * @see org.genepattern.server.eula.IEulaManager#recordEula(org.genepattern.server.config.ServerConfiguration.Context)
     */
    public void recordEula(final Context taskContext) throws IllegalArgumentException {
        if (taskContext == null) {
            throw new IllegalArgumentException("taskContext==null");
        }
        TaskInfo taskInfo = taskContext.getTaskInfo();
        if (taskInfo==null) {
            throw new IllegalArgumentException("taskInfo==null");
        }
        if (taskInfo.getLsid()==null || taskInfo.getLsid().length()==0) {
            throw new IllegalArgumentException("taskInfo not set");
        }
        if (taskContext.getUserId()==null) {
            throw new IllegalArgumentException("userId==null");
        }
        if (taskContext.getUserId().length()==0) {
            throw new IllegalArgumentException("userId not set");
        }

        log.debug("recording EULA for user="+taskContext.getUserId()+", lsid="+taskContext.getTaskInfo().getLsid());
        
        List<EulaInfo> eulaInfos = getEulaInfosFromTaskInfo(taskInfo);
        for(EulaInfo eulaInfo : eulaInfos) {
            try {
                RecordEula recordEula = getRecordEula(eulaInfo);
                recordEula.recordLicenseAgreement(taskContext.getUserId(), eulaInfo.getModuleLsid());
            }
            catch (Exception e) {
                //TODO: report back to end user
                log.error("Error recording EULA for userId="+taskContext.getUserId()+", module="+eulaInfo.getModuleName()+", lsid="+eulaInfo.getModuleLsid());
            }
        }
    }

    /**
     * Get the list of required End-user license agreements for the given module or pipeline.
     * 
     * @param taskContext
     * @param includeAll
     * @return
     */
    private List<EulaInfo> getEulaInfos(final Context taskContext, final boolean includeAll) {
        if (taskContext == null) {
            throw new IllegalArgumentException("taskContext==null");
        }
        TaskInfo taskInfo = taskContext.getTaskInfo();
        if (taskInfo==null) {
            throw new IllegalArgumentException("taskInfo==null");
        }
        if (taskInfo.getLsid()==null || taskInfo.getLsid().length()==0) {
            throw new IllegalArgumentException("taskInfo not set");
        }
        List<EulaInfo> eulaObjs = getEulaInfosFromTaskInfo(taskInfo);
        if (includeAll) {
            return eulaObjs;
        }
        
        //only return ones which require user agreement
        if (taskContext.getUserId()==null) {
            throw new IllegalArgumentException("userId==null");
        }
        if (taskContext.getUserId().length()==0) {
            throw new IllegalArgumentException("userId not set");
        }
        
        List<EulaInfo> notYetAgreed = new ArrayList<EulaInfo>();
        for(EulaInfo eulaObj : eulaObjs) {
            boolean hasAgreed = false;
            final String userId=taskContext.getUserId();
            final String lsid=eulaObj.getModuleLsid();
            try {
                RecordEula recordEula = getRecordEula(eulaObj);
                hasAgreed = recordEula.hasUserAgreed(taskContext.getUserId(), eulaObj);
            }
            catch (Throwable t) {
                //TODO: report error back to end-user
                log.error("Error recording eula, userId="+userId+", lsid="+lsid, t);
            }
            if (!hasAgreed) {
                notYetAgreed.add( eulaObj );
            }
        }
        return notYetAgreed;
    }
    
    /**
     * Get the list of EulaInfo for the given task, can be a module or pipeline.
     * When it is a pipeline, recursively get all required licenses.
     * 
     * @param taskInfo, must not be null
     * @return
     */
    private List<EulaInfo> getEulaInfosFromTaskInfo(final TaskInfo taskInfo) { 
        GetEulaFromTaskRecursive getEulaFromTask = new GetEulaFromTaskRecursive();
        GetEulaFromTask impl = getGetEulaFromTask();
        getEulaFromTask.setGetEulaFromTask(impl);
        getEulaFromTask.setGetTaskStrategy(getTaskStrategy);
        SortedSet<EulaInfo> eulaObjs = getEulaFromTask.getEulasFromTask(taskInfo);
        //return the sortedset as a list
        List<EulaInfo> list = new ArrayList<EulaInfo>(eulaObjs);
        return list;
    }
    
}
