/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.auth;

import java.io.Serializable;

public class JobGroup implements Serializable {
    private int job_no = -1;
    private String group_id = null;
    private int permission_flag = -1;
    
    public JobGroup() {
    }
    
    public int getJobNo() {
        return job_no;
    }
    public void setJobNo(int i) {
        this.job_no = i;
    }
    
    public String getGroupId() {
        return group_id;
    }
    public void setGroupId(String str) {
        this.group_id = str;
    }
 
    public int getPermissionFlag() {
        return permission_flag;
    }
    public void setPermissionFlag(int i) {
        this.permission_flag = i;
    }

    //implement equals and hashCode because the JOB_GROUP table has a compound primary key
    public boolean equals(Object obj) {
        if (obj == null || ! (obj instanceof JobGroup)) {
            return false;
        }
        
        JobGroup jobGroup = (JobGroup) obj;
        return 
            job_no == jobGroup.job_no && 
            cmpStr(group_id, jobGroup.group_id) &&
            permission_flag == jobGroup.permission_flag;
    }
    
    public int hashCode() {
        String val = "" + job_no + "" + group_id + "" + permission_flag;
        return val.hashCode();
    }
    
    //compare strings accounting for null strings
    private boolean cmpStr(String str1, String str2) {
        if (str1 == null && str2 == null) {
            return true;
        }
        if (str1 == null) { //str2 is not null or we wouldn't be here
            return false;
        }
        return str1.equals(str2);
    }


}
