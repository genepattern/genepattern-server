/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.executor.drm;

import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.drm.DrmJobRecord;
import org.genepattern.drm.DrmJobStatus;
import org.genepattern.drm.DrmJobSubmission;

public class DrmLookupFactory {
    private static final Logger log = Logger.getLogger(DrmLookupFactory.class);

    public enum Type {
        HASHMAP,
        DB;
    }

    public static final DrmLookup initializeDrmLookup(final String jobRunnerClassname, final String jobRunnerName) {
        return initializeDrmLookup(Type.HASHMAP, jobRunnerClassname, jobRunnerName);
    }
    
    public static final DrmLookup initializeDrmLookup(final Type type, final String jobRunnerClassname, final String jobRunnerName) {
        log.debug("Initializing drmLookup, type="+type+", jobRunnerClassname="+jobRunnerClassname+", jobRunnerName="+jobRunnerName);
        if (type==Type.HASHMAP) {
            return new HashMapLookup(jobRunnerClassname, jobRunnerName);
        }
        else if (type==Type.DB) {
            return new DbLookup(jobRunnerClassname, jobRunnerName);
        }
        
        log.error("Unspecified DrmLookup Type, creating anonymous default implementation");   
        return new DrmLookup() {
            @Override
            public List<DrmJobRecord> getRunningDrmJobRecords() {
                return Collections.emptyList();
            }

            @Override
            public DrmJobRecord lookupJobRecord(Integer gpJobNo) {
                final String extJobId=""+gpJobNo;
                final String lsid="";
                log.warn("lsid not set");
                return new DrmJobRecord.Builder(gpJobNo, lsid)
                    .extJobId(extJobId)
                    .build();
            }

            @Override
            public void insertJobRecord(DrmJobSubmission jobSubmission) {
                //no-op
            }

            @Override
            public void updateJobStatus(DrmJobRecord drmJobRecord, DrmJobStatus drmJobStatus) {
                //no-op
            }
        };
    }
    
}
