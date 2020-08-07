package org.genepattern.drm.impl.deny;

import org.genepattern.drm.DrmJobRecord;
import org.genepattern.drm.DrmJobState;
import org.genepattern.drm.DrmJobStatus;
import org.genepattern.drm.DrmJobSubmission;
import org.genepattern.drm.JobRunner;
import org.genepattern.server.executor.CommandExecutorException;
import org.genepattern.server.executor.CommandProperties;

/**
 * RejectAllJobs JobRunner, special-case which rejects all new jobs.
 * @author pcarr
 */
public class DenyAllJobRunner implements JobRunner {
    private static final String DEFAULT_MESSAGE="This server is no longer accepting new jobs, please use 'https://cloud.genepattern.org/gp/' instead";
    
    private String message=DEFAULT_MESSAGE;
    
    public void setCommandProperties(final CommandProperties properties) {
        this.message=properties.getProperty("message", 
            DEFAULT_MESSAGE
        );
    }

    @Override
    public void stop() {
    }

    @Override
    public String startJob(final DrmJobSubmission drmJobSubmission) throws CommandExecutorException {
        throw new CommandExecutorException(message);
    }

    @Override
    public DrmJobStatus getStatus(DrmJobRecord drmJobRecord) {
        return new DrmJobStatus.Builder()
            .jobStatusMessage(message)
            .jobState(DrmJobState.ABORTED)
            .exitCode(1)
        .build();
    }

    @Override
    public boolean cancelJob(DrmJobRecord drmJobRecord) throws Exception {
        return true;
    }

}
