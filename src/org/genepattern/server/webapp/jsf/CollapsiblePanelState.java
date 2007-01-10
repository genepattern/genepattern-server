/**
 * 
 */
package org.genepattern.server.webapp.jsf;

import static org.genepattern.server.webapp.jsf.UIBeanHelper.getRequest;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author jrobinso
 * 
 */
public class CollapsiblePanelState {
    private final String expStatePrefix = "expansion_state_";

    private Map<String, String> panelStates = new HashMap<String, String>();

    /**
         * This really should just be an action method, but the ajax servlet is
         * configured to get properties from beans, not call action methods. So
         * the method has to follow java bean property conventions (get...).
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
	String panelState = (String) panelStates.get(panelId);
	return panelState != null && panelState.equals("closed");

    }

    private String selectedMode = "category";

    /**
         * This really should just be an action method, but the ajax servlet is
         * configured to get properties from beans, not call action methods. So
         * the method has to follow java bean property conventions (get...).
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

    public String getSelectedMode() {
	return selectedMode;
    }

    public void setSelectedMode(String selectedMode) {
	this.selectedMode = selectedMode;
    }

}
