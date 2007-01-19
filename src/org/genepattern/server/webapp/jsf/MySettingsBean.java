/**
 * 
 */
package org.genepattern.server.webapp.jsf;

import static org.genepattern.server.webapp.jsf.UIBeanHelper.getRequest;

import javax.faces.event.ActionEvent;

import org.genepattern.server.user.User;
import org.genepattern.server.user.UserDAO;

public class MySettingsBean {

    private static String[] modes = new String[] { "Change Password",
            "Change Email", "Visualizer Memory", "History"};

    private String currentMode = modes[0]; // Default

    /**
     * 
     */
    public MySettingsBean() {
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
