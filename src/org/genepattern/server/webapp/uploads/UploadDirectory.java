package org.genepattern.server.webapp.uploads;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UploadDirectory {
    public String name;
    public Map<String, UploadFileInfo> uploadFiles;
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

    public Map<String, UploadFileInfo> getUploadFiles() {
        if (uploadFiles == null)
            uploadFiles = new HashMap<String, UploadFileInfo>();
        return uploadFiles;
    }

    public void setUploadFiles(Map<String, UploadFileInfo> uploadFiles) {
        this.uploadFiles = uploadFiles;

    }
    
    public List<UploadFileInfo> getSortedUploadFiles() {
        List<UploadFileInfo> files = new ArrayList<UploadFileInfo>();
        for (UploadFileInfo i : this.getUploadFiles().values()) {
            files.add(i);
        }
        Collections.sort(files, new UploadFileComparator());
        return files;
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
    
    public class UploadFileComparator implements Comparator<UploadFileInfo> {
        public int compare(UploadFileInfo o1, UploadFileInfo o2) {
            long value = o1.getModified() - o2.getModified();
            if (value < 0) {
                return 1;
            }
            else if (value == 0) {
                return 0;
            }
            else {
                return -1;
            }
        }
    }
}
