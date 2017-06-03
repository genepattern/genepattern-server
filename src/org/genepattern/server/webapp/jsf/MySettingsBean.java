/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

/**
 *
 */
package org.genepattern.server.webapp.jsf;

import static org.genepattern.server.webapp.jsf.UIBeanHelper.getRequest;

import javax.faces.event.ActionEvent;

import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;

public class MySettingsBean {
    private String[] modes;
    private String currentMode;

    public MySettingsBean() {
        final GpConfig gpConfig=ServerConfigurationFactory.instance();
        final boolean passwordRequired = 
            gpConfig.isPasswordRequired(GpContext.getServerContext());
        modes = passwordRequired ? new String[] { "Change Email", "Change Password", "History", "Visualizer Memory" }
                : new String[] { "Change Email", "History", "Visualizer Memory" };
        currentMode = modes[0]; // Default
    }

    public String getCurrentMode() {
        return currentMode;
    }

    public void setCurrentMode(String currentMode) {
        this.currentMode = currentMode;
    }

    public void modeChanged(ActionEvent evt) {
        setCurrentMode(getRequest().getParameter("mode"));
    }

    public String[] getModes() {
        return modes;
    }

}
