package org.genepattern.server.repository;

/**
 * Class representing the known quality info about a module
 * NOTE: Not currently implemented. This is just a card-coded stub.
 * @author tabor
 */
public class ModuleQualityInfo {
    public static final String PRODUCTION_REPOSITORY = "GenePattern";
    public static final String BETA_REPOSITORY = "Beta Modules";
    public static final String GPARC = "GParc";
    
    public static final String PRODUCTION = "Production";
    public static final String BETA = "Beta";
    public static final String DEVELOPMENT = "Development";
    
    // TODO: These values are hard-coded for now. 
    // When this class is implemented they should be changed.
    private String source = PRODUCTION_REPOSITORY;
    private String quality = BETA;
    
    public ModuleQualityInfo(String lsid) {
        // TODO: Implement the constructor
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getQuality() {
        return quality;
    }

    public void setQuality(String quality) {
        this.quality = quality;
    }
}
