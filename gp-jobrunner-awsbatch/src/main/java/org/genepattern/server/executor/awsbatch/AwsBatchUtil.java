package org.genepattern.server.executor.awsbatch;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.genepattern.drm.DrmJobSubmission;

import com.google.common.base.Strings;

/**
 * Utility methods initially implemented in the AWS Batch integration.
 * Under consideration for the core GenePattern Server code.
 * 
 * @author pcarr
 */
public class AwsBatchUtil {
    private static final Logger log = Logger.getLogger(AwsBatchUtil.class);

    protected static Set<File> getInputFiles(final DrmJobSubmission gpJob) {
        if (log.isDebugEnabled()) {
            log.debug("listing input files for gpJobNo="+gpJob.getGpJobNo()+" ...");
        }
        // linked hash set preserves insertion order
        final Set<File> inputFiles = new LinkedHashSet<File>();
        for(final String localFilePath : gpJob.getJobContext().getLocalFilePaths()) {
            final File file=getInputFile(gpJob, localFilePath);
            if (file != null) {
                inputFiles.add(file);
            }
        }
        return inputFiles;
    }

    // called-by getInputFiles
    protected static File getInputFile(final DrmJobSubmission gpJob, final String localFilePath) {
        log.debug("    localFilePath="+localFilePath);
        if (Strings.isNullOrEmpty(localFilePath)) {
            return null;
        }
        final File file=new File(localFilePath);
        if (file.exists()) {
            return file;
        }
        else {
            log.error("file doesn't exist, for gpJobNo="+gpJob.getGpJobNo()+", localFilePath="+localFilePath);
            return null;
        }
    }

}
