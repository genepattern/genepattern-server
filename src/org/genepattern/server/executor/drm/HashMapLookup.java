package org.genepattern.server.executor.drm;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
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
    private BiMap<String, String> lookup=HashBiMap.create();

    //table of details
    private static class ExternalJobRecord {
        private final String gpJobId;
        private final File workingDir;
        private final String jobRunnerClassname;
        private final String jobRunnerId;
        private final String extJobId;
        private final JobState extJobState;
        private final String extJobStatusMessage;
        private final Date timestamp=new Date();
        
        public ExternalJobRecord(final String jobRunnerClassname, final String jobRunnerId, final File workingDir, final JobInfo jobInfo) {
            this(jobRunnerClassname, jobRunnerId, workingDir, ""+jobInfo.getJobNumber());
        }
        
        public ExternalJobRecord(final String jobRunnerClassname, final String jobRunnerId, final File workingDir, final String gpJobId) {
            this.jobRunnerClassname=jobRunnerClassname;
            this.jobRunnerId=jobRunnerId;
            this.gpJobId=gpJobId;
            this.workingDir=workingDir;
            this.extJobId=null;
            this.extJobState=JobState.UNDETERMINED;
            this.extJobStatusMessage=extJobState.toString();
        }
        
        public ExternalJobRecord(final ExternalJobRecord in, final DrmJobStatus updated) {
            this.jobRunnerClassname=in.jobRunnerClassname;
            this.jobRunnerId=in.jobRunnerId;
            this.gpJobId=in.gpJobId;
            this.workingDir=in.workingDir;
            this.extJobId=updated.getDrmJobId();
            this.extJobState=updated.getJobState();
            this.extJobStatusMessage=updated.getJobStatusMessage();
        }
    }
    
    private Map<String, ExternalJobRecord> detailsTable=new HashMap<String,ExternalJobRecord>();

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
    public void updateDrmRecord(String gpJobId, DrmJobStatus drmJobStatus) {
        ExternalJobRecord rowOrig=detailsTable.get(gpJobId);
        if (rowOrig==null) {
            log.error("Row not initialized for gpJobId="+gpJobId);
            rowOrig=new ExternalJobRecord(jobRunnerClassname, jobRunnerId, null, gpJobId);
        }
        final ExternalJobRecord updated=new ExternalJobRecord(rowOrig, drmJobStatus);
        if (DrmExecutor.isSet(updated.extJobId)) {
            lookup.put(updated.gpJobId, updated.extJobId);
        }
        detailsTable.put(gpJobId, updated);
    }

    @Override
    public void insertDrmRecord(final File workingDir, final JobInfo jobInfo) {
        ExternalJobRecord row=new ExternalJobRecord(jobRunnerClassname, jobRunnerId, workingDir, jobInfo);
        //lookup.put(row.gpJobId, row.extJobId);
        detailsTable.put(row.gpJobId, row);
    }
}