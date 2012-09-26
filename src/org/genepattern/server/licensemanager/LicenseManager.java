package org.genepattern.server.licensemanager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.webservice.TaskInfo;

public class LicenseManager {
    public static Logger log = Logger.getLogger(LicenseManager.class);

    static public class Singleton {
        private static final LicenseManager INSTANCE = new LicenseManager();
        public static LicenseManager instance() {
            return INSTANCE;
        }
    }
    static public LicenseManager instance() {
        return Singleton.INSTANCE;
    }
    
    /**
     * Implement a run-time check, before starting a job, verify that there are
     * no EULA which the current user has not yet agreed to.
     * 
     * Returns true, if,
     *     1) the module requires no EULAs, or
     *     2) the current user has agreed to all EULAs for the module.
     * 
     * @param jobContext
     * @return true if there is no record of EULA for the current user.
     */
    public boolean requiresEULA(Context jobContext) {
        final List<EULAInfo> notYetAgreed = getEULAs(jobContext,false);
        if (notYetAgreed.size()>0) {
            return true;
        }
        return false;
    }
    
    /**
     * Get the list of all End-user license agreements for the given module or pipeline.
     * This list includes all EULA, even if the current user has already agreed to them.
     * 
     * @param taskContext, must have a valid taskInfo object
     * @return
     */
    public List<EULAInfo> getAllEULAForModule(final Context taskContext) {
        return getEULAs(taskContext, true);
    }

    /**
     * Get the list of End-user license agreements for the given module or pipeline, for
     * which to prompt for agreement from the current user.
     * 
     * Use this list as the basis for prompting the current user for agreement before
     * going to the job submit form for the task.
     * 
     * @param taskContext
     * @return
     */
    public List<EULAInfo> getPendingEULAForModule(final Context taskContext) {
        return getEULAs(taskContext, false);
    }
    
    /**
     * Record current user agreement to the End-user license agreement.
     * 
     *     TODO: when the taskInfo is a pipeline, make sure to accept all agreements.
     * 
     * @param taskContext, must have a valid taskInfo and userId
     */
    public void recordLicenseAgreement(final Context taskContext) {
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

        //TODO: implement this method
        log.debug("recording EULA for user="+taskContext.getUserId()+", lsid="+taskContext.getTaskInfo().getLsid());
        log.error("Not yet implemented!");
        recordLicenseAgreement_impl(taskContext.getUserId(), taskContext.getTaskInfo().getLsid());
    }

    /**
     * Get the list of required End-user license agreements for the given module or pipeline.
     * 
     * @param taskContext
     * @param includeAll
     * @return
     */
    private List<EULAInfo> getEULAs(final Context taskContext, final boolean includeAll) {
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
        List<EULAInfo> eulaObjs = getEULAObjs(taskInfo);
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
        List<EULAInfo> notYetAgreed = new ArrayList<EULAInfo>();
        for(EULAInfo eulaObj : eulaObjs) {
            boolean hasAgreed = hasUserAgreed(taskContext.getUserId(), eulaObj);
            if (!hasAgreed) {
                notYetAgreed.add( eulaObj );
            }
        }
        return notYetAgreed;
    }

    private boolean hasUserAgreed(String userId, EULAInfo eula) {
        if (userId == null) {
            throw new IllegalArgumentException("userId==null");
        }
        if (userId.length()==0) {
            throw new IllegalArgumentException("userId is not set");
        }
        if (eula == null) {
            throw new IllegalArgumentException("eula==null");
        }
        
        //TODO: implement the method, properly, current using stub which records to the current session
        //      all agreement info is lost on server restart
        return hasUserAgreed_impl(userId, eula);
        
        //old stub, delete this
        //TODO: implement this method
        //if ("admin".equals(userId)) {
        //    //for debugging only ...
        //    return true;
        //}
        //return false;
    }
    
    /**
     * Implement the rule representing a module which requires an end-user license agreement.
     * @param taskInfo
     * @return
     */
    private List<EULAInfo> getEULAObjs(TaskInfo taskInfo) {
        if (taskInfo==null) {
            log.error("taskInfo==null");
            return Collections.emptyList();
        }

        List<EULAInfo> eulaObjs = new ArrayList<EULAInfo>();
        Object licenseObj = taskInfo.getAttributes().get("license");
        if (licenseObj != null) {
            String licenseStr;
            if (licenseObj instanceof String) {
                licenseStr = (String) licenseObj;
            }
            else {
                licenseStr = licenseObj.toString();
            }
            EULAInfo eula = new EULAInfo();
            eula.setModuleLsid(taskInfo.getLsid());
            eula.setLicense(licenseStr);
            eulaObjs.add(eula);
        }
        
        if (taskInfo.isPipeline()) {
            //TODO: implement for pipelines
            log.error("This method is not yet implemented for pipelines");
        }
        return eulaObjs;
    }
    
    
    //------ the following is prototype code, should be re-factored into a factory pattern before releasing
    // 1st draft, record agreement to local session only
    //     Each user will have to accept the EULA after a server restart
    //    Note: this is not thread-safe.
    
    Set<String> acceptedEulas = new HashSet<String>();
    private void recordLicenseAgreement_impl(String userId, String lsid) {
        String uniq_key = lsid+"_"+userId;
        acceptedEulas.add(uniq_key);
    }
    private boolean hasUserAgreed_impl(String userId, EULAInfo eula) {
        String lsid=eula.getModuleLsid();
        String uniq_key = lsid+"_"+userId;
        return acceptedEulas.contains(uniq_key);
    }
}
