package org.genepattern.server.repository;

import org.genepattern.webservice.TaskInfo;

public interface SourceInfoLoader {
    public SourceInfo getSourceInfo(TaskInfo taskInfo);
}
