package org.genepattern.server.executor.awsbatch;

import java.io.File;
import java.nio.file.Files;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.genepattern.drm.DrmJobRecord;
import org.genepattern.drm.DrmJobSubmission;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.executor.CommandExecutorException;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.webservice.JobInfo;

import com.google.common.base.Strings;

/**
 * Utility methods initially implemented in the AWS Batch integration.
 * Under consideration for the core GenePattern Server code.
 * 
 * @author pcarr
 */
public class AwsBatchUtil {
    private static final Logger log = Logger.getLogger(AwsBatchUtil.class);

    /**
     * Make symbolic link to the target file in the given directory.
     *     ln -s <targetFile> <targetFile.name>
     * @param linkDir
     * @param target
     * @param linkName
     * @throws CommandExecutorException
     */
    protected static void makeSymLink(final File linkDir, final File target, final String linkName) throws CommandExecutorException {
        try {
            Files.createSymbolicLink(
                // link
                linkDir.toPath().resolve(linkName), 
                // target
                target.toPath());
        }
        catch (Throwable t) {
            final String message="Error creating symlink to local input file='"+target+"' in directory='"+linkDir+"'";
            log.error(message, t);
            throw new CommandExecutorException(message, t);
        }
    }

    protected static GpContext initJobContext(final DrmJobRecord jobRecord) {
        JobInfo jobInfo = null;
        if (jobRecord!=null && jobRecord.getGpJobNo() != null) {
            try {
                jobInfo = new AnalysisDAO().getJobInfo(jobRecord.getGpJobNo());
            }
            catch (Throwable t) {
                log.debug("Error initializing jobInfo from db, jobNumber="+jobRecord.getGpJobNo(), t);
            }
        }
        final GpContext jobContext=new GpContext.Builder()
            .jobNumber(jobRecord.getGpJobNo())
            .jobInfo(jobInfo)
        .build();
        return jobContext;
    }

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
