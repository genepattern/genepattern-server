package org.genepattern.server.job.input;

import com.google.common.base.Objects;

/**
 * Unique identifier for a group of input ParamValue.
 * @author pcarr
 *
 */
public class GroupId {
    public static final GroupId EMPTY=new GroupId();

    private final String name;
    private final String groupId;
    private GroupId() {
        this.name="";
        this.groupId="";
    }
    public GroupId(final String nameIn) {
        if (nameIn==null || nameIn.length()==0) {
            throw new IllegalArgumentException("name not set");
        }
        this.name=nameIn.trim();
        this.groupId=this.name.toLowerCase();
    }

    //copy constructor
    public GroupId(final GroupId in) {
        this.name=in.name;
        this.groupId=in.groupId;
    }
    
    public String getGroupId() {
        return groupId;
    }
    
    public String getName() {
        return name;
    }
    
    @Override
    public String toString() {
        return name;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(groupId);
    }

    @Override
    public boolean equals(final Object obj){
        if (obj==null) {
            return false;
        }
        if (!(obj instanceof GroupId)) {
            return false;
        }
        final GroupId other = (GroupId) obj;
        final boolean eq = Objects.equal(groupId, other.groupId);
        return eq;
    }

}
