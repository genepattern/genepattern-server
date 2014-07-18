package org.genepattern.server.executor.lsf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.genepattern.drm.DrmJobState;
import org.genepattern.junitutil.FileUtil;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by IntelliJ IDEA.
 * User: nazaire
 * Date: Nov 5, 2012
 * Time: 1:05:28 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestLsfErrorCheckerImpl {

    private static String getExpectedMessageFromFilename(String filename) throws IOException {
        File file = FileUtil.getSourceFile(TestLsfErrorCheckerImpl.class, filename);
        String message = null;
	    BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            StringBuffer messageBuffer = new StringBuffer();

            String line;
            while((line = reader.readLine()) != null) {
                messageBuffer.append(line);
                messageBuffer.append("\n");
            }

            message = messageBuffer.toString();
        }
        finally {
            if (reader != null) {
                reader.close();
            }
        }
        return message;
    }

    /**
     * Make sure we can detect an out of memory error from LSF
     */
    @Test
    public void outOfMemoryError() throws IOException {
        File lsfLogFile=FileUtil.getSourceFile(this.getClass(), "memory_limit_lsf.out.txt");
        LsfErrorStatus lsfStatus = new LsfErrorCheckerImpl(lsfLogFile).getStatus();
        Assert.assertNotNull("Expecting non-null lsfStatus", lsfStatus);
        Assert.assertEquals("exitCode", 1, lsfStatus.getExitCode());
        
        //String expectedMessage="TERM_MEMLIMIT: job killed after reaching LSF memory usage limit.\n"+
        //        "Exited with exit code 1.";
        String expectedMessage=getExpectedMessageFromFilename("memory_limit_expected_message.txt");

        Assert.assertEquals("errorMessage", expectedMessage, lsfStatus.getErrorMessage());
        Assert.assertEquals("jobState", DrmJobState.TERM_MEMLIMIT, lsfStatus.getJobState());
    }

    /**
     * Make sure we can detect a walltime limit error from LSF
     */
    @Test
    public void walltimeError() throws IOException {
        File lsfLogFile=FileUtil.getSourceFile(this.getClass(), "walltime_limit_lsf.out.txt");
        LsfErrorStatus lsfStatus = new LsfErrorCheckerImpl(lsfLogFile).getStatus();
        Assert.assertNotNull("Expecting non-null lsfStatus", lsfStatus);
        Assert.assertEquals("exitCode", 134, lsfStatus.getExitCode());
        
        //String expectedMessage="TERM_RUNLIMIT: job killed after reaching LSF run time limit.\n"+
        //        "Exited with exit code 134.";
        String expectedMessage=getExpectedMessageFromFilename("walltime_limit_expected_message.txt");
        Assert.assertEquals("errorMessage", expectedMessage, lsfStatus.getErrorMessage());
        Assert.assertEquals("jobState", DrmJobState.TERM_RUNLIMIT, lsfStatus.getJobState());
    }

    /**
     * Make sure we can detect an out of memory error from LSF
     */
    @Test
    public void cancelled() throws IOException {
        File lsfLogFile=FileUtil.getSourceFile(this.getClass(), "bkill_job_lsf.out.txt");
        LsfErrorStatus lsfStatus = new LsfErrorCheckerImpl(lsfLogFile).getStatus();
        Assert.assertNotNull("Expecting non-null lsfStatus", lsfStatus);
        Assert.assertEquals("exitCode", 130, lsfStatus.getExitCode());
        
        //String expectedMessage="TERM_OWNER: job killed by owner.\n"+
        //        "Exited with exit code 130.";
        String expectedMessage=getExpectedMessageFromFilename("bkill_job_lsf_expected_message.txt");
        Assert.assertEquals("errorMessage", expectedMessage, lsfStatus.getErrorMessage());
        Assert.assertEquals("jobState", DrmJobState.CANCELLED, lsfStatus.getJobState());
    }
    
    @Test
    public void completedSuccessfully() {
        File lsfLogFile=FileUtil.getSourceFile(this.getClass(), "completed_job.lsf.out");
        LsfErrorStatus lsfStatus = new LsfErrorCheckerImpl(lsfLogFile).getStatus();
        Assert.assertNotNull("Expecting non-null lsfStatus", lsfStatus);
        Assert.assertEquals("exitCode", 0, lsfStatus.getExitCode());
        Assert.assertEquals("errorMessage", "Successfully completed.", lsfStatus.getErrorMessage());
        Assert.assertEquals("jobState", DrmJobState.DONE, lsfStatus.getJobState());
    }
    
    @Test
    public void nonZeroExitCode() {
        File lsfLogFile=FileUtil.getSourceFile(this.getClass(), "non_zero_exit_code.lsf.out");
        LsfErrorStatus lsfStatus = new LsfErrorCheckerImpl(lsfLogFile).getStatus();
        Assert.assertEquals("exitCode", 136, lsfStatus.getExitCode());
        Assert.assertEquals("errorMessage", "Exited with exit code 136.", lsfStatus.getErrorMessage());
        Assert.assertEquals("jobState", DrmJobState.FAILED, lsfStatus.getJobState());
    }
}
