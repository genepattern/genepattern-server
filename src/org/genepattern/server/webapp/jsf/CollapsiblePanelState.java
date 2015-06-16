/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

/**
 *
 */
package org.genepattern.server.webapp.jsf;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author jrobinso
 * 
 */
public class CollapsiblePanelState implements Serializable {
    private static final long serialVersionUID = -7715182064696544439L;

    /** Maps panel id to state [closed, open] */
    private Map<String, String> panelStates = new HashMap<String, String>();

    private String selectedMode;

    public String getSelectedMode() {
        return selectedMode;
    }

    /**
     * This really should just be an action method, but the ajax servlet is
     * configured to get properties from beans, not call action methods. So the
     * method has to follow java bean property conventions (get...).
     * 
     */
    @SuppressWarnings("rawtypes")
    public String getUpdateChooserMode() {
        Map parameters = UIBeanHelper.getRequest().getParameterMap();
        String[] mode = (String[]) parameters.get("mode");

        if (mode != null) {
            setSelectedMode(mode[0]);
        }
        return "";
    }

    /**
     * This really should just be an action method, but the ajax servlet is
     * configured to get properties from beans, not call action methods. So the
     * method has to follow java bean property conventions (get...).
     * 
     */
    @SuppressWarnings("rawtypes")
    public String getUpdatePanelState() {
        Map parameters = UIBeanHelper.getRequest().getParameterMap();
        String[] panelId = (String[]) parameters.get("id");
        String[] state = (String[]) parameters.get("state");

        if (panelId != null && state != null) {
            panelStates.put(panelId[0], state[0]);
        }
        return "";
    }

    public boolean isClosed(String panelId) {
        String panelState = panelStates.get(panelId);
        return panelState == null || panelState.equals("closed");
    }

    public void setSelectedMode(String selectedMode) {
        this.selectedMode = selectedMode;
    }

}
