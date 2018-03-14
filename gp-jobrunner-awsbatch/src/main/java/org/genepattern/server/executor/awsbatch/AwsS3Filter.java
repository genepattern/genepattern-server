package org.genepattern.server.executor.awsbatch;

import java.io.File;

/**
 * Helper class for filtering AWS S3 files as part of the AWS Batch integration.
 * Initially developed as a workaround to by-pass the AWS S3 push from the GP
 * server head node to an AWS S3 bucket before submitting a job.
 * 
 * @author pcarr
 *
 */
public class AwsS3Filter {
    
    /**
     * job.awsbatch.s3-filename-filter
     * Example:
     * <pre>
           job.awsbatch.s3-filename-filter: [ " \*\/.cache/uploads/cache/datasets.genepattern.org/data/TCGA_BRCA/BRCA_HTSeqCounts/*" ]
     * </pre>
     */
    public static final String PROP_="job.awsbatch.s3-filefilter";

    /**
     * Should I skip pushing the inputFile from the gp server head node into an S3 bucket?
     * @param inputFile a local file path
     * @return true to skip the s3 upload
     * 
     * TODO: make this a configurable property, similar to job.FilenameFilter.
     * <pre>
           job.awsbatch.s3-skip-filename-filter: [ " \*\/.cache/uploads/cache/datasets.genepattern.org/data/TCGA_BRCA/BRCA_HTSeqCounts/*" ]
     * </pre>
     */
    public static boolean skipS3Upload(final File inputFile) {
        // should I skip the push to s3
        if (inputFile.getPath().contains("/.cache/uploads/cache/datasets.genepattern.org/data/TCGA_BRCA/BRCA_HTSeqCounts/")) {
            return true;
        }
        return false;
    }

}
