package org.genepattern.server.executor.lsf;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;
import org.genepattern.junitutil.FileUtil;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: nazaire
 * Date: Nov 5, 2012
 * Time: 1:05:28 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestLsfErrorCheckerImpl
{
    final static public Logger log = Logger.getLogger(TestLsfErrorCheckerImpl.class);

    @Before
    public void setUp(){}


    private static File getLsfLogFile(String filename) {
        return FileUtil.getSourceFile(TestLsfErrorCheckerImpl.class, filename);
    }

    private static String getMessage(String filename)
    {
        File file = FileUtil.getSourceFile(TestLsfErrorCheckerImpl.class, filename);

        String message = null;


	    BufferedReader reader = null;
        try
        {
            reader = new BufferedReader(new FileReader(file));
            StringBuffer messageBuffer = new StringBuffer();

            String line;
            while((line = reader.readLine()) != null)
            {
                messageBuffer.append(line);
                messageBuffer.append("\n");
            }

            message = messageBuffer.toString();
        }
        catch(IOException io)
        {
           io.printStackTrace();
        }
        finally
        {
            if(reader != null)
            {
                try{reader.close();} catch(IOException e){};
            }
        }
        return message;
    }

    /**
     * Make sure we can detect an out of memory error from LSF
     */
    @Test
    public void testOutOfMemoryError()
    {
        validateLsfLogResults("memory_limit_lsf.out.txt", "memory_limit_expected_message.txt", 1);        
    }

    /**
     * Make sure we can detect a walltime limit error from LSF
     */
    @Test
    public void testWalltimeError() {
        validateLsfLogResults("walltime_limit_lsf.out.txt", "walltime_limit_expected_message.txt", 134);
    }

    /**
     * Make sure we can detect an out of memory error from LSF
     */
    @Test
    public void testJobAbortByAdmin()
    {
        validateLsfLogResults("bkill_job_lsf.out.txt", "bkill_job_lsf_expected_message.txt", 130);
    }
    

    private void validateLsfLogResults(String logFileName, String expectedMessageFileName, int expectedExitCode)
    {
        final File lsfFile= getLsfLogFile(logFileName);
        Assert.assertNotNull(lsfFile);

        LsfErrorCheckerImpl errorCheck = new LsfErrorCheckerImpl(lsfFile);
        LsfErrorStatus status = errorCheck.getStatus();
        Assert.assertNotNull(status);

        //check that the lsf exit code is 1
        Assert.assertEquals(expectedExitCode, status.getExitCode());

        String expectedMessage = getMessage(expectedMessageFileName);
        Assert.assertNotNull(expectedMessage);

        Assert.assertEquals(expectedMessage,status.getErrorMessage());
    }
}
