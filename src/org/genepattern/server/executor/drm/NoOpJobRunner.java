/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.executor.drm;

import org.genepattern.drm.DrmJobRecord;
import org.genepattern.drm.DrmJobStatus;
import org.genepattern.drm.DrmJobSubmission;
import org.genepattern.drm.JobRunner;
import org.genepattern.server.executor.CommandExecutorException;

/**
 * No-op JobRunner, only gets created when there are config file errors.
 * @author pcarr
 *
 */
public class NoOpJobRunner implements JobRunner {
    private final String classname;
    public NoOpJobRunner(final String classname) {
        this.classname=classname;
    }

    @Override
    public void stop() {
    }

    @Override
    public String startJob(final DrmJobSubmission drmJobSubmission) throws CommandExecutorException {
        throw new CommandExecutorException("Server configuration error: the jobRunner was not initialized from classname="+classname);
    }

    @Override
    public DrmJobStatus getStatus(DrmJobRecord drmJobRecord) {
        return null;
    }

    @Override
    public boolean cancelJob(final DrmJobRecord drmJobRecord) throws Exception {
        throw new Exception("Server configuration error: the jobRunner was not initialized from classname="+classname);
    }

}
