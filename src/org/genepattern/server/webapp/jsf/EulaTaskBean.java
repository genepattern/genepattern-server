package org.genepattern.server.webapp.jsf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.licensemanager.EulaInfo;
import org.genepattern.server.licensemanager.LicenseManager;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.TaskInfo;

/**
 * Request scope JSF bean for handling End-user license agreement(s) before running a particular module. 
 * 
 * The GUI must prompt a GP User before going to the job submit form.
 * 
 * @author pcarr
 */
public class EulaTaskBean {
    private static Logger log = Logger.getLogger(EulaTaskBean.class);
    
    /**
     * Use this class as an intermediary between the license manager and the JSF gui.
     * 
     * @author pcarr
     */
    static public class EulaInfoBean {
        static EulaInfoBean from(EulaInfo eulaInfoObj) {
            EulaInfoBean eulaInfo = new EulaInfoBean();
            eulaInfo.setLsid(eulaInfoObj.getModuleLsid());
            eulaInfo.setTaskName(eulaInfoObj.getModuleName());
            eulaInfo.setContent(eulaInfoObj.getContent());
            eulaInfo.setLink(eulaInfoObj.getLink());
            return eulaInfo;
        }

        private String lsid;
        private String taskName;
        private String content;
        private String link;

        public String getLsid() {
            return lsid;
        }
        public void setLsid(String lsid) {
            this.lsid = lsid;
        }

        public String getTaskName() {
            return taskName;
        }

        public void setTaskName(String taskName) {
            this.taskName = taskName;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getLink() {
            return link;
        }

        public void setLink(String link) {
            this.link = link;
        }
    }
    
    //the current GP user
    private String currentUser=null;
    //the lsid of the job that the current user wants to run (can be a module or a pipeline)
    private String currentLsid=null;
    //instantiated, when necessary, from the currentLsid
    private TaskInfo currentTaskInfo=null;
    //if true, it means we need to display the EULA agreement form, otherwise skip ahead to the job submit form
    private boolean prompt=false;
    //the list of pending EULA, those which the current user must agree to before running the module or pipeline 
    private List<EulaInfoBean> eulas=null;
    //internal helper variable (to deal with JSF lifecycle), sometimes prompt is true AND accepted is true
    //    which means, don't prompt again
    private boolean accepted=false;
    //helper method, needed to restore the 'reloadJob=<jobId>' request parameter,
    //    when we have to prompt for EULA before reloading a job
    private String reloadJobParam="";

    public EulaTaskBean() {
        this.currentUser = UIBeanHelper.getUserId();
    }
    
    //callback from ModuleChooserBean#setSelectedModule, JobBean#reload
    //  #{eulaTaskBean.initialQueryString}
    public void setCurrentLsid(final String currentLsid) {
        log.debug("currentLsid="+currentLsid);
        this.currentLsid=currentLsid; 
        if (currentLsid != null && currentLsid.length() != 0) {
            //if necessary, init currentTaskInfo
            if (currentTaskInfo == null || !currentLsid.equals( currentTaskInfo.getLsid() )) {
                log.debug("initializing currentTaskInfo, currentLsid="+currentLsid);
                currentTaskInfo = initTaskInfo(currentUser, currentLsid);
            }
        }
        if (this.currentTaskInfo!=null) {
            this.prompt=initEulaInfo(currentTaskInfo);
        }
        else {
            this.prompt=false;
        }
    }
    
    /**
     * Get the lsid for the module (or pipeline) that the current user wants to run.
     * This may or may not be the same lsid as that which requires EULA.
     * 
     * We use this to display the header information.
     * 
     * @return
     */
    public String getCurrentLsid() {
        return currentLsid;
    }
    
    /**
     * Check to see if we need to prompt the currentUser for an EULA for the current module.
     * Note: this covers the following cases:
     *     1) module has no EULA (return false)
     *     2) module has one or more EULA, but current user has already agreed to all of them (return false)
     *     3) module has one or more EULA which the current user has not yet agreed to (return true)
     * @return true, if the GUI should prompt the current user to accept one or more EULA.
     */
    public boolean isPrompt() {
        Object obj = UIBeanHelper.getRequest().getSession().getAttribute(GPConstants.LSID);
        if (obj instanceof String) {
            String taskNameOrLsid = (String) obj;
            if (taskNameOrLsid != currentLsid) {
                setCurrentLsid(taskNameOrLsid);
            }
        }

        if (prompt) {
            return !accepted;
        }
        return prompt;
    }

    /**
     * Get the list of pending End-user license agreements that the current user must agree to 
     * before they can run the current module.
     * 
     * @return
     */
    public List<EulaInfoBean> getEulas() {
        if (eulas==null) {
            return Collections.emptyList();
        }
        return eulas;
    }

    //helper method, when reloading a job from a URL, e.g.
    //    /gp/pages/index.jsf?lsid=urn:lsid:8080.gp-trunk-dev.120.0.0.1:genepatternmodules:303:2&reloadJob=7767
    public String getInitialQueryString() {
        HttpServletRequest request = UIBeanHelper.getRequest();
        String qs = request.getQueryString();
        if (qs == null) {
            qs="";
        }
        log.debug("queryString="+qs);
        return qs;
    }
    
    //helper method, when reloading a job from the job menu
    public void setReloadJobParam(final String str) {
        log.debug("reloadJobParam="+str);
        this.reloadJobParam=str;
    }
    
    public String getReloadJobParam() {
        return reloadJobParam;
    }

    private static TaskInfo initTaskInfo(final String currentUser, final String lsid) {
        //TODO: this code is duplicated in RunTaskBean, 
        //      should find a way to share the same instance of the TaskInfo per page request
        TaskInfo taskInfo = null;
        if (lsid != null && lsid.length()>0) {
            try {
                final LocalAdminClient lac = new LocalAdminClient(currentUser);
                taskInfo = lac.getTask(lsid);
            }
            catch (Throwable t) {
                log.error("Error initializing taskInfo for lsid=" + lsid, t);
            }
        } 
        return taskInfo;
    }

    /**
     * @see #prompt for documentation.
     */
    private boolean initEulaInfo(final TaskInfo taskInfo) {
        log.debug("initializing EULA info for userId="+currentUser+", lsid="+currentLsid);
        boolean requiresEULA = false; 
        if (taskInfo != null) {
            Context taskContext = Context.getContextForUser(currentUser);
            taskContext.setTaskInfo(taskInfo);
            List<EulaInfo> promptForEulas = LicenseManager.instance().getPendingEULAForModule(taskContext);
            if (promptForEulas == null || promptForEulas.size()==0) {
                requiresEULA=false;
                this.eulas=Collections.emptyList();
            }
            else {
                requiresEULA=true;
                if (eulas==null) {
                    eulas=new ArrayList<EulaInfoBean>();
                }
                else {
                    eulas.clear();
                }
                for(EulaInfo eulaInfoObj : promptForEulas) {
                    EulaInfoBean eulaInfoBean = EulaInfoBean.from(eulaInfoObj);
                    eulas.add(eulaInfoBean);
                }
            }
        }
        
        log.debug("requiresEULA="+requiresEULA);
        return requiresEULA;
    }

    /**
     * When the 'OK' button is clicked to accept the EULA.
     * The lsid is set with a hidden input parameter in a standard web form.
     * <pre>
       <input type="hidden" name="lsid" value="#{eulaTaskBean.lsid}" />
     * </pre>
     * @param event
     */
    public void acceptOk(ActionEvent event) {
        log.debug("acceptOk");
        Map<String,String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
        final String lsid=params.get("lsid"); 
        recordEULA(currentUser, lsid);
    }

    /**
     * When the 'OK' link is clicked to accept the EULA.
     * The lsid is set with a f:param nested in the h:commandLink tag.
     * <pre>
       <f:param name="lsid" value="#{eulaTaskBean.lsid}"/>
     * </pre>
     * @param event
     */
    public void acceptClicked(ActionEvent event) {
        log.debug("acceptClicked");
        HttpServletRequest request = UIBeanHelper.getRequest();
        String lsid = request.getParameter("lsid");
        recordEULA(currentUser, lsid);
    }
    
    //use the LicenseManager to record EULA
    private void recordEULA(final String _userId, final String _lsid) {
        log.debug("recordEULA, userId="+_userId+", lsid="+_lsid);
        Context taskContext=Context.getContextForUser(_userId);
        TaskInfo taskInfo = null;
        taskInfo = initTaskInfo(_userId, _lsid);
        taskContext.setTaskInfo(taskInfo);
        LicenseManager.instance().recordLicenseAgreement(taskContext);
        accepted=true;
    }
}
