/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2009) by the
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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author jrobinso
 * 
 */
public class CollapsiblePanelState implements Serializable {
    /** Maps panel id to state [closed, open] */
    private Map<String, String> panelStates = new HashMap<String, String>();

    private String selectedMode;

    public String getSelectedMode() {
	return selectedMode;
    }

    /**
     * This really should just be an action method, but the ajax servlet is configured to get properties from beans, not
     * call action methods. So the method has to follow java bean property conventions (get...).
     * 
     */
    public String getUpdateChooserMode() {
	Map parameters = UIBeanHelper.getRequest().getParameterMap();
	String[] mode = (String[]) parameters.get("mode");

	if (mode != null) {
	    setSelectedMode(mode[0]);
	}
	return "";
    }

    /**
     * This really should just be an action method, but the ajax servlet is configured to get properties from beans, not
     * call action methods. So the method has to follow java bean property conventions (get...).
     * 
     */
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
	return panelState != null && panelState.equals("closed");

    }

    public void setSelectedMode(String selectedMode) {
	this.selectedMode = selectedMode;
    }

}
