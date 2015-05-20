/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.genepattern;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.genepattern.drm.DrmJobSubmission;
import org.genepattern.drm.Memory;
import org.genepattern.junitutil.FileUtil;
import org.genepattern.junitutil.TaskUtil;
import org.genepattern.server.config.GpContext;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.TaskInfo;
import org.junit.Assert;
import org.junit.Test;

/**
 * junit test cases for automatically setting java Xmx flag based on 'job.memory' property.
 * @author pcarr
 *
 */
public class TestCustomXmxFlags {
    private static GpContext createJobContext(final String name, final String cmdLine) {
        final TaskInfo taskInfo=createTask(name, cmdLine);
        final File taskLibDir=new File("taskLib/"+name+".1.0");
        final GpContext taskContext=new GpContext.Builder()
            .taskInfo(taskInfo)
            .taskLibDir(taskLibDir)
            .build();
        return taskContext;
    }

    private static TaskInfo createTask(final String name, final String cmdLine) {
        TaskInfo mockTask=new TaskInfo();
        mockTask.setName(name);
        mockTask.giveTaskInfoAttributes();
        mockTask.getTaskInfoAttributes().put(GPConstants.LSID, "");
        mockTask.getTaskInfoAttributes().put(GPConstants.TASK_TYPE, "Test");
        mockTask.getTaskInfoAttributes().put(GPConstants.COMMAND_LINE, cmdLine);
        return mockTask;
    }    

    final private Memory mem=Memory.fromString("16 Gb");

    /**
     * An example java module which already has an -Xmx flag,
     * replace it with the new value.
     */
    @Test
    public void testReplaceXmx() {
        final GpContext jobContext=createJobContext("DemoJava", "<java> <java_flags> -cp <libdir>DemoJava.jar");
        final File libdir=jobContext.getTaskLibDir();
        final String[] cmdLineArgs={ "java", "-Xmx512m", "-cp", ""+libdir.getAbsolutePath()+"/DemoJava.jar"};
        final String[] expected={ "java", "-Xmx16g", "-cp", ""+libdir.getAbsolutePath()+"/DemoJava.jar"};
        final String[] actual=CustomXmxFlags.addOrReplaceXmxFlag(jobContext, mem, cmdLineArgs);
        Assert.assertArrayEquals(expected, actual);
    }
    
    /**
     * An example java module on a system which defines more than arg in the <java_flags> property.
     * <pre>
     *     java_flags: -Xmx512m -Dhttp.proxyHost=<http.proxyHost> -Dhttp.proxyPort=<http.proxyPort>
     * </pre>
     */
    @Test
    public void testReplaceXmxMultiArgJava_Flags() {
        final GpContext jobContext=createJobContext("DemoJava", "<java> <java_flags> -cp <libdir>DemoJava.jar");
        final File libdir=jobContext.getTaskLibDir();
        final String[] cmdLineArgs={ "java", "-Xmx512m -Dhttp.proxyHost=localhost -Dhttp.proxyPort=9393", "-cp", ""+libdir.getAbsolutePath()+"/DemoJava.jar"};
        final String[] expected={ "java", "-Xmx16g -Dhttp.proxyHost=localhost -Dhttp.proxyPort=9393", "-cp", ""+libdir.getAbsolutePath()+"/DemoJava.jar"};
        final String[] actual=CustomXmxFlags.addOrReplaceXmxFlag(jobContext, mem, cmdLineArgs);
        Assert.assertArrayEquals("No change for pipelines", expected, actual);
    }

    /**
     * An example java module on a system which defines more than arg in the <java_flags> property.
     * <pre>
     *     java_flags: -Xmx512m -Dhttp.proxyHost=<http.proxyHost> -Dhttp.proxyPort=<http.proxyPort>
     * </pre>
     */
    @Test
    public void testReplaceXmxMultiArgJava_Flags_split() {
        final GpContext jobContext=createJobContext("DemoJava", "<java> <java_flags> -cp <libdir>DemoJava.jar");
        final File libdir=jobContext.getTaskLibDir();
        final String[] cmdLineArgs={ "java", "-Xmx512m", "-Dhttp.proxyHost=localhost", "-Dhttp.proxyPort=9393", "-cp", ""+libdir.getAbsolutePath()+"/DemoJava.jar"};
        final String[] expected={ "java", "-Xmx16g", "-Dhttp.proxyHost=localhost", "-Dhttp.proxyPort=9393", "-cp", ""+libdir.getAbsolutePath()+"/DemoJava.jar"};
        final String[] actual=CustomXmxFlags.addOrReplaceXmxFlag(jobContext, mem, cmdLineArgs);
        Assert.assertArrayEquals("No change for pipelines", expected, actual);
    }

    /**
     * An example java module which does not already have an -Xmx flag.
     */
    @Test
    public void testAddXmx() {
        final GpContext jobContext=createJobContext("DemoJava", "<java> -cp <libdir>DemoJava.jar");

        final File libdir=jobContext.getTaskLibDir();
        final String[] cmdLineArgs={ "java", "-cp", ""+libdir.getAbsolutePath()+"/DemoJava.jar"};
        final String[] expected={ "java", "-Xmx16g", "-cp", ""+libdir.getAbsolutePath()+"/DemoJava.jar"};

        final String[] actual=CustomXmxFlags.addOrReplaceXmxFlag(jobContext, mem, cmdLineArgs);
        Assert.assertArrayEquals("No change for pipelines", expected, actual);
    }
    
    @Test
    public void testReplaceXmx01() {
        final GpContext jobContext=createJobContext("DemoJava", "<java> <java_flags> -cp <libdir>DemoJava.jar");

        final File libdir=jobContext.getTaskLibDir();
        final String[] cmdLineArgs={ "java", "-Xmx512m", "-cp", ""+libdir.getAbsolutePath()+"/DemoJava.jar"};
        final String[] expected={ "java", "-Xmx16g", "-cp", ""+libdir.getAbsolutePath()+"/DemoJava.jar"};

        final String[] actual=CustomXmxFlags.addOrReplaceXmxFlag(jobContext, mem, cmdLineArgs);
        Assert.assertArrayEquals("No change for pipelines", expected, actual);
    }
    
    @Test
    public void testGolubPipeline() {
        final File golubZip=FileUtil.getDataFile("modules/Golub.Slonim.1999.Nature.all.aml.pipeline_v2_modules_only.zip");
        final TaskInfo taskInfo=TaskUtil.getTaskInfoFromZip(golubZip);
        final GpContext jobContext=new GpContext.Builder()
            .taskInfo(taskInfo)
            .build();

        final String[] cmdLineArgs=taskInfo.giveTaskInfoAttributes().get("commandLine").split(" ");
        final String[] actual=CustomXmxFlags.addOrReplaceXmxFlag(jobContext, mem, cmdLineArgs);
        Assert.assertArrayEquals("No change for pipelines", cmdLineArgs, actual);
    }
    
    @Test
    public void testNullCmdLine() {
        final GpContext jobContext=createJobContext("DemoJava", null);
        final String[] cmdLineArgs={ };
        final String[] actual=CustomXmxFlags.addOrReplaceXmxFlag(jobContext, mem, cmdLineArgs);
        Assert.assertArrayEquals("null cmdLine", cmdLineArgs, actual);
    }
    
    @Test
    public void testNullTaskInfo() {
        final GpContext jobContext=GpContext.getServerContext();
        final String[] cmdLineArgs={"ant", "install-task" };
        final String[] actual=CustomXmxFlags.addOrReplaceXmxFlag(jobContext, mem, cmdLineArgs);
        Assert.assertArrayEquals("null taskInfo", cmdLineArgs, actual);
    }
    
    @Test
    public void testNullMem() {
        final GpContext jobContext=createJobContext("DemoJava", "<java> -cp <libdir>DemoJava.jar");

        final File libdir=jobContext.getTaskLibDir();
        final String[] cmdLineArgs={ "java", "-cp", ""+libdir.getAbsolutePath()+"/DemoJava.jar"};

        final String[] actual=CustomXmxFlags.addOrReplaceXmxFlag(jobContext, null, cmdLineArgs);
        Assert.assertArrayEquals("No change for pipelines", cmdLineArgs, actual);
    }


    
    @Test
    public void testReplaceXmx_null() {
        Assert.assertEquals("null string", null, CustomXmxFlags.replaceXmx(mem, null));
    }

    @Test
    public void testReplaceXmx_emptyString() {
        Assert.assertEquals("empty string", "", CustomXmxFlags.replaceXmx(mem, ""));
    }

    @Test
    public void testReplaceXmx_completeString() {
        Assert.assertEquals("-Xmx16g", CustomXmxFlags.replaceXmx(mem, "-Xmx1024m"));
    }
    
    @Test
    public void testReplaceXmx_at_beginning() {
        Assert.assertEquals("-Xmx16g -Dhttp.proxyHost=webcache.example.com -Dhttp.proxyPort=5555", 
                CustomXmxFlags.replaceXmx(mem, "-Xmx512m -Dhttp.proxyHost=webcache.example.com -Dhttp.proxyPort=5555"));
    }

    @Test
    public void testReplaceXmx_in_middle() {
        Assert.assertEquals("-Dhttp.proxyHost=webcache.example.com -Xmx16g -Dhttp.proxyPort=5555", 
                CustomXmxFlags.replaceXmx(mem, "-Dhttp.proxyHost=webcache.example.com -Xmx512m -Dhttp.proxyPort=5555"));
    }

    @Test
    public void testReplaceXmx_at_end() {
        Assert.assertEquals("-Dhttp.proxyHost=webcache.example.com -Dhttp.proxyPort=5555 -Xmx16g", 
                CustomXmxFlags.replaceXmx(mem, "-Dhttp.proxyHost=webcache.example.com -Dhttp.proxyPort=5555 -Xmx512m"));
    }

    @Test
    public void testSkipInvalidSpec_empty() {
        final String arg="This is an example of -Xmx happening to be in a string";
        Assert.assertEquals(arg, 
                CustomXmxFlags.replaceXmx(mem, arg));
    }

    @Test
    public void testSkipInvalidSpec_notANumber() {
        final String arg="This is an example of -Xmx=2345 happening to be in a string";
        Assert.assertEquals(arg, 
                CustomXmxFlags.replaceXmx(mem, arg));
    }

    @Test
    public void testGetXmxMem_emptyCmdLine() {
        assertEquals(null, getXmxMem(Arrays.asList("")));
    }

    @Test
    public void testGetXmxMem_NoXmxArg() {
        assertEquals(null, getXmxMem(Arrays.asList("<R2.5_Rscript>", "--version")));
    }

    @Test
    public void testGetXmxMem_512m() {
        assertEquals(Memory.fromString("512m"), getXmxMem(Arrays.asList("java", "-Xmx512m", "-jar", "myApp.jar")));
    }

    @Test
    public void testGetXmxMem_1g() {
        assertEquals(Memory.fromString("1 Gb"), getXmxMem(Arrays.asList("java", "-Xmx1g", "-jar", "myApp.jar")));
    }

    @Test
    public void testGetXmxMem_16g() {
        assertEquals(Memory.fromString("16 Gb"), getXmxMem(Arrays.asList("java", "-Xmx16g", "-jar", "myApp.jar")));
    }
    
    @Test
    public void testPadXmxForIU() {
        // given a command line with an Xmx flag .... 
        //     a) if necessary, change the Xmx flag to be >= 1 Gb
        //     b) return the amount of memory to request of the job queue, padded by 3 Gb
        
        List<String> cmdLineIn=Arrays.asList("java", "-Xmx512m", "-jar", "/mock/libdir/DemoJava.jar");
        DrmJobSubmission job=mock(DrmJobSubmission.class);
        when(job.getCommandLine()).thenReturn(cmdLineIn);
        when(job.getMemory()).thenReturn(Memory.fromString("512m"));

        Memory minXmx=Memory.fromString("1 Gb");
        Memory pad=Memory.fromString("3 Gb");
        List<String> adjustedCmdLine=adjustXmxFlag(job, minXmx);
        Memory xmxMem=getXmxMem(adjustedCmdLine);
        Memory queueMemory=Memory.fromSizeInBytes( xmxMem.getNumBytes() + pad.getNumBytes() );
        assertEquals(Arrays.asList("java", "-Xmx1g", "-jar", "/mock/libdir/DemoJava.jar"), adjustedCmdLine);

        assertEquals("adjustMem", Memory.fromString("4 Gb").getNumBytes(), queueMemory.getNumBytes());
    }
    
    /**
     * Get the adjusted queue memory for the given job.
     * 
     * @param job, the job to run
     * @param minXmx, the minimum allowed xmx memory for the server, e.g. "1g"
     * @param pad, the amount of extra system memory to allocate in addition to the xmx memory
     * @return the adjusted queue memory for the job, this is the amount to request
     */
    protected Memory adjustMem(DrmJobSubmission job, Memory minXmx, Memory pad) {
        List<String> adjustedCmdLine=adjustXmxFlag(job, minXmx);
        Memory queueMem=job.getMemory();
        Memory xmxMem=getXmxMem(adjustedCmdLine);
        if (xmxMem != null) {
            queueMem=Memory.fromSizeInBytes( xmxMem.getNumBytes() + pad.getNumBytes() );
        }
        return queueMem;
    }
    
    /**
     * For the given job, adjust the command line args to set the '-Xmx' java memory flag 
     * to be greater than or equal to the min value.
     * 
     * @param job, the job to run
     * @param minXmx, the minimum Xmx memory value required by the system (e.g. 1 Gb)
     * @return, an adjusted command line to be submitted to the queue
     */
    protected List<String> adjustXmxFlag(DrmJobSubmission job, Memory minXmx) {
        Memory xmxIn=getXmxMem(job);
        if (xmxIn == null) {
            return job.getCommandLine();
        }
        if (xmxIn.getNumBytes() < minXmx.getNumBytes()) {
            // need to edit the Xmx flag
            return replaceXmxFlag(job.getCommandLine(), minXmx);
        }
        return job.getCommandLine();
    }
    
    /**
     * Get the Xmx flag for the job; this is the value set by the Gp server before adjustment.
     * @param job
     * @return the xmx value or null if none set
     */
    protected Memory getXmxMem(DrmJobSubmission job) {
        return getXmxMem(job.getCommandLine());
    }

    /**
     * Get the '-Xmx' flag from the given list of command line arguments.
     * @param cmdLine
     * @return the xmx value or null if none set
     */
    protected Memory getXmxMem(List<String> cmdLine) {
        for(final String arg : cmdLine) {
            if (arg.startsWith("-Xmx")) {
                return Memory.fromString(arg.substring(4));
            }
        }
        return null;
    }
    
    /**
     * Replace the command line with an adjusted java xmx flag
     * @param cmdLineArgsIn, the initial command line
     * @param adjustedXmxMem, the adjusted Xmx value
     * @return the new command line
     */
    protected static List<String> replaceXmxFlag(List<String> cmdLineArgsIn, final Memory adjustedXmxMem) {
        if (adjustedXmxMem==null) {
            return cmdLineArgsIn;
        }
        final List<String> cmdLineArgsOut=new ArrayList<String>();
        for(final String arg : cmdLineArgsIn) {
            if (arg.contains("-Xmx")) {
                //replace existing -Xmx flag
                String updated=CustomXmxFlags.replaceXmx(adjustedXmxMem,arg);
                cmdLineArgsOut.add(updated);
            }
            else {
                cmdLineArgsOut.add(arg);
            }
        }
        return cmdLineArgsOut;
    }    
    
}
