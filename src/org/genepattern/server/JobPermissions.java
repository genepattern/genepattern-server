package org.genepattern.server;

/**
 * Representation of the access permissions that a given user has for a given job.
 * 
 * @author pcarr
 *
 */
public class JobPermissions {
    private final boolean isPublic;
    private final boolean isShared;
    private final boolean canRead;
    private final boolean canWrite;
    private final boolean canSetPermissions;
    
    private JobPermissions(Builder in) {
        this.isPublic=in.isPublic;
        this.isShared=in.isShared;
        this.canRead=in.canRead;
        this.canWrite=in.canWrite;
        this.canSetPermissions=in.canSetPermissions;
    }
    
    /**
     * Does the user have read access to the job.
     * 
     * @return
     */
    public boolean canReadJob() {
        return this.canRead;
    }

    /**
     * Does the user have write access to the job.
     * 
     * @return
     */
    public boolean canWriteJob() {
        return this.canWrite;
    }

    /**
     * Can the user set permissions on the job.
     * @return
     */
    public boolean canSetJobPermissions() {
        return this.canSetPermissions;
    }

    /**
     * Is the current job read or write accessible by members of the 'public' group.
     * @return
     */
    public boolean isPublic() {
        return isPublic;        
    }
    
    /**
     * Is the current job read or write accessible by anyone other than the owner?
     * @return
     */
    public boolean isShared() {
        return isShared;
    }
    
    public static class Builder {
        private boolean canRead = false;
        private boolean canWrite = false;
        private boolean canSetPermissions = false;
        private boolean isPublic = false;
        private boolean isShared = false;

        public JobPermissions build() {
            return new JobPermissions(this);
        }
        
        public Builder canRead(final boolean canRead) {
            this.canRead=canRead;
            return this;
        }
        public Builder canWrite(final boolean canWrite) {
            this.canWrite=canWrite;
            return this;
        }
        public Builder canSetPermissions(final boolean canSetPermissions) {
            this.canSetPermissions=canSetPermissions;
            return this;
        }
        public Builder isPublic(final boolean isPublic) {
            this.isPublic=isPublic;
            return this;
        }
        public Builder isShared(final boolean isShared) {
            this.isShared=isShared;
            return this;
        }
    }

}
