package org.genepattern.server.executor.drm;

import java.io.File;
import java.util.List;

import org.genepattern.webservice.JobInfo;

public class DbLookup implements DrmLookup {
    private final String jobRunnerClassname;
    private final String jobRunnerId;
    
    public DbLookup(final String jobRunnerClassname, final String jobRunnerId) {
        this.jobRunnerClassname=jobRunnerClassname;
        this.jobRunnerId=jobRunnerId;
    }

    @Override
    public List<String> getRunningDrmJobIds() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String lookupDrmJobId(JobInfo jobInfo) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void insertDrmRecord(File workingDir, JobInfo jobInfo) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Integer lookupGpJobNo(String drmJobId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void updateDrmRecord(Integer gpJobNo, DrmJobStatus drmJobStatus) {
        // TODO Auto-generated method stub
        
    }

}
