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
     * Get the list of licenses for the module or pipeline with the given lsid.
     * 
     * @param lsid
     * @return
     */
    private List<EULAObj> getEULAs(String lsid) {
        return Collections.emptyList();
    }

    /**
     * Implement a run-time check, before starting a job, verify that the current user
     * has permission to run the job.
     * 
     * @param jobContext
     * @return true if there is no record of EULA for the current user.
     */
    public boolean requiresEULA(Context jobContext) {
        if (jobContext == null) {
            throw new IllegalArgumentException("jobContext==null");
        }
        if (jobContext.getUserId()==null) {
            throw new IllegalArgumentException("userId==null");
        }
        if (jobContext.getUserId().length()==0) {
            throw new IllegalArgumentException("userId not set");
        }
    
        //check 1: if the module doesn't require a license, then we are done
        TaskInfo taskInfo = jobContext.getTaskInfo();
        if (taskInfo==null) {
            throw new IllegalArgumentException("taskInfo==null");
        }
        if (taskInfo.getLsid()==null || taskInfo.getLsid().length()==0) {
            throw new IllegalArgumentException("taskInfo not set");
        }
        List<EULAObj> eulaObjs = getEULAObjs(taskInfo);
        if (eulaObjs==null || eulaObjs.size()==0) {
            return false;
        }
    
        //check 2: if the module does require a license, make sure the current user has agreed to all EULAs
        List<EULAObj> notYetAgreed = new ArrayList<EULAObj>();
        for(EULAObj eulaObj : eulaObjs) {
            boolean hasAgreed = hasUserAgreed(jobContext.getUserId(), eulaObj);
            if (!hasAgreed) {
                notYetAgreed.add( eulaObj );
            }
        }
        if (notYetAgreed.size()>0) {
            return true;
        }
        return false;
    }
    
    private boolean hasUserAgreed(String userId, EULAObj eula) {
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
    private List<EULAObj> getEULAObjs(TaskInfo taskInfo) {
        if (taskInfo==null) {
            log.error("taskInfo==null");
            return Collections.emptyList();
        }

        Object licenseObj = taskInfo.getAttributes().get("license");
        if (licenseObj == null) {
            return Collections.emptyList();
        }
        String licenseStr;
        if (licenseObj instanceof String) {
            licenseStr = (String) licenseObj;
        }
        else {
            licenseStr = licenseObj.toString();
        }
        EULAObj eula = new EULAObj();
        eula.setModuleLsid(taskInfo.getLsid());
        eula.setLicense(licenseStr);
        List<EULAObj> eulas = new ArrayList<EULAObj>();
        eulas.add(eula);
        return eulas;
    }
    
    /**
     * Data representation of a single End-user license agreement 'form' for a module.
     * 
     * This object is needed in a number of different contexts.
     * 
     * 1) At runtime, to check to see if there is a local record of agreement for a particular user.
     *     For this we need some kind of unique ID for the license object.
     * 
     * 2) In the job submit form, to present to the end user the content of the license agreement.
     *     For this we need the full text of the agreement, or a link to where the full text lives.
     * 
     * @author pcarr
     *
     */
    class EULAObj {
        private String ID;
        private String link;
        private String content;

        //the lsid of the module which requires the EULA
        private String moduleLsid;
        //the value of the license= property in the manifest for the module
        private String license;
        
        public void setLicense(String license) {
            this.license=license;
        }
        public void setModuleLsid(String lsid) {
            this.moduleLsid=lsid;
        }
    }
}
