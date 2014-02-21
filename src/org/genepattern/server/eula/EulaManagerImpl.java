package org.genepattern.server.eula;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpContext;
import org.genepattern.webservice.TaskInfo;

/**
 * Methods for managing End-user license agreements (EULA) for GenePattern modules.
 * 
 * @author pcarr
 */
public class EulaManagerImpl implements IEulaManager {
    final static private Logger log = Logger.getLogger(EulaManagerImpl.class);

    private GetEulaFromTask getEulaFromTask = null;
    private GetTaskStrategy getTaskStrategy = null;
    private RecordEula recordEulaStrategy = null;

    /**
     * Optionally set the strategy for getting the list (if any) of EULA
     * which are required for a particular module or pipeline.
     * 
     * @param impl, an object which implements the GetEulaFromTask interface, can be null.
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
        return new GetEulaAsManifestProperty();
        //option 2: support file named '*license*' in tasklib
        //return new GetEulaAsSupportFile();
    }
    
    /**
     * Optionally set the strategy for initializing a TaskInfo from a task lsid.
     * 
     * @param impl, an object which implements this interface, can be null.
     */
    public void setGetTaskStrategy(GetTaskStrategy impl) {
        this.getTaskStrategy=impl;
    }

    /**
     * Optionally set the strategy for recording user agreement to the local database.
     * 
     * @param impl, an object which implements the RecordEula interface, can be null.
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
        return new RecordEulaDefault();
    }
    
    /**
     * @see IEulaManager#requiresEula(org.genepattern.server.config.GpContext)
     */
    public boolean requiresEula(final GpContext taskContext) {
        final List<EulaInfo> notYetAgreed = getEulaInfos(taskContext,false);
        if (notYetAgreed.size()>0) {
            return true;
        }
        return false;
    }

    /**
     * @see IEulaManager#getEulas(TaskInfo)
     */
    public List<EulaInfo> getEulas(final TaskInfo taskInfo) {
        GetEulaFromTask impl = getGetEulaFromTask();
        return impl.getEulasFromTask(taskInfo);
    }

    /**
     * @see IEulaManager#setEula(EulaInfo, TaskInfo)
     */
    public void setEula(final EulaInfo eula, final TaskInfo taskInfo) {
        GetEulaFromTask impl = getGetEulaFromTask();
        impl.setEula(eula, taskInfo);
    }

    /**
     * @see IEulaManager#setEulas(List, TaskInfo)
     */
    public void setEulas(final List<EulaInfo> eulas, final TaskInfo taskInfo) {
        GetEulaFromTask impl = getGetEulaFromTask();
        impl.setEulas(eulas, taskInfo);
    }

    /**
     * @see IEulaManager#getAllEulaForModule(org.genepattern.server.config.GpContext)
     */
    public List<EulaInfo> getAllEulaForModule(final GpContext taskContext) {
        List<EulaInfo> eulaInfos = getEulaInfos(taskContext, true);
        return eulaInfos;
    }

    /**
     * @see org.genepattern.server.eula.IEulaManager#getPendingEulaForModule(org.genepattern.server.config.GpContext)
     */
    public List<EulaInfo> getPendingEulaForModule(final GpContext taskContext) {
        List<EulaInfo> eulaInfos = getEulaInfos(taskContext, false);
        return eulaInfos;
    }
    
    /**
     * @see org.genepattern.server.eula.IEulaManager#recordEula(org.genepattern.server.config.GpContext)
     */
    public void recordEula(final GpContext taskContext) throws IllegalArgumentException {
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
                recordEula.recordLicenseAgreement(taskContext.getUserId(), eulaInfo);
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
    private List<EulaInfo> getEulaInfos(final GpContext taskContext, final boolean includeAll) {
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
