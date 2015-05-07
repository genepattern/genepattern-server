/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.auth;

import java.io.Serializable;
import java.util.Comparator;

public class GroupPermission implements Comparable<GroupPermission>, Serializable {
    /** Group id for public access. */
    final static public String PUBLIC = "*";

    public enum Permission {
        NONE,
        READ_WRITE,
        READ;

        public int getFlag() {
            return this.ordinal();
        }

        public boolean getWrite() {
            return this == READ_WRITE;
        }

        public boolean getRead() {
            return this == READ || this == READ_WRITE;
        }
    };
    
    /**
     * Helper class for sorting permissions first by permission type (WRITE then READ), then by group id.
     * Use this to output a listing of all groups which have write permission, followed by all groups which have read permission.
     */
    public static class SortByPermission implements Comparator<GroupPermission> {
        public int compare(GroupPermission arg0, GroupPermission arg1) {
            if (arg0 == arg1) {
                return 0;
            }
            int pcomp = arg0.permission.compareTo(arg1.permission);
            if (pcomp != 0) {
                return pcomp;
            }
            return arg0.groupId.compareTo(arg1.groupId);
        }
    }
    
    private String groupId = null;
    private Permission permission = null;
    
    /**
     * Create a new instance via db lookup.
     * @param groupId
     * @param flag_from_db 
     *        - use the integer value from the PERMISSION_FLAG.ID table in the database,
     *          Make sure the values in the DB correspond to the order of declaration in the Permission enum.
     */
    public GroupPermission(String groupId, int flag_from_db) {
        this.groupId = groupId;
        if (flag_from_db >= 0 && flag_from_db < Permission.values().length) {
            this.permission = Permission.values()[flag_from_db];
        }
        else {
            this.permission = Permission.NONE;
        }
    }

    public GroupPermission(String groupId, Permission p) {
        this.groupId = groupId;
        this.permission = p;
    }
    
    public String getGroupId() {
        return groupId;
    }
    
    public Permission getPermission() {
        return permission;
    }

    /**
     * Default comparison between groups is based on case-insensitive on group id.
     */
    public int compareTo(GroupPermission to) {
        if (this.groupId == null) {
            if (to.groupId == null) {
                return 0;
            }
            return -1;
        }
        if (to.groupId == null) {
            return 1;
        }
        //by default compare the group id's
        int rval = this.groupId.toLowerCase().compareTo(to.groupId.toLowerCase());
        if (rval == 0) {
            return this.permission.compareTo(to.permission);
        }
        return rval;
    }
}
