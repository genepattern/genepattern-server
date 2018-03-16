package org.genepattern.server.executor.awsbatch;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;

import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.Value;

/**
 * Helper class for filtering AWS S3 files as part of the AWS Batch integration.
 * Initially developed as a workaround to by-pass the AWS S3 push from the GP
 * server head node to an AWS S3 bucket before submitting a job.
 * 
 * Example configuration:
 * <pre>
    default.properties:
        ...
        job.awsbatch.s3-upload-filter: [ "**\/.cache/uploads/cache/datasets.genepattern.org/data/TCGA_BRCA/BRCA_HTSeqCounts{,/**}" ]
 * </pre>
 * 
 * See: https://docs.oracle.com/javase/tutorial/essential/io/fileOps.html#glob
 */
public class AwsS3Filter {
    
    /**
     * job.awsbatch.s3-upload-filter, 
     *   set this to bypass the push to aws s3 from the local file system
     * before submitting the job to AWS Batch.
     */
    public static final String PROP_JOB_AWSBATCH_S3_UPLOAD_FILTER="job.awsbatch.s3-upload-filter";
    public static final Value DEFAULT_JOB_AWSBATCH_S3_UPLOAD_FILTER=null;
    
    public static AwsS3Filter initAwsS3Filter(final GpConfig gpConfig, final GpContext gpContext) {
        //final Value defaultValue=new Value(Arrays.asList(
        //        "**/.cache/uploads/cache/datasets.genepattern.org/data/ccmi_tutorial/2017-12-15{,/**}",
        //        "**/.cache/uploads/cache/datasets.genepattern.org/data/ccmi_tutorial/2018-03-14{,/**}",
        //        "**/.cache/uploads/cache/datasets.genepattern.org/data/TCGA_BRCA/BRCA_HTSeqCounts{,/**}"
        //    ));
        final boolean skipAll=false; // <--- default when no glob pattern is set
        final FileFilter awsS3UploadFilter=GenericFileFilter.initGlobFilter(
            gpConfig, gpContext, PROP_JOB_AWSBATCH_S3_UPLOAD_FILTER, DEFAULT_JOB_AWSBATCH_S3_UPLOAD_FILTER, skipAll);
        return new AwsS3Filter(awsS3UploadFilter);
    }

    private final FileFilter s3UploadFilter;
    
    private AwsS3Filter() {
        this((FileFilter)null);
    }
    private AwsS3Filter(final FileFilter s3UploadFilter) {
        this.s3UploadFilter=s3UploadFilter;
    }
    
    /**
     * Should I skip the push to aws s3 from the local file system before submitting the job?
     * @param inputFile a local file path
     * @return true to skip the s3 upload step for the given file
     */
    public boolean skipS3Upload(final File inputFile) {
        if (s3UploadFilter != null) {
            if (s3UploadFilter.accept(inputFile)) {
                return true;
            }
        }
        return false;
    }

}
