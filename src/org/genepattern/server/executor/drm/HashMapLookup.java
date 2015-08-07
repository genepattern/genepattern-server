/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.executor.drm;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.genepattern.drm.DrmJobRecord;
import org.genepattern.drm.DrmJobStatus;
import org.genepattern.drm.DrmJobSubmission;
import org.genepattern.server.executor.drm.dao.JobRunnerJob;

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

    private static final boolean isSet(final String str) {
        return str!=null && str.length()>0;
    }

    private final String jobRunnerClassname;
    private final String jobRunnerName;
    
    public HashMapLookup(final String jobRunnerClassname, final String jobRunnerName) {
        this.jobRunnerClassname=jobRunnerClassname;
        this.jobRunnerName=jobRunnerName;
    }
    
    //map of gpJobId -> drmJobId
    private BiMap<Integer, String> lookup=HashBiMap.create();
    private BiMap<Integer, DrmJobRecord> lookup2=HashBiMap.create();

    private Map<Integer, JobRunnerJob> detailsTable=new HashMap<Integer, JobRunnerJob>();

    @Override
    public List<DrmJobRecord> getRunningDrmJobRecords() {
        return new ArrayList<DrmJobRecord>(lookup2.values());
    }


    @Override
    public DrmJobRecord lookupJobRecord(Integer gpJobNo) {
        return lookup2.get(gpJobNo);
    }
    

//    @Override
//    public void updateJobStatus(final Integer gpJobNo, final DrmJobStatus drmJobStatus) {
//        JobRunnerJob rowOrig=detailsTable.get(gpJobNo);
//        if (rowOrig==null) {
//            log.error("Row not initialized for gpJobId="+gpJobNo);
//            //TODO: figure out how to initialize the workingDir
//            final File workingDir=new File(""+gpJobNo);
//            rowOrig=new JobRunnerJob.Builder(jobRunnerClassname, workingDir, gpJobNo).drmJobStatus(drmJobStatus).build();
//        }
//        final JobRunnerJob updated=new JobRunnerJob.Builder(rowOrig).drmJobStatus(drmJobStatus).build();
//        if (isSet(updated.getExtJobId())) {
//            lookup.put(updated.getGpJobNo(), updated.getExtJobId());
//            String extJobId=lookup.get(gpJobNo);
//            DrmJobRecord drmJobRecord=new DrmJobRecord.Builder(gpJobNo)
//                .extJobId(extJobId)
//                .build();
//            lookup2.put(updated.getGpJobNo(), drmJobRecord);
//        }
//        detailsTable.put(gpJobNo, updated);
//    }
    
    @Override
    public void updateJobStatus(final DrmJobRecord drmJobRecord, final DrmJobStatus drmJobStatus) {
        final Integer gpJobNo=drmJobRecord.getGpJobNo();
        JobRunnerJob rowOrig=detailsTable.get(gpJobNo);
        if (rowOrig==null) {
            log.error("Row not initialized for gpJobId="+gpJobNo);
            final File workingDir=drmJobRecord.getWorkingDir();
            rowOrig=new JobRunnerJob.Builder(jobRunnerClassname, workingDir, gpJobNo).drmJobStatus(drmJobStatus).build();
        }
        final JobRunnerJob updated=new JobRunnerJob.Builder(rowOrig).drmJobStatus(drmJobStatus).build();
        if (isSet(updated.getExtJobId())) {
            lookup.put(updated.getGpJobNo(), updated.getExtJobId());
            lookup2.put(updated.getGpJobNo(), drmJobRecord);
        }
        detailsTable.put(gpJobNo, updated);
    }



    @Override
    public void insertJobRecord(final DrmJobSubmission jobSubmission) {
        JobRunnerJob row=new JobRunnerJob.Builder(jobRunnerClassname, jobSubmission.getWorkingDir(), jobSubmission.getGpJobNo()).jobRunnerName(jobRunnerName).build();
        detailsTable.put(row.getGpJobNo(), row);
    }


}