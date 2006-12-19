/*******************************************************************************
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright (2003-2006) by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are
 * reserved.
 *  
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 *  
 *******************************************************************************/
package org.genepattern.server.webapp.jsf;

public class AboutBean {
    private String full;

    public AboutBean() {
        int major = (int) Long.parseLong(System.getProperty("version.major"));
        String minor = System.getProperty("version.minor");
        int revision = (int) Long.parseLong(System
                .getProperty("version.revision"));
        String release = System.getProperty("release");
        final String rev = (revision > 0) ? "." + revision : "";
        this.full = major + "." + minor + rev + release;
    }

    public String getBuildTag() {
        return System.getProperty("build.tag");
    }

    public String getDate() {
        return System.getProperty("date");
    }

    public String getFull() {
        return full;
    }
}
