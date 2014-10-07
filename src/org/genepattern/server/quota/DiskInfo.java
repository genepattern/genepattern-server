package org.genepattern.server.quota;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.log4j.Logger;
import org.genepattern.drm.Memory;
import org.genepattern.server.DbException;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.dm.userupload.dao.UserUploadDao;

/**
 * Created by nazaire on 7/10/14.
 */
@XmlRootElement
public class DiskInfo
{
    final static private Logger log = Logger.getLogger(DiskInfo.class);

    private final String userId;
    private Memory diskUsageTotal;
    private Memory diskUsageTmp;
    private Memory diskUsageFilesTab;
    private Memory diskQuota;

    public DiskInfo(final String userId) {
        this.userId=userId;
    }
    
    public String getUserId() {
        return userId;
    }

    public void setDiskUsageTotal(Memory diskUsageTotal)
    {
        this.diskUsageTotal = diskUsageTotal;
    }

    public Memory getDiskUsageTotal() { return diskUsageTotal;}

    public void setDiskUsageTmp(Memory diskUsageTmp) {
        this.diskUsageTmp = diskUsageTmp;
    }

    public Memory getDiskUsageTmp() { return diskUsageTmp;}

    public void setDiskUsageFilesTab(Memory diskUsageFilesTab) {
        this.diskUsageFilesTab = diskUsageFilesTab;
    }

    public Memory getDiskUsageFilesTab() { return diskUsageFilesTab;}

    public Memory getDiskQuota() {
        return diskQuota;
    }

    public void setDiskQuota(Memory diskQuota) {
        this.diskQuota = diskQuota;
    }
    
    public static DiskInfo createDiskInfo(final String userId, final long filesTab_NumBytes) {
        GpConfig gpConfig=ServerConfigurationFactory.instance();
        return createDiskInfo(gpConfig, userId, filesTab_NumBytes);
    }

    public static DiskInfo createDiskInfo(final GpConfig gpConfig, final String userId, final long filesTab_NumBytes) {
        GpContext userContext=GpContext.createContextForUser(userId, false);
        return createDiskInfo(gpConfig, userContext, filesTab_NumBytes);
    }

    public static DiskInfo createDiskInfo(final GpConfig gpConfig, final GpContext userContext, final long filesTab_NumBytes) {
        final DiskInfo diskInfo = new DiskInfo(userContext.getUserId());
        diskInfo.setDiskUsageFilesTab(Memory.fromSizeInBytes(filesTab_NumBytes));
        diskInfo.setDiskQuota(gpConfig.getGPMemoryProperty(userContext, "quota"));
        return diskInfo;
    } 

    public static DiskInfo createDiskInfo(GpConfig gpConfig, GpContext context) throws DbException {
        final String userId=context.getUserId();
        final Memory diskQuota=gpConfig.getGPMemoryProperty(context, "quota");
        return createDiskInfo(userId, diskQuota);
    }

    public static DiskInfo createDiskInfo(final String userId, final Memory diskQuota) throws DbException {
        final DiskInfo diskInfo = new DiskInfo(userId);
        final boolean isInTransaction= HibernateUtil.isInTransaction();
        try
        {
            HibernateUtil.beginTransaction();
            UserUploadDao userUploadDao = new UserUploadDao();

            // bug fix, GP-5412, make sure to compute files tab usage before total usage
            Memory diskUsageFilesTab = userUploadDao.sizeOfAllUserUploads(userId, false);
            Memory diskUsageTotal = userUploadDao.sizeOfAllUserUploads(userId, true);

            Memory diskUsageTmp = null;
            if(diskUsageTotal != null && diskUsageFilesTab != null)
            {
                diskUsageTmp = Memory.fromSizeInBytes(diskUsageTotal.getNumBytes() - diskUsageFilesTab.getNumBytes());
            }

            diskInfo.setDiskUsageTotal(diskUsageTotal);
            diskInfo.setDiskUsageFilesTab(diskUsageFilesTab);
            diskInfo.setDiskUsageTmp(diskUsageTmp);
            diskInfo.setDiskQuota(diskQuota);
        }
        catch (Throwable t)
        {
            log.error(t);
            throw new DbException(t);
        }
        finally
        {
            if (!isInTransaction)
            {
                HibernateUtil.closeCurrentSession();
            }
        }

        return diskInfo;
    }

    public boolean isAboveQuota()
    {
        return isAboveQuota(0);
    }

    public boolean isAboveQuota(long fileSizeInBytes)
    {
        if(diskQuota == null || diskUsageFilesTab == null)
        {
            return false;
        }

        long diskUsagePlus = diskUsageFilesTab.getNumBytes();

        if(fileSizeInBytes > 0)
        {
            diskUsagePlus += fileSizeInBytes;
        }

        return diskUsagePlus > diskQuota.getNumBytes();
    }
}
