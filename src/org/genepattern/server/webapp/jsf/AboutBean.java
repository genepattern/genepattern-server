/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webapp.jsf;

import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;

/**
 * Display information about this GenePattern Server by reading System properties loaded from the build.properties file.
 * 
 * @author pcarr
 */
public class AboutBean { 
    private final GpConfig gpConfig;
    private final GpContext serverContext;
    
    private String full;
    
    //mapped from build.properties file
    private String genepatternVersion = "";
    private String versionLabel = "";
    private String versionRevision = "";
    private String versionBuildDate = "";

    public AboutBean() {
        this(ServerConfigurationFactory.instance(), GpContext.getServerContext());
    }

    public AboutBean(GpConfig gpConfig, GpContext serverContext) {
        this.gpConfig=gpConfig;
        this.serverContext=serverContext;
        
        this.genepatternVersion = gpConfig.getGenePatternVersion();
        this.versionLabel =  gpConfig.getGPProperty(serverContext, GpConfig.PROP_VERSION_LABEL, "");
        this.versionRevision = gpConfig.getGPProperty(serverContext, GpConfig.PROP_VERSION_REVISION_ID, "");
        this.versionBuildDate = gpConfig.getGPProperty(serverContext, GpConfig.PROP_VERSION_BUILD_DATE, "");
        
        this.full = genepatternVersion + " " + versionLabel;
        this.full = full.trim();
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
     * @return the java version on which the server is running, <code>System.getProperty("java.version")</code>
     */
    public String getJavaVersion() {
        String javaVersion = System.getProperty("java.version", "");
        return javaVersion;
    }

    public String getContactUs() {
        GpContext context = UIBeanHelper.getUserContext();
        String link = gpConfig.getGPProperty(context, "contact.link", "/gp/pages/contactUs.jsf");
        return link;
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
        boolean rval=gpConfig.getGPBooleanProperty(serverContext, GpConfig.PROP_GA_ENABLED, false);
        return rval;
    }

    public String getGoogleAnalyticsTrackingId() {
        return gpConfig.getGPProperty(serverContext, GpConfig.PROP_GA_TRACKING_ID, "");
    }

}
