/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.repository;

import java.net.URL;

import org.genepattern.server.config.GpContext;

/**
 * Java Bean representation of a GenePattern Module Repository.
 * 
 * @author pcarr
 */
public class RepositoryInfo {
    final static public String PROP_MODULE_REPOSITORY_URL="ModuleRepositoryURL";
    final static public String PROP_MODULE_REPOSITORY_URLS="ModuleRepositoryURLs";
    
    final static public String GP_PROD_URL="https://modulerepository.genepattern.org/gpModuleRepository/";
    final static public String GP_DEV_URL="https://modulerepository.genepattern.org/gpModuleRepository/?env=dev";
    final static public String GP_BETA_URL="https://modulerepository.genepattern.org/betaModuleRepository/";
    //internal path to gparc, http://vgpprod01.broadinstitute.org:4542/gparcModuleRepository
    final static public String GPARC_URL="https://modulerepository.genepattern.org/gparcModuleRepository/";
    
    final static public String DEFAULT_MODULE_REPOSITORY_URLS=
            GP_PROD_URL+","+GPARC_URL;
    
    /**
     * RepositoryInfoFactory implementation
     */
    final static public RepositoryInfoLoader getRepositoryInfoLoader(final GpContext userContext) {
        return new ConfigRepositoryInfoLoader(userContext);
    }
    
    /**
     * Is the module installed from the old Broad production module repository.
     * Handles the special-case for modules which were installed before the 
     * module repository changed from the old 'www' url to the new 'software' url.
     * 
     * @param repoUrl, the REPO_URL from the TASK_INSTALL table
     * @return true if the repoUrl matches the old or new prod repo
     */
    public static final boolean isGPProdUrl(final String repoUrl) {
        return repoUrl.equalsIgnoreCase(GP_PROD_URL) ||
                repoUrl.equalsIgnoreCase("http://software.broadinstitute.org/webservices/gpModuleRepository");
    }
    
    private String label="";
    final private URL url;
    private String briefDescription="";
    private String fullDescription="";

    private String iconImgSrc=null;
    
    public RepositoryInfo(final URL url) {
        this(null, url);
    }
    
    public RepositoryInfo(final String label, final URL url) {
        if (url==null) {
            throw new IllegalArgumentException("Invalid null arg, url==null");
        }
        this.url=url;
        
        if (label==null) {
            this.label=url.toExternalForm();
        }
        else {
            this.label=label;
        }
    }
    
    public URL getUrl() {
        return url;
    }

    public String getLabel() {
        return label;
    }
    public void setLabel(final String label) {
        this.label=label;
    }

    public String getBriefDescription() {
        return briefDescription;
    }
    public void setBriefDescription(final String description) {
        this.briefDescription=description;
    }

    public String getFullDescription() {
        return fullDescription;
    }
    public void setFullDescription(final String description) {
        this.fullDescription=description;
    }

    /**
     * Get the optional uri to an icon representing the repository.
     * If it is a relative path, it must be relative to the GenePatternURL,
     * e.g. "images/broad-symbol.gif"
     *      
     * 
     * @return null if there is no icon for the repository.
     */
    public String getIconImgSrc() {
        return iconImgSrc;
    }

    public void setIconImgSrc(final String imgSrc) {
        this.iconImgSrc=imgSrc;
    }

}
