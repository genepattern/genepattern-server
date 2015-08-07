/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.status;

import org.genepattern.server.DbException;
import org.genepattern.server.config.GpContext;

public interface JobStatusLoader {
    Status loadJobStatus(GpContext jobContext) throws DbException;
}
