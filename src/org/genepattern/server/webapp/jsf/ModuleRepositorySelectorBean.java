/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webapp.jsf;

import java.util.ArrayList;
import java.util.List;

import javax.faces.model.SelectItem;

import org.genepattern.server.config.GpContext;
import org.genepattern.server.repository.RepositoryInfo;
import org.genepattern.server.repository.RepositoryInfoLoader;

/**
 * JSF backing bean for selecting the module repository from a GP server configured list of repositories.
 * 
 * @author pcarr
 *
 */
public class ModuleRepositorySelectorBean {
    final private RepositoryInfoLoader loader;
    final private List<SelectItem> menuItems;
    private RepositoryInfo currentRepository;
    
    public ModuleRepositorySelectorBean() {
        final String userId=UIBeanHelper.getUserId();
        final GpContext userContext=GpContext.getContextForUser(userId);
        loader=RepositoryInfo.getRepositoryInfoLoader(userContext);
        currentRepository=loader.getCurrentRepository();
        this.menuItems=new ArrayList<SelectItem>(); 
        final List<RepositoryInfo> infos=loader.getRepositories();
        for(final RepositoryInfo info : infos) {
            final String value=info.getUrl().toExternalForm();
            final String label=info.getLabel();
            final SelectItem selectItem=new SelectItem(value, label);
            menuItems.add(selectItem);
        }
    }
    
    /**
     * In response to selecting a repository from the menu.
     * @param url
     */
    synchronized public void setUrl(final String url) {
        loader.setCurrentRepository(url);
        this.currentRepository=loader.getRepository(url);
    }

    public String getUrl() {
        return currentRepository.getUrl().toExternalForm();
    }
    
    public String getLabel() {
        return currentRepository.getLabel();
    }

    public String getBriefDescription() {
        return currentRepository.getBriefDescription();
    }

    public String getFullDescription() {
        return currentRepository.getFullDescription();
    }

    public String getIconImgSrc() {
        return currentRepository.getIconImgSrc();
    }

    public List<SelectItem> getMenuItems() {
        return menuItems;
    }

    public RepositoryInfo getCurrentRepository() {
        return currentRepository;
    }

}
