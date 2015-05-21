/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.genepattern;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.genepattern.drm.DrmJobSubmission;
import org.genepattern.drm.JobRunner;
import org.genepattern.drm.Memory;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.junit.Before;
import org.junit.Test;

/**
 * junit test cases for automatically adjusting the java Xmx flag and the queue memory request based on config parameters:
 *     job.javaXmxMin
 *     job.javaXmxPad
 * 
 * Implementation code is included in this test file.
 * 
 * @author pcarr
 *
 */
public class TestPadXmxFlags {
    DrmJobSubmission job;
    GpConfig gpConfig;
    GpContext jobContext;
    
    @Before
    public void setUp() {
        List<String> cmdLineIn=Arrays.asList("java", "-Xmx512m", "-jar", "/mock/libdir/DemoJava.jar");
        job=mock(DrmJobSubmission.class);
        when(job.getCommandLine()).thenReturn(cmdLineIn);
        
        // mock customizations for 'job.javaXmxMin' and 'job.javaXmxPad'
        gpConfig=mock(GpConfig.class);
        jobContext=mock(GpContext.class);
        when(job.getJobContext()).thenReturn(jobContext);
        when(job.getGpConfig()).thenReturn(gpConfig);
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_MEMORY)).thenReturn(Memory.fromString("512 Mb"));
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_JAVA_XMX_MIN)).thenReturn(Memory.fromString("1 Gb"));
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_JAVA_XMX_PAD)).thenReturn(Memory.fromString("3 Gb"));
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
    public void testGetXmxMem_MultiArg() {
        assertEquals(Memory.fromString("16 Gb"), getXmxMem(Arrays.asList("java", "-Xmx16g -Dhttp.proxyHost=localhost -Dhttp.proxyPort=9393", "-jar", "myApp.jar")));
    }

    @Test
    public void testPadXmxForIU() {
        // given a command line with an Xmx flag .... 
        //     a) if necessary, change the Xmx flag to be >= 1 Gb
        //     b) return the amount of memory to request of the job queue, padded by 3 Gb 

        List<String> adjustedCmdLine=adjustXmxFlag(job); // this is the command line that should be submitted to the queue
        Memory queueMemory=getPaddedQueueMemory(job); // this is the memory allocation to be requested of the queue
        assertEquals(Arrays.asList("java", "-Xmx1g", "-jar", "/mock/libdir/DemoJava.jar"), adjustedCmdLine);
        assertEquals("adjustMem", Memory.fromString("4 Gb").getNumBytes(), queueMemory.getNumBytes());
    }
    
    @Test
    public void noConfig() {
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_JAVA_XMX_MIN)).thenReturn(null);
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_JAVA_XMX_PAD)).thenReturn(null);
        
        assertEquals(job.getCommandLine(), adjustXmxFlag(job));
        assertEquals(Memory.fromString("512m"), getPaddedQueueMemory(job));
    }
    
    @Test
    public void noJavaXmxPad() {
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_JAVA_XMX_PAD)).thenReturn(null);
        assertEquals(Arrays.asList("java", "-Xmx1g", "-jar", "/mock/libdir/DemoJava.jar"), adjustXmxFlag(job));
        assertEquals(Memory.fromString("1 Gb"), getPaddedQueueMemory(job));
    }

    @Test
    public void noJavaXmxMin() {
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_JAVA_XMX_MIN)).thenReturn(null);
        assertEquals(Arrays.asList("java", "-Xmx512m", "-jar", "/mock/libdir/DemoJava.jar"), adjustXmxFlag(job));
        assertEquals(Memory.fromString("3.5 Gb"), getPaddedQueueMemory(job));
    }
    
    /**
     * Get the padded queue memory for the given job, based on config_yaml settings:
     * 
     * For example,
     *     job.javaXmxMin: 1 Gb
     *     job.javaXmxPad: 3 Gb
     * 
     * @param job, the job to run
     * @return the adjusted queue memory for the job, this is the amount to request for a node on the job queue
     */
    protected Memory getPaddedQueueMemory(DrmJobSubmission job) {
        Memory javaXmxMin=job.getGpConfig().getGPMemoryProperty(job.getJobContext(), JobRunner.PROP_JAVA_XMX_MIN);
        Memory javaXmxPad=job.getGpConfig().getGPMemoryProperty(job.getJobContext(), JobRunner.PROP_JAVA_XMX_PAD);
        return getPaddedQueueMemory(job, javaXmxMin, javaXmxPad);
    }
    
    /**
     * Get the padded queue memory for the given job.
     * 
     * @param job, the job to run
     * @param javaXmxMin, the minimum allowed xmx memory for the server.
     * @param javaXmxPad, the amount of extra system memory to allocate in addition to the javaXmxFlag memory
     * @return the adjusted queue memory for the job, this is the amount to request
     */
    protected Memory getPaddedQueueMemory(DrmJobSubmission job, Memory javaXmxMin, Memory javaXmxPad) {
        List<String> cmdLine;
        if (javaXmxMin==null) {
            // by default, don't adjust the -Xmx flag
            cmdLine=job.getCommandLine();
        }
        else {
            // when javaXmxMin is set, adjust the command line
            cmdLine=adjustXmxFlag(job, javaXmxMin);
        }

        // by default, use the job.memory from the config
        Memory queueMem=job.getGpConfig().getGPMemoryProperty(job.getJobContext(), JobRunner.PROP_MEMORY);
        Memory xmxMem=getXmxMem(cmdLine);
        if (javaXmxPad != null) {
            if (xmxMem != null) {
                // pad the queue memory when there is an Xmx arg and job.javaXmxPad is set
                queueMem=Memory.fromSizeInBytes( xmxMem.getNumBytes() + javaXmxPad.getNumBytes() );
            }
        }
        else if (xmxMem != null) {
            //special-case, adjust queueMem >= javaXmx
            if (xmxMem.getNumBytes() > queueMem.getNumBytes()) {
                queueMem=xmxMem;
            }
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
    protected List<String> adjustXmxFlag(DrmJobSubmission job) {
        Memory javaXmxMin=job.getGpConfig().getGPMemoryProperty(job.getJobContext(), JobRunner.PROP_JAVA_XMX_MIN);
        return adjustXmxFlag(job, javaXmxMin);
    }

    protected List<String> adjustXmxFlag(DrmJobSubmission job, Memory javaXmxMin) {
        if (javaXmxMin==null) {
            return job.getCommandLine();
        }
        Memory xmxFromCmdLine=getXmxMem(job);
        if (xmxFromCmdLine != null && xmxFromCmdLine.getNumBytes() < javaXmxMin.getNumBytes()) {
            // increase the Xmx flag
            return replaceXmxFlag(job.getCommandLine(), javaXmxMin);
        }
        else {
            return job.getCommandLine();
        }
    }
    
    /**
     * Get the Xmx flag from job's the command line; before adjusting based on the optional 'job.javaXmxMin' setting. 
     * @param job
     * @return the xmx value or null if none set
     */
    protected Memory getXmxMem(DrmJobSubmission job) {
        return getXmxMem(job.getCommandLine());
    }

    protected Memory getXmxMem(List<String> cmdLine) {
        return getXmxMem_matchAnywhere(cmdLine);
    }
    
    /**
     * Get the Xmx flag from the list of command line args
     * @param job
     * @return the xmx value or null if none set
     */
    protected Memory getXmxMem_matchFirst(List<String> cmdLine) {
        for(final String arg : cmdLine) {
            if (arg.startsWith("-Xmx")) {
                return Memory.fromString(arg.substring(4));
            }
        }
        return null;
    }

    protected Memory getXmxMem_matchAnywhere(List<String> cmdLine) {
        for(final String arg : cmdLine) {
            Memory xmx=getXmxMem_fromArg(arg);
            if (xmx != null) {
                return xmx;
                
            }
        }
        return null;
    }

    public static Memory getXmxMem_fromArg(final String arg) {
        final String XMX="-Xmx";
        if (arg==null) {
            return null;
        }
        final int i0=arg.indexOf(XMX);
        if (i0<0) {
            //no match
            return null;
        }
        String xmxVal="";
        String tail="";
        final int i1=arg.indexOf(" ", i0);
        if (i1>=0) {
            xmxVal=arg.substring(i0+XMX.length(),i1);
            tail=arg.substring(i1);
        }
        else {
            xmxVal=arg.substring(i0+XMX.length());
        }
        try {
            Memory memOrig=Memory.fromString(xmxVal);
            return memOrig;
        }
        catch (Throwable t) {
            //ignore
            return null;
        }
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
