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
    
    final static public String BROAD_PROD_URL="http://www.broadinstitute.org/webservices/gpModuleRepository";
    final static public String BROAD_BETA_URL="http://www.broadinstitute.org/webservices/betaModuleRepository";
    final static public String BROAD_DEV_URL="http://www.broadinstitute.org/webservices/gpModuleRepository?env=dev";
    //internal path to gparc, http://vgpprod01.broadinstitute.org:4542/gparcModuleRepository
    final static public String GPARC_URL="http://www.broadinstitute.org/webservices/gparcModuleRepository";
    
    final static public String DEFAULT_MODULE_REPOSITORY_URLS=
            BROAD_PROD_URL+","+GPARC_URL+","+BROAD_BETA_URL;
    
    /**
     * RepositoryInfoFactory implementation
     */
    final static public RepositoryInfoLoader getRepositoryInfoLoader(final GpContext userContext) {
        return new ConfigRepositoryInfoLoader(userContext);
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
