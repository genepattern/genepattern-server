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

    public EulaTaskBean() {
        this.currentUser = UIBeanHelper.getUserId();
    }
    
    public void setSelectedModule(String selectedModule) {
        this.lsid=selectedModule;
        this.prompt=isRequiresEULA();
    }
    
    public void setLsid(final String lsid) {
        this.lsid = lsid;
    }
    
    public String getLsid() {
        return lsid;
    }
    
    /**
     * Check to see if we need to prompt for EULA.
     * @return true, if the GUI should prompt the current user to accept one or more EULA.
     */
    public boolean isPrompt() {
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

    private boolean isRequiresEULA() {
        boolean requiresEULA = false;
        
        final String lsid = getLsid();
        
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
    private static void recordEULA(final String currentUser, final String lsid) {
        Context taskContext=Context.getContextForUser(currentUser);
        TaskInfo taskInfo = null;
        taskInfo = initTaskInfo(currentUser, lsid);
        taskContext.setTaskInfo(taskInfo);
        LicenseManager.instance().recordLicenseAgreement(taskContext);
    }
}
