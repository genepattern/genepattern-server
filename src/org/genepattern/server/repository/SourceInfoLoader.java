/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.repository;

import org.genepattern.webservice.TaskInfo;

public interface SourceInfoLoader {
    public SourceInfo getSourceInfo(TaskInfo taskInfo);
}
