package org.genepattern.server.webapp.jsf;

import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.licensemanager.EULAInfo;
import org.genepattern.server.licensemanager.LicenseManager;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.TaskInfo;

/**
 * Request scope JSF bean for handling End-user license agreement(s) before running a particular module. 
 * 
 * @author pcarr
 */
public class EulaTaskBean {
    private static Logger log = Logger.getLogger(EulaTaskBean.class);
    
    private String currentUser=null;
    private String lsid;
    private boolean prompt=false;
    private boolean accepted=false;
    private String reloadJobParam="";

    public EulaTaskBean() {
        this.currentUser = UIBeanHelper.getUserId();
    }
    
    //callback from ModuleChooserBean#setSelectedModule, JobBean#reload
    public void setSelectedModule(String selectedModule) {
        log.debug("selectedModuel="+selectedModule);
        this.lsid=selectedModule;
        this.prompt=isRequiresEULA();
    }
    
    public String getLsid() {
        return lsid;
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
    public String getReloadJobParam() {
        return reloadJobParam;
    }
    
    public void setReloadJobParam(final String str) {
        log.debug("reloadJobParam="+str);
        this.reloadJobParam=str;
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
            if (taskNameOrLsid != lsid) {
                setSelectedModule(taskNameOrLsid);
            }
        }

        if (prompt) {
            return !accepted;
        }
        return prompt;
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
    private boolean isRequiresEULA() {
        log.debug("checking for EULA, userId="+currentUser+", lsid="+lsid);
        boolean requiresEULA = false; 
        TaskInfo taskInfo = null;
        taskInfo = initTaskInfo(currentUser, lsid);
        if (taskInfo != null) {
            Context taskContext = Context.getContextForUser(currentUser);
            taskContext.setTaskInfo(taskInfo);
            List<EULAInfo> promptForEulas = LicenseManager.instance().getPendingEULAForModule(taskContext);
            if (promptForEulas == null || promptForEulas.size()==0) {
                requiresEULA=false;
            }
            else {
                requiresEULA=true;
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
