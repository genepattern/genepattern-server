package org.genepattern.server.dm.userupload;

import org.genepattern.drm.Memory;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.webapp.jsf.JobHelper;


public class UserDiskPair {
    private String user;
    private long diskUsage;

    public UserDiskPair(Object[] query) {
        this.setUser((String) query[0]);
        this.setDiskUsage((Long) query[1]);
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public long getDiskUsage() {
        return diskUsage;
    }

    public void setDiskUsage(long diskUsage) {
        this.diskUsage = diskUsage;
    }

    public String getPrettyDiskUsage() {
        return JobHelper.getFormattedSize(diskUsage);
    }

    public Memory getDiskQuota() {
        GpConfig config = ServerConfigurationFactory.instance();
        GpContext context = GpContext.getContextForUser(user);
        return config.getGPMemoryProperty(context, "quota");
    }

    public boolean isOverQuota() {
        Memory quota = this.getDiskQuota();
        if (quota == null) {
            return false;
        }
        return quota.getNumBytes() <= diskUsage;
    }
}

