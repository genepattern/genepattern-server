package org.genepattern.server.licensemanager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
     * Implement a run-time check, before starting a job, verify that the current user
     * has permission to run the job.
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
    public List<EULAInfo> getAllLicensesForModule(final Context taskContext) {
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
    public List<EULAInfo> getLicensesWhichRequireAgreement(final Context taskContext) {
        return getEULAs(taskContext, false);
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
        //TODO: implement this method
        if ("admin".equals(userId)) {
            //for debugging only ...
            return true;
        }
        return false;
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
}
