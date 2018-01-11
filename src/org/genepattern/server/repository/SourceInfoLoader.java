/*******************************************************************************
 * Copyright (c) 2003-2018 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.repository;

import org.genepattern.webservice.TaskInfo;

public interface SourceInfoLoader {
    public SourceInfo getSourceInfo(TaskInfo taskInfo);
}
