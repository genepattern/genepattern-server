package org.genepattern.server.auth;

public class GroupPermission {
    public enum Permission {
        NONE (0),
        READ_WRITE (1),
        READ (2);
        
        private final int flag_id;
        Permission(int flag_id) {
            this.flag_id = flag_id;
        }
        
        public int getFlag() {
            return flag_id;
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
    
    public GroupPermission(String groupId, int flag_from_db) {
        this.groupId = groupId;
        //hmmm ... kinda defeats the purpose of the enum
        switch (flag_from_db) {
        case 1:
            this.permission = Permission.READ_WRITE;
            break;
        case 2:
            this.permission = Permission.READ;
            break;
        default:
            this.permission = Permission.NONE;
            break;
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
