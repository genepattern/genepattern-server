/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.status;

import org.genepattern.server.DbException;
import org.genepattern.server.config.GpContext;

public interface JobStatusLoader {
    Status loadJobStatus(GpContext jobContext) throws DbException;
}
