package org.genepattern.server.executor.drm;

import java.util.ArrayList;
import java.util.List;

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
    //map of gpJobId -> drmJobId
    private BiMap<String, String> lookup=HashBiMap.create();

    @Override
    public List<String> getRunningDrmJobIds() {
        return new ArrayList<String>(lookup.values());
    }

    @Override
    public String lookupDrmJobId(JobInfo jobInfo) {
        return lookup.get(""+jobInfo.getJobNumber());
    }
    
    @Override
    public String lookupGpJobId(final String drmJobId) {
        return lookup.inverse().get(drmJobId);
    }

    @Override
    public void initDrmRecord(JobInfo jobInfo) {
        lookup.put(""+jobInfo.getJobNumber(), null);
    }

    @Override
    public void recordDrmId(String drmJobId, JobInfo jobInfo) {
        lookup.put(""+jobInfo.getJobNumber(), drmJobId);
    }
}