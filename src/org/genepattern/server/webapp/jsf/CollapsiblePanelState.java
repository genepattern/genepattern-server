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

    public String updatePanelState() {
	String[] panelId = (String[]) UIBeanHelper.getRequest()
		.getParameterMap().get("panelId");
	String[] state = (String[]) UIBeanHelper.getRequest().getParameterMap()
		.get("state");
	if (panelId != null && state != null) {
	    panelStates.put(panelId[0], state[0]);
	}
	return "";
    }

    public boolean isClosed(String panelId) {
        String panelState = (String) panelStates.get(panelId);
	return   panelState != null && panelState.equals("closed");
	    
    }


    private String selectedMode = "category";

    public String getSelectedMode() {
	return selectedMode;
    }

    public void setSelectedMode(String selectedMode) {
	this.selectedMode = selectedMode;
    }

}
