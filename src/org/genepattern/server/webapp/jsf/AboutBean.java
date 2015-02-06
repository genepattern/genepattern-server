/*******************************************************************************
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright (2003-2011) by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are
 * reserved.
 *  
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 *  
 *******************************************************************************/
package org.genepattern.server.webapp.jsf;

import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;

/**
 * Display information about this GenePattern Server by reading System properties loaded from the build.properties file.
 * 
 * @author pcarr
 */
public class AboutBean {
    private String full;
    
    //mapped from build.properties file
    private String genepatternVersion = "";
    private String versionLabel = "";
    private String versionRevision = "";
    private String versionBuildDate = "";

    public AboutBean() {
        this.genepatternVersion = System.getProperty("genepattern.version", "unknown");
        this.versionLabel = System.getProperty("version.label", "");
        this.versionRevision = System.getProperty("version.revision.id", "");
        this.versionBuildDate = System.getProperty("version.build.date", "");
        
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
        String javaVersion = System.getProperty("java.version");
        if (javaVersion == null) {
            javaVersion = "";
        }
        return javaVersion;
    }

    public String getContactUs() {
        GpContext context = UIBeanHelper.getUserContext();
        String link = ServerConfigurationFactory.instance().getGPProperty(context, "contact.link", "/gp/pages/contactUs.jsf");
        return link;
    }

}
