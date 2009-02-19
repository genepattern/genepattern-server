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

public class AboutBean {
    private String full;

    public AboutBean() {
        String major = System.getProperty("version.major");
        String minor = System.getProperty("version.minor");
        String revision = System.getProperty("version.revision");
        String release = System.getProperty("release");
        String rev = "";
        if (revision != null && !"".equals(revision.trim())) {
            try {
                //don't display the rev if it is a number less than zero
                int revNum = Integer.parseInt(revision);
                if (revNum > 0) {
                    rev = "." + revision;
                }
            }
            catch (NumberFormatException e) {
                rev = "." + revision;
            }
        }
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
