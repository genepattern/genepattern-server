/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2011) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.
 
 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

/**
 *
 */
package org.genepattern.server.webapp.jsf;

import static org.genepattern.server.webapp.jsf.UIBeanHelper.getRequest;

import javax.faces.event.ActionEvent;

public class MySettingsBean {

    private String[] modes;

    private String currentMode;

    /**
     *
     */
    public MySettingsBean() {
        String prop = System.getProperty("require.password", "false").toLowerCase();
        boolean passwordRequired = (prop.equals("true") || prop.equals("y") || prop.equals("yes"));
        modes = passwordRequired ? new String[] { "Change Email", "Change Password", "History", "Visualizer Memory" }
                : new String[] { "Change Email", "History", "Visualizer Memory" };
        currentMode = modes[0]; // Default
    }

    /**
     * @return
     */
    public String getCurrentMode() {
        return currentMode;
    }

    /**
     * @param currentMode
     */
    public void setCurrentMode(String currentMode) {
        this.currentMode = currentMode;
    }

    /**
     * @param evt
     */
    public void modeChanged(ActionEvent evt) {
        setCurrentMode(getRequest().getParameter("mode"));
    }

    public String[] getModes() {
        return modes;
    }

}
