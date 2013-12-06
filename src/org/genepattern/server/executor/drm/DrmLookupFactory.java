package org.genepattern.server.executor.drm;

import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
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
        if (type==Type.HASHMAP) {
            return new HashMapLookup(jobRunnerClassname, jobRunnerName);
        }
        else if (type==Type.DB) {
            return new DbLookup(jobRunnerClassname, jobRunnerName);
        }
        
        log.error("Unspecified DrmLookup Type, creating anonymous default implementation");   
        return new DrmLookup() {
            @Override
            public List<String> getRunningDrmJobIds() {
                return Collections.emptyList();
            }

            @Override
            public String lookupDrmJobId(final Integer gpJobNo) {
                return ""+gpJobNo;
            }

            @Override
            public Integer lookupGpJobNo(String drmJobId) {
                try {
                    Integer gpJobNo=new Integer(drmJobId);
                    return gpJobNo;
                }
                catch (Throwable t) {
                    log.error("Can't convert drmJobId to gpJobNo, drmJobId="+drmJobId, t);
                    return null;
                }
            }

            @Override
            public void insertJobRecord(DrmJobSubmission jobSubmission) {
                //no-op
            }

            @Override
            public void updateJobStatus(final Integer gpJobNo, final DrmJobStatus drmJobStatus) {
                //no-op
            }

        };
    }
    
}
