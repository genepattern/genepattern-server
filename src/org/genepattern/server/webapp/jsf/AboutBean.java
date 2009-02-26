/*******************************************************************************
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright (2003-2008) by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are
 * reserved.
 *  
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 *  
 *******************************************************************************/
package org.genepattern.server.webapp.jsf;

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

    public String getBuildTag() {
        return versionRevision;
    }

    public String getDate() {
        return versionBuildDate;
    }

    
    public String getFull() {
        return full;
    }
}
