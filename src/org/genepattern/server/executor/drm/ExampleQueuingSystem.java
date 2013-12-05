package org.genepattern.server.executor.drm;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import org.genepattern.server.executor.CommandExecutorException;
import org.genepattern.webservice.JobInfo;

public class ExampleQueuingSystem implements QueuingSystem {
    private Map<String,DrmJobStatus> statusMap=new LinkedHashMap<String,DrmJobStatus>();

    @Override
    public String startJob(String[] commandLine, Map<String, String> environmentVariables, File runDir, File stdoutFile, File stderrFile, JobInfo jobInfo, File stdinFile) throws CommandExecutorException {
        // TODO Auto-generated method stub
        final String drmJobId="DRM_"+jobInfo.getJobNumber();
        final DrmJobStatus drmJobStatus=new DrmJobStatus.Builder(drmJobId, JobState.QUEUED).build();
        statusMap.put(drmJobId, drmJobStatus);
        return drmJobId;
    }

    @Override
    public DrmJobStatus getStatus(String drmJobId) {
        return statusMap.get(drmJobId);
    }

    @Override
    public void cancelJob(String drmJobId, JobInfo jobInfo) throws Exception {
        throw new Exception("cancelJob not implemented, gpJobId="+jobInfo.getJobNumber()+", drmJobId="+drmJobId);
    }

    @Override
    public void stop() {
        //no-op
    }
}