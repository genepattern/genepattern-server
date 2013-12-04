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

    //map of gpJobId -> drmJobId
    private BiMap<String, String> lookup=HashBiMap.create();

    //table of details
    private static class Row {
        private final String gpJobId;
        private final File workingDir;
        private final String extJobId;
        private final JobState extJobState;
        private final String extJobStatusMessage;
        private final Date timestamp=new Date();
        
        public Row(final File workingDir, final JobInfo jobInfo) {
            this(workingDir, ""+jobInfo.getJobNumber());
        }
        
        public Row(final File workingDir, final String gpJobId) {
            this.gpJobId=gpJobId;
            this.workingDir=workingDir;
            this.extJobId=null;
            this.extJobState=JobState.UNDETERMINED;
            this.extJobStatusMessage=extJobState.toString();
        }

//        public Row(final Row in) {
//            this.gpJobId=in.gpJobId;
//            this.workingDir=in.workingDir;
//            this.extJobId=in.extJobId;
//            this.extJobState=in.extJobState;
//            this.extJobStatusMessage=in.extJobStatusMessage;
//            
//        }
        
        public Row(final Row in, final DrmJobStatus updated) {
            this.gpJobId=in.gpJobId;
            this.workingDir=in.workingDir;
            this.extJobId=updated.getDrmJobId();
            this.extJobState=updated.getJobState();
            this.extJobStatusMessage=updated.getJobStatusMessage();
        }
    }
    
    private Map<String, Row> detailsTable=new HashMap<String,Row>();

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
        Row rowOrig=detailsTable.get(gpJobId);
        if (rowOrig==null) {
            log.error("Row not initialized for gpJobId="+gpJobId);
            rowOrig=new Row(null, gpJobId);
        }
        final Row updated=new Row(rowOrig, drmJobStatus);
        if (DrmExecutor.isSet(updated.extJobId)) {
            lookup.put(updated.gpJobId, updated.extJobId);
        }
        detailsTable.put(gpJobId, updated);
    }

    @Override
    public void insertDrmRecord(final File workingDir, final JobInfo jobInfo) {
        Row row=new Row(workingDir, jobInfo);
        //lookup.put(row.gpJobId, row.extJobId);
        detailsTable.put(row.gpJobId, row);
    }
}