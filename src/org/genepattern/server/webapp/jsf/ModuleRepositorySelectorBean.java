package org.genepattern.server.webapp.jsf;

import java.util.ArrayList;
import java.util.List;

import javax.faces.model.SelectItem;

import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.repository.RepositoryInfo;
import org.genepattern.server.repository.RepositoryInfoLoader;

/**
 * JSF backing bean for selecting the module repository from a GP server configured list of repositories.
 * 
 * @author pcarr
 *
 */
public class ModuleRepositorySelectorBean {
    private Context userContext;
    private RepositoryInfoLoader loader;
    private RepositoryInfo currentRepository;
    
    public ModuleRepositorySelectorBean() {
        setUserId(UIBeanHelper.getUserId());
        init();
    }
    
    public void setUserId(final String userId) {
        userContext=ServerConfiguration.Context.getContextForUser(userId);
        loader=RepositoryInfo.getRepositoryInfoLoader(userContext);
    }

    private void init() {
        currentRepository=getCurrentRepository();
    }
    
    /**
     * In response to selecting a repository from the menu.
     * @param url
     */
    public void setUrl(final String url) {
        //TODO: must save this to the DB or else the menu will appear to have no effect
        //initial implementation matches current (<= 3.6.0 functionality) by saving as a global system property
        System.setProperty(RepositoryInfo.PROP_MODULE_REPOSITORY_URL, url);
        init();
    }

    public String getUrl() {
        return currentRepository.getUrl().toExternalForm();
    }
    
    public String getLabel() {
        return currentRepository.getLabel();
    }

    public String getDescription() {
        return currentRepository.getDescription();
    }

    public List<SelectItem> getMenuItems() {
        final List<SelectItem> selectItems=new ArrayList<SelectItem>(); 
        final List<RepositoryInfo> infos=getModuleRepositoryInfos();
        for(final RepositoryInfo info : infos) {
            final String value=info.getUrl().toExternalForm();
            final String label=info.getLabel();
            final SelectItem selectItem=new SelectItem(value, label);
            selectItems.add(selectItem);
        }
        return selectItems;
    }

    // ----- JSF agnostic helper methods 
    private RepositoryInfo getCurrentRepository() {
        return loader.getCurrentRepository();
    }

    private List<RepositoryInfo> getModuleRepositoryInfos() {
        RepositoryInfoLoader loader=RepositoryInfo.getRepositoryInfoLoader(userContext);
        return loader.getRepositories();
    }

}
