package org.genepattern.server.job.status;

import org.genepattern.server.DbException;
import org.genepattern.server.config.GpContext;

public interface JobStatusLoader {
    Status loadJobStatus(GpContext jobContext) throws DbException;
}
