package org.genepattern.server.webapp.uploads;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.genepattern.server.webapp.jsf.KeyValuePair;
import org.genepattern.server.webapp.jsf.UIBeanHelper;

import org.genepattern.util.GPConstants;
import org.genepattern.util.SemanticUtil;
import org.genepattern.webservice.TaskInfo;

public class UploadDirectory {
    private static final Comparator<KeyValuePair> COMPARATOR = new KeyValueComparator();

    public String name;
    public List<UploadFileInfo> uploadFiles;
    public boolean expanded = true;
    public int level = 0;

    public UploadDirectory(String nom) {
        this.name = nom;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<UploadFileInfo> getUploadFiles() {
        if (uploadFiles == null)
            uploadFiles = new ArrayList<UploadFileInfo>();
        return uploadFiles;
    }

    public void setUploadFiles(List<UploadFileInfo> uploadFiles) {
        this.uploadFiles = uploadFiles;

    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    private static class KeyValueComparator implements Comparator<KeyValuePair> {

        public int compare(KeyValuePair o1, KeyValuePair o2) {
            return o1.getKey().compareToIgnoreCase(o2.getKey());
        }

    }
}
