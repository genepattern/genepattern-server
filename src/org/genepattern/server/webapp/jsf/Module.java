/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.server.webapp.jsf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.faces.model.SelectItem;

import org.genepattern.util.LSID;
import org.genepattern.util.LSIDVersionComparator;
import org.genepattern.webservice.TaskInfo;

/**
 * Represents a module or pipeline including all versions.
 *
 * @author jrobinso
 *
 */
public class Module implements java.io.Serializable {

    /** Whether this module is a pipeline */
    private boolean pipeline;

    /** LSID for this module */
    private LSID lsid;

    private String name;

    /** Whether this module is selected */
    private boolean selected = false;

    /** selected module version (e.g. 3, 5, or empty string for latest version) */
    private String selectedVersion = "";

    private String shortName;

    private String userId;

    /** List of all available versions for this module */
    private List<SelectItem> versions = new ArrayList<SelectItem>();

    public Module(TaskInfo ti, LSID lsid) {
        this.userId = ti.getUserId();
        this.name = ti.getName();
        this.shortName = ti.getShortName();
        this.pipeline = ti.isPipeline();
        this.lsid = lsid;

        // Add the "latest" version option
        versions.add(new SelectItem("", "latest"));
        versions.add(new SelectItem(lsid.getVersion(), lsid.getVersion()));
    }

    public String getSelectedLsid() {
        return selectedVersion == null || selectedVersion.equals("") ? lsid.toString() : lsid.toStringNoVersion() + ":"
                + selectedVersion;
    }

    public void addVersion(LSID lsid) {
        versions.add(new SelectItem(lsid.getVersion(), lsid.getVersion()));
    }

    public String getLsid() {
        return lsid.toString();
    }

    public LSID getLSID() {
        return lsid;
    }

    public String getName() {
        return name;
    }

    public String getSelectedVersion() {
        return selectedVersion;
    }

    public String getShortName() {
        return shortName;
    }

    public String getUserId() {
        return userId;
    }

    public List<SelectItem> getVersions() {
        return versions;
    }

    public List<SelectItem> getVersionSelectItems() {
        Collections.sort(versions, new Comparator<SelectItem>() {
            public int compare(SelectItem o1, SelectItem o2) {
                String v1 = o1.getLabel();
                String v2 = o2.getLabel();
                if (v1.toLowerCase().equals("latest"))
                    return -1;
                else if (v2.toLowerCase().equals("latest"))
                    return 1;
                else
                    return LSIDVersionComparator.INSTANCE.compare(v2, v1);
            }

        });
        return versions;

    }

    /**
     *
     * @return true if the LSID indicates this module is from the broad. This
     *         affects the color scheme for the module name.
     */
    public boolean isFromBroad() {
        String authority = "";
        LSID lsid = this.getLSID();
        if (lsid != null) {
            authority = lsid.getAuthority();
        }
        return "broadinstitute.org".equals(authority) || "broad.mit.edu".equals(authority);
    }

    public boolean isPipeline() {
        return pipeline;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public void setSelectedVersion(String selectedVersion) {
        this.selectedVersion = selectedVersion;
    }

}
