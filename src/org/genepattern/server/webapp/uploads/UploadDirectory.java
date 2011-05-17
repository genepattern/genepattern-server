package org.genepattern.server.webapp.uploads;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.genepattern.server.webapp.uploads.UploadFilesBean.FileInfoWrapper;

public class UploadDirectory {
    public String name;
    public List<FileInfoWrapper> uploadFiles;
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

    public List<FileInfoWrapper> getUploadFiles() {
        if (uploadFiles == null)
            uploadFiles = new ArrayList<FileInfoWrapper>();
        return uploadFiles;
    }

    public void setUploadFiles(List<FileInfoWrapper> uploadFiles) {
        this.uploadFiles = uploadFiles;

    }
    
    public List<FileInfoWrapper> getSortedUploadFiles() {
        Collections.sort(uploadFiles, new UploadFileComparator());
        return uploadFiles;
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
    
    public class UploadFileComparator implements Comparator<FileInfoWrapper> {
        public int compare(FileInfoWrapper o1, FileInfoWrapper o2) {
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
