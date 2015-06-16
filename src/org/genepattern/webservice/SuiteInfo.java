/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.webservice;

/**
 * Used to hold information about particular suite
 * 
 * @author Ted Liefeld
 * @version 1.0
 */

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.genepattern.util.GPConstants;

public class SuiteInfo implements Serializable {

    private String lsid = null;

    private String name = "", description = "";

    private int accessId = 0;

    private String author = null;

    private String owner = null;

    private String contact = null;

    private String[] moduleLsids = new String[0];

    private String[] docFiles = new String[0];

    /** Creates new SuiteInfo */
    public SuiteInfo() {
    }

    public SuiteInfo(String lsid, String name, String description, String author, String owner, List modules,
            int access_id, List docs) {
        this.lsid = lsid;
        this.name = name;
        this.author = author;
        this.owner = owner;
        this.description = description;
        this.moduleLsids = (String[]) modules.toArray(new String[modules.size()]);
        this.accessId = access_id;
        this.docFiles = (String[]) docs.toArray(new String[docs.size()]);

    }

    public SuiteInfo(Map hm) {
        this.lsid = (String) hm.get("lsid");
        this.name = (String) hm.get("name");
        this.author = (String) hm.get("author");
        this.owner = (String) hm.get("owner");
        this.description = (String) hm.get("description");
        ArrayList modules = (ArrayList) hm.get("modules");
        this.moduleLsids = new String[modules.size()];
        int i = 0;
        for (Iterator iter = modules.iterator(); iter.hasNext(); i++) {
            Map modMap = (Map) iter.next();
            moduleLsids[i] = (String) modMap.get("lsid");
        }
        ArrayList docs = (ArrayList) hm.get("docFiles");
        this.docFiles = new String[docs.size()];
        i = 0;
        for (Iterator iter = docs.iterator(); iter.hasNext(); i++) {
            String doc = (String) iter.next();
            docFiles[i] = doc;
        }
        this.accessId = GPConstants.ACCESS_PUBLIC;

    }

    public void setOwner(String userId) {
        this.owner = userId;
    }

    public String getOwner() {
        return this.owner;
    }

    public void setAuthor(String userId) {
        this.author = userId;
    }

    public String getAuthor() {
        return this.author;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return this.description;
    }

    public void setLSID(String LSID) {
        this.lsid = LSID;
    }

    public String getLSID() {
        return this.lsid;
    }

    public void setAccessId(int accessId) {
        this.accessId = accessId;
    }

    public int getAccessId() {
        return this.accessId;
    }

    public String getName() {
        return name;
    }

    public void setName(java.lang.String taskName) {
        this.name = taskName;
    }

    public String getID() {
        return getLSID();
    }

    public void setID(String ID) {
        setLSID(ID);
    }

    public String[] getModuleLSIDs() {
        return moduleLsids;
    }

    public void setModuleLSIDs(String[] mods) {
        this.moduleLsids = mods;
    }

    public String[] getDocumentationFiles() {
        return docFiles;
    }

    public void setDocumentationFiles(String[] mods) {
        this.docFiles = mods;
    }

    public boolean equals(Object otherThing) {
        if (!(otherThing instanceof SuiteInfo)) {
            return false;
        }
        SuiteInfo other = (SuiteInfo) otherThing;
        return getOwner().equals(other.getOwner()) && getAuthor() == other.getAuthor()
                && getName().equals(other.getName()) && getLSID() == other.getLSID()
                && getDescription().equals(other.getDescription());
    }

    public int hashCode() {
        return getLSID() != null ? getLSID().hashCode() : super.hashCode();
    }

    public String[] getDocFiles() {
        return docFiles;
    }

    public void setDocFiles(String[] docFiles) {
        this.docFiles = docFiles;
    }

    public String getLsid() {
        return lsid;
    }

    public void setLsid(String lsid) {
        this.lsid = lsid;
    }

    public String[] getModuleLsids() {
        return moduleLsids;
    }

    public void setModuleLsids(String[] moduleLsids) {
        this.moduleLsids = moduleLsids;
    }

    public void setModuleLsids(List<String> moduleList) {

        this.moduleLsids = new String[moduleList.size()];
        for (int i = 0; i < moduleList.size(); i++) {
            moduleLsids[i] = moduleList.get(i);
        }
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

}
