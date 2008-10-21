package org.genepattern.server.auth;

public class GroupPermission {
    public enum Permission {
        NONE,
        READ_WRITE,
        READ;

        public int getFlag() {
            return this.ordinal();
        }

        public boolean canWrite() {
            return this == READ_WRITE;
        }

        public boolean canRead() {
            return this == READ || this == READ_WRITE;
        }
    };
    
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
}
