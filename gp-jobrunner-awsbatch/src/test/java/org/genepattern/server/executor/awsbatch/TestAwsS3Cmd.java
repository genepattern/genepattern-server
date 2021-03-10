package org.genepattern.server.executor.awsbatch;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Arrays;

import org.genepattern.server.executor.awsbatch.testutil.Util;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.hamcrest.CoreMatchers;

public class TestAwsS3Cmd {

    private static File localDir;
    private static File localFile;
    
    private String s3_prefix="s3://example.com";
    private AwsS3Cmd s3Cmd;

    @BeforeClass
    public static void beforeClass() {
        localDir=new File(Util.getDataDir(), "all_aml");
        localFile=new File(localDir, "all_aml_test.cls");
    }
    
    @Before
    public void setUp() {
        s3Cmd=new AwsS3Cmd.Builder()
            .s3_bucket(s3_prefix)
        .build();
    }

    @Test
    public void sync_fromFile_destPrefix_isSet() {
        assertEquals("sanity check, localDir.exists", true, localDir.exists());
        assertEquals("sanity check, localFile.exists", true, localFile.exists());
        
        final String dest_prefix="/local";
        String[] expected=new String[] {
            "s3", "sync", s3_prefix+""+localFile.getParent(), dest_prefix+""+localFile.getParent(), "--exclude", "*", "--include", localFile.getName()
        };
        assertThat("syncFromS3Args", 
            s3Cmd.getSyncFromS3Args(localFile, dest_prefix, "ignore"), 
            CoreMatchers.is( Arrays.asList(expected) ));
    }
    @Test
    public void sync_fromFile_destPrefix_isNull() {
        assertEquals("sanity check, localDir.exists", true, localDir.exists());
        assertEquals("sanity check, localFile.exists", true, localFile.exists());
        
        final String dest_prefix=null;
        String[] expected=new String[] {
            "s3", "sync", s3_prefix+""+localFile.getParent(), localFile.getParent(), "--exclude", "*", "--include", localFile.getName()
        };
        assertThat("syncFromS3Args", 
            s3Cmd.getSyncFromS3Args(localFile, dest_prefix, "ignore"), 
            CoreMatchers.is( Arrays.asList(expected) ));
    }

    @Test
    public void sync_fromFile_destPrefix_isEmpty() {
        assertEquals("sanity check, localDir.exists", true, localDir.exists());
        assertEquals("sanity check, localFile.exists", true, localFile.exists());
        
        final String dest_prefix="";
        String[] expected=new String[] {
            "s3", "sync", s3_prefix+""+localFile.getParent(), localFile.getParent(), "--exclude", "*", "--include", localFile.getName()
        };
        assertThat("syncFromS3Args", 
            s3Cmd.getSyncFromS3Args(localFile, dest_prefix, "ignore"), 
            CoreMatchers.is( Arrays.asList(expected) ));
    }

    @Test
    public void initWithEmptyEnv() {
        final AwsS3Cmd s3Cmd=new AwsS3Cmd.Builder()
        .build();
        assertNotNull("sanity check", s3Cmd);
    }
    
}
