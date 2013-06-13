package org.genepattern.server.repository;

import java.net.URL;

import org.genepattern.server.config.ServerConfiguration.Context;

/**
 * Java Bean representation of a GenePattern Module Repository.
 * 
 * @author pcarr
 */
public class RepositoryInfo {
    final static public String PROP_MODULE_REPOSITORY_URL="ModuleRepositoryURL";
    final static public String PROP_MODULE_REPOSITORY_URLS="ModuleRepositoryURLs";

    
    /**
     * RepositoryInfoFactory implementation
     */
    final static public RepositoryInfoLoader getRepositoryInfoLoader(final Context userContext) {
        return new DefaultRepositoryInfoLoader(userContext);
    }
    
    private String label="";
    final private URL url;
    private String description="";
    private String iconImgSrc="images/broad-symbol.gif";
    
    public RepositoryInfo(final URL url) {
        this(url.toExternalForm(), url);
    }
    
    public RepositoryInfo(final String label, final URL url) {
        this.label=label;
        this.url=url;
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

    public String getDescription() {
        return description;
    }
    public void setDescription(final String description) {
        this.description=description;
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
