package org.genepattern.server.executor.drm;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.genepattern.server.executor.drm.dao.JobRunnerJob;
import org.genepattern.webservice.JobInfo;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

/**
 * A quick implementation of the lookup table which stores the values in RAM.
 * The lookup table is not persisted after a server restart.
 * @author pcarr
 *
 */
public class HashMapLookup implements DrmLookup {
    private static final Logger log = Logger.getLogger(HashMapLookup.class);

    private final String jobRunnerClassname;
    private final String jobRunnerId;
    
    public HashMapLookup(final String jobRunnerClassname, final String jobRunnerId) {
        this.jobRunnerClassname=jobRunnerClassname;
        this.jobRunnerId=jobRunnerId;
    }
    
    //map of gpJobId -> drmJobId
    private BiMap<Integer, String> lookup=HashBiMap.create();

    private Map<Integer, JobRunnerJob> detailsTable=new HashMap<Integer, JobRunnerJob>();

    @Override
    public List<String> getRunningDrmJobIds() {
        return new ArrayList<String>(lookup.values());
    }

    @Override
    public String lookupDrmJobId(JobInfo jobInfo) {
        return lookup.get(""+jobInfo.getJobNumber());
    }
    
    @Override
    public Integer lookupGpJobNo(final String drmJobId) {
        return lookup.inverse().get(drmJobId);
    }

    @Override
    public void updateDrmRecord(Integer gpJobNo, DrmJobStatus drmJobStatus) {
        JobRunnerJob rowOrig=detailsTable.get(gpJobNo);
        if (rowOrig==null) {
            log.error("Row not initialized for gpJobId="+gpJobNo);
            rowOrig=new JobRunnerJob(jobRunnerClassname, jobRunnerId, null, gpJobNo);
        }
        final JobRunnerJob updated=new JobRunnerJob(rowOrig, drmJobStatus);
        if (DrmExecutor.isSet(updated.getExtJobId())) {
            lookup.put(updated.getGpJobNo(), updated.getExtJobId());
        }
        detailsTable.put(gpJobNo, updated);
    }

    @Override
    public void insertDrmRecord(final File workingDir, final JobInfo jobInfo) {
        JobRunnerJob row=new JobRunnerJob(jobRunnerClassname, jobRunnerId, workingDir, jobInfo);
        detailsTable.put(row.getGpJobNo(), row);
    }
}