/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

/* Auto generated file */

package org.genepattern.server.domain;

import java.net.MalformedURLException;
import java.util.List;

import org.genepattern.util.LSID;

/**
 * A hibernate mapped POJO representing a Suite. This class is a near copy of SuiteInfo. Both are kept for an interim
 * period as we transition to Hibernate.
 * 
 * @author jrobinso
 * 
 */
public class Suite implements java.io.Serializable {

    private String lsid;

    private String name;

    private String author;

    private String contact;

    private String description;

    private String userId;

    private Integer accessId;

    private List<String> modules;

    private boolean selected = false;

    private boolean expanded = true;

    public String getLsid() {
        return this.lsid;
    }

    public void setLsid(String value) {
        this.lsid = value;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String value) {
        this.name = value;
    }

    public String getAuthor() {
        return this.author;
    }

    public void setAuthor(String value) {
        this.author = value;
    }

    public String getUserId() {
        return this.userId;
    }

    public void setUserId(String value) {
        this.userId = value;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String value) {
        this.description = value;
    }

    public Integer getAccessId() {
        return this.accessId;
    }

    public void setAccessId(Integer value) {
        this.accessId = value;
    }

    public List<String> getModules() {
        return modules;
    }

    public void setModules(List<String> modules) {
        this.modules = modules;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public boolean isSelected() {
        return selected;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public String getVersion() {
        try {
            LSID lsidObject = new LSID(getLsid());
            return lsidObject.getVersion();
        } catch (MalformedURLException e) {
            return "";
        }
    }
}
