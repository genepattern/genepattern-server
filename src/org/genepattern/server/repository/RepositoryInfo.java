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

}
