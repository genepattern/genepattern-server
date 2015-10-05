package org.genepattern.drm.impl.local.commons_exec;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.Executor;
import org.genepattern.drm.DrmJobSubmission;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.genepattern.ValueResolver;
import org.genepattern.server.rest.ParameterInfoRecord;
import org.genepattern.webservice.JobInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.common.io.Files;

public class TestLocalCommonsExecJobRunner {
    private GpConfig gpConfig;
    private File jobDir;
    
    private File antHome;
    
    private DrmJobSubmission gpJob;
    
    // for asynchronous capture of command line result
    private Executor exec=null;
    private DefaultExecuteResultHandler resultHandler=null;
    private Integer cmdExitValue=null;
    private ExecuteException cmdException=null;
    
    @Rule
    public TemporaryFolder temp= new TemporaryFolder();
    
    /**
     * helper method, initialize a list of args (by substituting) from the given cmd string.
     * e.g. converts "<ant> -version" into [ "/path/to/ant", "-version" ]
     * @param cmd
     * @return
     */
    protected List<String> parseCmd(final String cmd) {
        GpContext gpContext=GpContext.getServerContext();
        Map<String,String> props=Collections.emptyMap();
        Map<String,ParameterInfoRecord> paramInfoMap=Collections.emptyMap();
        List<String> args=ValueResolver.resolveValue(gpConfig, gpContext, cmd, props, paramInfoMap);
        return args;
    }
    
    /**
     * custom assertion, assert that the actual file contains the expected content.
     * @param message
     * @param expectedContent
     * @param actual
     */
    protected static void assertFileContent(final String message, final String expectedContent, final File actual) {
        assertEquals(""+actual+" exists", true, actual.exists());
        try {
            String actualContent=Files.toString(actual, Charset.forName("UTF-8"));
            assertEquals(message, expectedContent, actualContent);        
        }
        catch (Throwable t) {
            fail("error validating file contents: "+t.getLocalizedMessage());
        }
    }

    /**
     * helper method for this test-case; validate the 'ant -version' output
     * @throws ExecuteException
     */
    protected void assertAntVersion() throws ExecuteException {
        assertExitStatus();

        // validate stdout.txt
        File stdout=new File(jobDir,"stdout.txt");
        assertFileContent("stdout.txt", "Apache Ant(TM) version 1.8.4 compiled on May 22 2012\n", stdout);
    }

    protected void assertExitStatus() throws ExecuteException {
        if (cmdExitValue != null && cmdExitValue != 0) {
            String msg="non-zero exitValue="+cmdExitValue;
            if (cmdException != null) {
                msg+=": "+cmdException.getMessage();
            }
            
            fail(msg);
        }
        if (cmdException != null) {
            throw cmdException;
        }
    }

    @Before
    public void setUp() throws ExecutionException, IOException {
        File webappDir=new File("website").getAbsoluteFile();
        gpConfig=new GpConfig.Builder()
            .addProperty("java", "java")
            .webappDir(webappDir)
        .build();
        antHome=new File(webappDir, "WEB-INF/tools/ant/apache-ant-1.8.4").getAbsoluteFile();
        assertTrue("antHome.exists", antHome.exists()); 
        
        File gpHome=temp.newFolder(".genepattern");
        final int jobNo=1;
        jobDir=new File(gpHome, "jobResults/"+jobNo);
        jobDir.mkdirs();
        assertTrue("jobDir.exists", jobDir.exists());
                
        exec=null;
        cmdExitValue=null;
        cmdException=null;
        resultHandler=new DefaultExecuteResultHandler() {
            @Override
            public void onProcessComplete(int exitValue) {
                cmdExitValue=exitValue;
                super.onProcessComplete(exitValue);
            }

            @Override
            public void onProcessFailed(ExecuteException e) {
                cmdExitValue=e.getExitValue();
                cmdException=e;
                super.onProcessFailed(e);
            }
        };
    }
    
    protected DrmJobSubmission initGpJob(final String[] cmdArgs) {
        return initGpJob(Arrays.asList(cmdArgs));
    }

    protected DrmJobSubmission initGpJob(final List<String> cmdArgs) {
        GpContext jobContext=mock(GpContext.class);
        JobInfo jobInfo=mock(JobInfo.class);
        when(jobContext.getJobInfo()).thenReturn(jobInfo);
        DrmJobSubmission gpJob=new DrmJobSubmission.Builder(jobDir)
            .jobContext(jobContext)
            .commandLine(cmdArgs)
            .stdoutFile(new File("stdout.txt"))
            .stderrFile(new File("stderr.txt"))
        .build();
        return gpJob;
    }
    
    @After
    public void tearDown() {
        if (exec != null && exec.getWatchdog() != null) {
            exec.getWatchdog().stop();
        }
    }
    
    @Test
    public void checkAntVersion_hardCodedPath() throws IOException, ExecutionException, InterruptedException { 
        List<String> antCmd=Arrays.asList("java", "-Dant.home="+antHome, "-cp", antHome+"/lib/ant-launcher.jar", "org.apache.tools.ant.launch.Launcher", "-version");
        gpJob=initGpJob(antCmd);
        exec=LocalCommonsExecJobRunner.runJobNoWait(gpJob, null, resultHandler);
        resultHandler.waitFor();
        assertAntVersion();
    }

    @Test
    public void checkAntVersion_nonNullEnv() throws IOException, ExecutionException, InterruptedException { 
        gpJob=initGpJob(parseCmd("<ant> -version"));
        final Map<String,String>  cmdEnv=new HashMap<String,String>();
        exec=LocalCommonsExecJobRunner.runJobNoWait(gpJob, cmdEnv, resultHandler);
        resultHandler.waitFor();
        assertAntVersion();
    }

    @Test
    public void checkAntVersion_Ant_substitution()  throws IOException, ExecutionException, InterruptedException { 
        gpJob=initGpJob(parseCmd("<ant> -version"));
        exec=LocalCommonsExecJobRunner.runJobNoWait(gpJob, null, resultHandler);
        resultHandler.waitFor();
        assertAntVersion();
    }

    @Test
    public void checkAntVersion_Ant_1_8_substitution()  throws IOException, ExecutionException, InterruptedException { 
        final List<String> args = parseCmd("<ant-1.8> -version"); 
        gpJob=initGpJob(args);
        exec=LocalCommonsExecJobRunner.runJobNoWait(gpJob, null, resultHandler);
        resultHandler.waitFor();
        assertAntVersion();
    }

    @Test
    public void checkAntVersion_Ant_Java_substitution()  throws IOException, ExecutionException, InterruptedException { 
        gpJob=initGpJob(parseCmd("<ant-java> -version"));
        exec=LocalCommonsExecJobRunner.runJobNoWait(gpJob, null, resultHandler);
        resultHandler.waitFor();
        assertAntVersion();
    }

    /**
     * Verify that the required extra libraries are present for the 'ftp' task.
     * Simulate running ant from a plugin type job, see PluginManagerLegecy installPatch.
     * 
     * @throws IOException 
     * @throws ExecuteException 
     * @throws InterruptedException 
     */
    @Test
    public void antFtpTask() throws ExecutionException, IOException, InterruptedException {
        // ftp://gpftp.broadinstitute.org/example_data/gpservertest/DemoFileDropdown/input.file/dummy_file_1.txt
        final String ftpServer="gpftp.broadinstitute.org"; 
        final String ftpRemotedir="example_data/gpservertest/DemoFileDropdown/input.file"; 
        final String ftpFilename="dummy_file_1.txt";
        final File libdir=new File("test/data/taskLib/AntFtp/").getAbsoluteFile();
        final List<String> args = parseCmd("<ant> -f "+libdir+"/build-ftptest.xml "+
                "-Dftp.server="+ftpServer+
                " -Dftp.remotedir="+ftpRemotedir+
                " -Dftp.filename="+ftpFilename);
        gpJob=initGpJob(args);
        assertEquals("gpJob.workingDir", jobDir, gpJob.getWorkingDir());
        exec=LocalCommonsExecJobRunner.runJobNoWait(gpJob, null, resultHandler);
        resultHandler.waitFor();
        assertExitStatus();
        assertFileContent(ftpFilename, 
                "dummy file 1\n", 
                new File(jobDir,ftpFilename));
    }

    @Test(expected=ExecuteException.class)
    public void spaceCharBeforeExecutable() throws ExecuteException, IOException, InterruptedException, ExecutionException { 
        gpJob=initGpJob(parseCmd(" ant -version"));
        exec=LocalCommonsExecJobRunner.runJobNoWait(gpJob, null, resultHandler);
        resultHandler.waitFor();
        //expecting ExecutionException
        if (cmdException != null) {
            throw cmdException;
        }
    }

}
