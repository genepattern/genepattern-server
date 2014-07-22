package org.genepattern.server.quota;

import org.apache.log4j.Logger;
import org.genepattern.drm.Memory;
import org.genepattern.server.DbException;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.dm.userupload.dao.UserUploadDao;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by nazaire on 7/10/14.
 */
@XmlRootElement
public class DiskInfo
{
    final static private Logger log = Logger.getLogger(DiskInfo.class);

    private Memory diskUsageTotal;
    private Memory diskUsageTmp;
    private Memory diskUsageFilesTab;
    private Memory diskQuota;

    private DiskInfo()
    {}

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

    public static DiskInfo createDiskInfo(GpConfig gpConfig, GpContext context) throws DbException
    {

        DiskInfo diskInfo = new DiskInfo();

        final boolean isInTransaction= HibernateUtil.isInTransaction();

        try
        {
            HibernateUtil.beginTransaction();
            UserUploadDao userUploadDao = new UserUploadDao();

            Memory diskUsageTotal = userUploadDao.sizeOfAllUserUploads(context.getUserId(), true);
            Memory diskUsageFilesTab = userUploadDao.sizeOfAllUserUploads(context.getUserId(), false);
            Memory diskUsageTmp = Memory.fromSizeInBytes(diskUsageTotal.getNumBytes() - diskUsageFilesTab.getNumBytes());

            diskInfo.setDiskUsageTotal(diskUsageTotal);
            diskInfo.setDiskUsageFilesTab(diskUsageFilesTab);
            diskInfo.setDiskUsageTmp(diskUsageTmp);

            //now get the quota from the gpconfig
            diskInfo.setDiskQuota(gpConfig.getGPMemoryProperty(context, "quota"));
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
}
