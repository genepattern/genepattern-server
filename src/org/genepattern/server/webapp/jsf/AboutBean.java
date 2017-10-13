/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webapp.jsf;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;

/**
 * Display information about this GenePattern Server.
 * 
 * @author pcarr
 */
public class AboutBean { 
    private static final Logger log = Logger.getLogger(AboutBean.class);

    private String full;
    
    //mapped from build.properties file
    private String genepatternVersion = "";
    private String versionLabel = "";
    private String versionRevision = "";
    private String versionBuildDate = "";
    
    private final String contactUs;
    private final boolean gaEnabled;
    private final String gaTrackingId;

    public AboutBean() {
        this(ServerConfigurationFactory.instance(), UIBeanHelper.getUserContext());
    }

    public AboutBean(GpConfig gpConfig, GpContext userContext) {
        if (gpConfig==null) {
            throw new IllegalArgumentException("gpConfig==null");
        }
        if (userContext==null) {
            log.warn("userContext==null");
            userContext=UIBeanHelper.getUserContext();
        }
        
        this.genepatternVersion = gpConfig.getGenePatternVersion();
        this.versionLabel =  gpConfig.getBuildProperty(GpConfig.PROP_VERSION_LABEL, "");
        this.versionRevision = gpConfig.getBuildProperty(GpConfig.PROP_VERSION_REVISION_ID, "");
        this.versionBuildDate = gpConfig.getBuildProperty(GpConfig.PROP_VERSION_BUILD_DATE, "");
        
        this.full = genepatternVersion + " " + versionLabel;
        this.full = full.trim();
        
        this.contactUs = gpConfig.getGPProperty(userContext, GpConfig.PROP_CONTACT_LINK, GpConfig.DEFAULT_CONTACT_LINK);
        this.gaEnabled = gpConfig.getGPBooleanProperty(userContext, GpConfig.PROP_GA_ENABLED, false);
        this.gaTrackingId = gpConfig.getGPProperty(userContext, GpConfig.PROP_GA_TRACKING_ID, "");
    }
    
    public String getFull() {
        return full;
    }

    /**
     * @return the genepattern.version from the build.properties file.
     */
    public String getGenePatternVersion() {
        return this.genepatternVersion;
    }
    
    /**
     * @return the version.label from the build.properties file.
     */
    public String getVersionLabel() {
        return this.versionLabel;
    }

    /**
     * @return the version.revision.id from the build.properties file.
     */
    public String getBuildTag() {
        return versionRevision;
    }    

    /**
     * @return the version.build.date from the build.properties file.
     */
    public String getDate() {
        return versionBuildDate;
    }

    /**
     * @return the java version on which the server is running, system property "java.version"
     */
    public String getJavaVersion() {
        String javaVersion = GpConfig.getJavaProperty("java.version", "");
        return javaVersion;
    }

    public String getContactUs() {
        return contactUs;
    }
    
    /**
     * Configurable Google Analytics so that we don't have to manually edit the index.xhtml file after each GP server update.
     * Documentation is in the GpConfig file and in the config_default.yaml file.
     * Example config_yaml entry:
     * <pre>
    googleAnalytics.enabled: true
    googleAnalytics.trackingId: "UA-****9150-1"  <---- not a real value
     * </pre>
     * 
     * When enabled, the ./pages/gaTracking.xhtml file is included in the head of the index.xhtml page.
     * You can make edits to that file directly to customize.
     * @return
     */
    public boolean isGoogleAnalyticsEnabled() {
        return gaEnabled;
    }

    public String getGoogleAnalyticsTrackingId() {
        return gaTrackingId;
    }

}
