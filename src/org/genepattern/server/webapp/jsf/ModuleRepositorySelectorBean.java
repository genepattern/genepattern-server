package org.genepattern.server.webapp.jsf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.faces.model.SelectItem;

import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.executor.CommandProperties.Value;

/**
 * JSF backing bean for the selected module repostiory url for the given user.
 * @author pcarr
 *
 */
public class ModuleRepositorySelectorBean {
    private Context userContext;
    private String moduleRepositoryUrl;
    
    public ModuleRepositorySelectorBean() {
        setUserId(UIBeanHelper.getUserId());
        initModuleRepositoryUrl();
    }
    
    public void setUserId(final String userId) {
        userContext=ServerConfiguration.Context.getContextForUser(userId);
        //TODO: need to set the admin flag
    }
    
    private void initModuleRepositoryUrl() {
        final String url=getModuleRepositoryUrlFromProps();
        this.moduleRepositoryUrl=url;
    }
    
    public void setModuleRepositoryUrl(final String url) {
        //from a choice selection
        this.moduleRepositoryUrl=url;
        
        //TODO: must save this to the DB or else the menu will appear to have no effect
        saveModuleRepositoryUrl(userContext, moduleRepositoryUrl);
    }
    
    public String getModuleRepositoryUrl() {
        return moduleRepositoryUrl;
    }

    public List<SelectItem> getModuleRepositoryUrlMenuItems() {
        final List<SelectItem> selectItems=new ArrayList<SelectItem>();
        final List<String> values=getModuleRepositoryUrls();
        for(final String value : values) {
            final SelectItem selectItem=new SelectItem(value, value);
            selectItems.add(selectItem);
        }
        return selectItems;
    }

    // ----- JSF agnostic helper methods 
    final static private String PROP_MODULE_REPOSITORY_URL="ModuleRepositoryURL";
    final static private String PROP_MODULE_REPOSITORY_URLS="ModuleRepositoryURLs";

    /**
     * Get the currently selected module repository url.
     * @return
     */
    private String getModuleRepositoryUrlFromProps() {
        final Value urlValue=ServerConfiguration.instance().getValue(userContext, PROP_MODULE_REPOSITORY_URL);
        if (urlValue == null) {
            //TODO: log error
            return "";
        }
        return urlValue.getValue();
    }
    
    private void saveModuleRepositoryUrl(final Context userContext, final String url) {
        //initial implementation matches current (<= 3.6.0 functionality) by saving as a global system property
        System.setProperty(PROP_MODULE_REPOSITORY_URL, url);
    }
    
    private List<String> getModuleRepositoryUrls() { 
        final Value urls_value=ServerConfiguration.instance().getValue(userContext, PROP_MODULE_REPOSITORY_URLS);
        if (urls_value==null) {
            return Collections.emptyList();
        }
        else if (urls_value.getNumValues()==0) {
            return Collections.emptyList();
        }
        else if (!urls_value.isFromCollection()) {
            //the old way, a comma-separated list
            final List<String> moduleRepositoryUrls=new ArrayList<String>();
            final String[] splits=urls_value.getValue().split(",");
            if (splits != null) {
                for(final String str : splits) {
                    moduleRepositoryUrls.add(str);
                }
            }
            return moduleRepositoryUrls;
        }
        else {
            //the new way, a list of values
            final List<String> moduleRepositoryUrls=new ArrayList<String>();
            for(final String str : urls_value.getValues()) {
                moduleRepositoryUrls.add(str);
            }
            return moduleRepositoryUrls;
        }
    }

}
