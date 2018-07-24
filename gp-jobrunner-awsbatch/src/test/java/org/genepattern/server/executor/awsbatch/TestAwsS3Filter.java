package org.genepattern.server.executor.awsbatch;

import static org.genepattern.server.executor.awsbatch.AwsS3Filter.PROP_JOB_AWSBATCH_S3_UPLOAD_FILTER;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Arrays;

import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.Value;
import org.genepattern.server.executor.awsbatch.testutil.Util;
import org.junit.Test;

public class TestAwsS3Filter {

    /**
     * Load a file from the standard maven location:
     *     gp-jobrunner-awsbatch/src/test/resources
     */
    public File getTestResource(final String filename) throws UnsupportedEncodingException {
        final URL url=getClass().getResource(filename);
        if (url==null) {
            fail("test resource not found: "+filename);
        }
        final File file = new File( URLDecoder.decode( url.getFile(), "UTF-8" ) );
        if (!file.exists()) {
            fail("test resource file doesn't exist: "+file);
        }
        return file;
    }

    /*
     * Example: pre-cache datasets.genepattern.org subdirectory
     */
    final String userRootDir="/opt/gpbeta/gp_home/users";
    final String cachePrefix="/.cache/uploads/cache";
    final String cacheDirPath=userRootDir+cachePrefix;
    final String dataPrefix="/datasets.genepattern.org/data";
    final String dataDirPath=cacheDirPath+dataPrefix;
    final String userUploadPath=userRootDir+"/test-user/uploads";

    // example paths
    final String  brca=dataDirPath+"/TCGA_BRCA/BRCA_HTSeqCounts";
    final String ccmi1=dataDirPath+"/ccmi_tutorial/2017-12-15";
    final String ccmi2=dataDirPath+"/ccmi_tutorial/2018-03-14";

    /** for testing AwsS3Filter.skipS3Upload */
    protected static void assertSkipUpload(final boolean expected, final AwsS3Filter awsS3filter, final File file) {
        assertEquals("skipS3Upload('"+file+"')",
            expected, 
            awsS3filter.skipS3Upload(file));
    }

    /** testing AwsS3Filter with example custom configuration */
    protected void assertCustom(final AwsS3Filter awsS3filter) {
        assertSkipUpload(true, awsS3filter, new File(userRootDir+"/"+brca));
        assertSkipUpload(true, awsS3filter, new File(userRootDir+"/"+ccmi1));
        assertSkipUpload(true, awsS3filter, new File(userRootDir+"/"+ccmi2));

        assertSkipUpload(true, awsS3filter, new File(userRootDir+"/"+brca+"/TCGA_BRCA_SAMPLEINFO.txt"));
        assertSkipUpload(true, awsS3filter, new File(userRootDir+"/"+ccmi1+"/TCGA_BRCA_SAMPLEINFO.txt"));
        assertSkipUpload(true, awsS3filter, new File(userRootDir+"/"+ccmi2+"/TCGA_BRCA_SAMPLEINFO.txt")); 
        
        // parent dir, shouldn't skip
        assertSkipUpload(false, awsS3filter, new File(userRootDir+"/"+dataDirPath));

        // different root dir, shouldn't skip
        final String user_brca=userUploadPath+"/TCGA_BRCA/BRCA_HTSeqCounts";
        assertSkipUpload(false, awsS3filter, new File(user_brca));
        assertSkipUpload(false, awsS3filter, new File(user_brca+"/TCGA_BRCA_SAMPLEINFO.txt"));
    }

    // by default, don't skip any uploads
    @Test
    public void with_default_config() {
        final GpConfig gpConfig=mock(GpConfig.class);
        final GpContext serverContext=GpContext.getServerContext();
        //when(gpConfig.getValue(serverContext, PROP_JOB_AWSBATCH_S3_UPLOAD_FILTER, AwsS3Filter.DEFAULT_JOB_AWSBATCH_S3_UPLOAD_FILTER))
        //  .thenReturn(null);
        final AwsS3Filter awsS3filter=AwsS3Filter.initAwsS3Filter(gpConfig, serverContext);
        
        assertSkipUpload(false, awsS3filter, new File(userRootDir+"/"+brca));
        assertSkipUpload(false, awsS3filter, new File(userRootDir+"/"+brca+"/TCGA_BRCA_SAMPLEINFO.txt"));        
        assertSkipUpload(false, awsS3filter, new File(userRootDir+"/"+dataDirPath));
        final String user_brca=userUploadPath+"/TCGA_BRCA/BRCA_HTSeqCounts";
        assertSkipUpload(false, awsS3filter, new File(user_brca));
        assertSkipUpload(false, awsS3filter, new File(user_brca+"/TCGA_BRCA_SAMPLEINFO.txt"));
    }

    @Test
    public void with_custom_config_from_mock() {
        final Value customValue=new Value(Arrays.asList(
            "**/.cache/uploads/cache/datasets.genepattern.org/data/ccmi_tutorial/2017-12-15{,/**}",
            "**/.cache/uploads/cache/datasets.genepattern.org/data/ccmi_tutorial/2018-03-14{,/**}",
            "**/.cache/uploads/cache/datasets.genepattern.org/data/TCGA_BRCA/BRCA_HTSeqCounts{,/**}"
        ));
        final GpConfig gpConfig=mock(GpConfig.class);
        final GpContext serverContext=GpContext.getServerContext();
        when(gpConfig.getValue(serverContext, PROP_JOB_AWSBATCH_S3_UPLOAD_FILTER, AwsS3Filter.DEFAULT_JOB_AWSBATCH_S3_UPLOAD_FILTER))
          .thenReturn(customValue);
        final AwsS3Filter awsS3filter=AwsS3Filter.initAwsS3Filter(gpConfig, serverContext);
        assertCustom(awsS3filter);
    }

    @Test
    public void with_custom_config_from_yaml() throws Throwable {
        final File configFile=getTestResource("/config_awsbatch_test.yaml");
        final GpConfig gpConfig=Util.initGpConfig(configFile);
        assertNotNull(gpConfig);
        final GpContext serverContext=GpContext.getServerContext();
        final Value s3UploadFilter=gpConfig.getValue(serverContext,PROP_JOB_AWSBATCH_S3_UPLOAD_FILTER);
        assertNotNull("expecting non-null "+PROP_JOB_AWSBATCH_S3_UPLOAD_FILTER, s3UploadFilter); 
        final AwsS3Filter awsS3filter=AwsS3Filter.initAwsS3Filter(gpConfig, serverContext);
        assertCustom(awsS3filter);
    }

}
