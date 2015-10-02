/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.genepattern;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Arrays;

import org.genepattern.drm.DrmJobSubmission;
import org.genepattern.drm.JobRunner;
import org.genepattern.drm.Memory;
import org.genepattern.junitutil.FileUtil;
import org.genepattern.junitutil.TaskUtil;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.TaskInfo;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * junit test cases for automatically setting java Xmx flag based on 'job.memory' property.
 * @author pcarr
 *
 */
public class TestCustomXmxFlags {
    // mock commandLine= in manifest file
    final String javaCmdLine="<java> <java_flags> -jar <libdir>DemoJava.jar";
    // mock runtime command array, after substitutions, before applying -Xmx customization
    final String[] javaCmdArgs={"java", "-Xmx512m", "-jar", "/mock/libdir/DemoJava.jar"};
    // mock <java> wrapper script
    final String[] javaWrapperCmdArgs={ 
            "/opt/genepattern/resources/wrapper-scripts/run-with-env.sh",
            "-c", "env-custom-broad-centos5.sh", 
            "-u", "Java", "java", 
            "-jar", "/mock/libdir/DemoJava.jar" 
    };
    
    DrmJobSubmission job;
    GpConfig gpConfig;
    GpContext jobContext;
    final private Memory mem=Memory.fromString("16 Gb");
    
    @Before
    public void setUp() {
        job=mock(DrmJobSubmission.class);
        gpConfig=mock(GpConfig.class);
        
        // example mock customizations ... 
        //when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_MEMORY)).thenReturn(Memory.fromString("512 Mb"));
        //when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_JAVA_XMX)).thenReturn("256 Mb");
        //when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_JAVA_XMX_MIN)).thenReturn(Memory.fromString("1 Gb"));
        //when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_JAVA_XMX_PAD)).thenReturn(Memory.fromString("3 Gb"));
    }
    
    protected void initContext(final String moduleCmdLine, final String[] jobCmdLineArgs) {
        when(job.getCommandLine()).thenReturn(Arrays.asList(jobCmdLineArgs));
        jobContext=mockJobContext("DemoModule", moduleCmdLine);
        when(job.getJobContext()).thenReturn(jobContext);
        when(job.getGpConfig()).thenReturn(gpConfig);
    }
    
    protected static GpContext mockJobContext(String taskName, String taskCommandLine) {
        GpContext jobContext=mock(GpContext.class);
        TaskInfo taskInfo = createTask(taskName, taskCommandLine);
        when(jobContext.getTaskInfo()).thenReturn(taskInfo);
        return jobContext;
    }

    protected static TaskInfo createTask(final String name, final String cmdLine) {
        TaskInfo mockTask=new TaskInfo();
        mockTask.setName(name);
        mockTask.giveTaskInfoAttributes();
        mockTask.getTaskInfoAttributes().put(GPConstants.LSID, "");
        mockTask.getTaskInfoAttributes().put(GPConstants.TASK_TYPE, "Test");
        mockTask.getTaskInfoAttributes().put(GPConstants.COMMAND_LINE, cmdLine);
        return mockTask;
    }    

    @Test
    public void replaceXmx_from_jobMemory() {
        initContext(javaCmdLine, javaCmdArgs);
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_MEMORY)).thenReturn(Memory.fromString("16 Gb"));
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_JAVA_XMX)).thenReturn(null);
        assertArrayEquals(
            // expected
            new String[]{ "java", "-Xmx16g", "-jar", "/mock/libdir/DemoJava.jar"}, 
            // actual
            CustomXmxFlags.addOrReplaceXmxFlag(gpConfig, jobContext, 
                    new String[] {"java", "-jar", "/mock/libdir/DemoJava.jar"}));
    }
    
    /**
     * An example java module which does not already have an -Xmx flag.
     * no -Xmx on cmd line
     */
    @Test
    public void addXmx_from_jobMemory() {
        initContext(javaCmdLine, javaCmdArgs);
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_MEMORY)).thenReturn(Memory.fromString("16 Gb"));
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_JAVA_XMX)).thenReturn(null);
        assertArrayEquals(
            // expected
            new String[]{ "java", "-Xmx16g", "-jar", "/mock/libdir/DemoJava.jar"}, 
            // actual
            CustomXmxFlags.addOrReplaceXmxFlag(gpConfig, jobContext, 
                    new String[] {"java", "-jar", "/mock/libdir/DemoJava.jar"}));
    }

    @Test
    public void addXmx_from_jobMemory_xmx_takes_precedence() {
        initContext(javaCmdLine, javaCmdArgs);
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_MEMORY)).thenReturn(Memory.fromString("12 Gb"));
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_JAVA_XMX)).thenReturn(Memory.fromString("16 Gb"));
        assertArrayEquals(
            // expected
            new String[]{ "java", "-Xmx16g", "-jar", "/mock/libdir/DemoJava.jar"}, 
            // actual
            CustomXmxFlags.addOrReplaceXmxFlag(gpConfig, jobContext, 
                    new String[] {"java", "-jar", "/mock/libdir/DemoJava.jar"}));
    }
    
    /** use 'job.javaXmxMin' when 'job.memory' and 'job.javaXmx' are not set */
    @Test
    public void padXmx_useMin_when_not_set() {
        initContext(javaCmdLine, javaCmdArgs);
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_JAVA_XMX_MIN)).thenReturn(Memory.fromString("1 Gb"));
        assertArrayEquals(
            // expected
            new String[]{ "java", "-Xmx1g", "-jar", "/mock/libdir/DemoJava.jar"}, 
            // actual
            CustomXmxFlags.addOrReplaceXmxFlag(gpConfig, jobContext, 
                    new String[] {"java", "-Xmx512m", "-jar", "/mock/libdir/DemoJava.jar"}));
    }

    /** use 'job.javaXmxMin' when 'job.memory' is not set and 'job.javaXmx' is less than 'job.javaXmxMin' */
    @Test
    public void padXmx_useMin_when_javaXmx_is_less() {
        initContext(javaCmdLine, javaCmdArgs);
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_JAVA_XMX)).thenReturn(Memory.fromString("256m"));
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_JAVA_XMX_MIN)).thenReturn(Memory.fromString("1 Gb"));
        assertArrayEquals(
            // expected
            new String[]{ "java", "-Xmx1g", "-jar", "/mock/libdir/DemoJava.jar"}, 
            // actual
            CustomXmxFlags.addOrReplaceXmxFlag(gpConfig, jobContext, 
                    new String[] {"java", "-Xmx512m", "-jar", "/mock/libdir/DemoJava.jar"}));
    }

    /** use 'job.javaXmxMin' when 'job.memory' is less than 'job.javaXmxMin' and 'job.javaXmx' is not set */
    @Test
    public void padXmx_useMin_when_jobMemory_is_less() {
        initContext(javaCmdLine, javaCmdArgs);
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_MEMORY)).thenReturn(Memory.fromString("256m"));
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_JAVA_XMX)).thenReturn(null);
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_JAVA_XMX_MIN)).thenReturn(Memory.fromString("1 Gb"));
        assertArrayEquals(
            // expected
            new String[]{ "java", "-Xmx1g", "-jar", "/mock/libdir/DemoJava.jar"}, 
            // actual
            CustomXmxFlags.addOrReplaceXmxFlag(gpConfig, jobContext, 
                    new String[] {"java", "-Xmx512m", "-jar", "/mock/libdir/DemoJava.jar"}));
    }
    
    @Test
    public void padXmx_ignoreMin() {
        initContext(javaCmdLine, javaCmdArgs);
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_MEMORY)).thenReturn(Memory.fromString("1 Gb"));
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_JAVA_XMX)).thenReturn(null);
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_JAVA_XMX_MIN)).thenReturn(Memory.fromString("256m"));
        assertArrayEquals(
            // expected
            new String[]{ "java", "-Xmx1g", "-jar", "/mock/libdir/DemoJava.jar"}, 
            // actual
            CustomXmxFlags.addOrReplaceXmxFlag(gpConfig, jobContext, 
                    new String[] {"java", "-Xmx512m", "-jar", "/mock/libdir/DemoJava.jar"}));
    }
    
    /** when no memory config flags; use the value from the command line */
    @Test
    public void padXmx_none() {
        initContext(javaCmdLine, javaCmdArgs);
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_MEMORY)).thenReturn(null);
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_JAVA_XMX)).thenReturn(null);
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_JAVA_XMX_MIN)).thenReturn(null);
        assertArrayEquals(
            // expected
            new String[]{ "java", "-Xmx512m", "-jar", "/mock/libdir/DemoJava.jar"}, 
            // actual
            CustomXmxFlags.addOrReplaceXmxFlag(gpConfig, jobContext, 
                    new String[] {"java", "-Xmx512m", "-jar", "/mock/libdir/DemoJava.jar"}));
    }

    /**
     * An example java module on a system which defines more than one arg in the <java_flags> property.
     * <pre>
     *     java_flags: -Xmx512m -Dhttp.proxyHost=<http.proxyHost> -Dhttp.proxyPort=<http.proxyPort>
     * </pre>
     */
    @Test
    public void padXmx_MultiArgJava_Flags() {
        initContext(javaCmdLine, javaCmdArgs);
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_MEMORY)).thenReturn(mem);
        assertArrayEquals(
            // expected
            new String[]{ "java", "-Xmx16g -Dhttp.proxyHost=localhost -Dhttp.proxyPort=9393", "-jar", "/mock/libdir/DemoJava.jar"}, 
            // actual
            CustomXmxFlags.addOrReplaceXmxFlag(gpConfig, jobContext, 
                    new String[] {"java", "-Xmx512m -Dhttp.proxyHost=localhost -Dhttp.proxyPort=9393", "-jar", "/mock/libdir/DemoJava.jar"}));
    }

    /**
     * An example java module on a system which defines more than one arg in the <java_flags> property.
     * <pre>
     *     java_flags: -Xmx512m -Dhttp.proxyHost=<http.proxyHost> -Dhttp.proxyPort=<http.proxyPort>
     * </pre>
     */
    @Test
    public void padXmx_MultiArgJava_Flags_Split() {
        initContext(javaCmdLine, javaCmdArgs);
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_MEMORY)).thenReturn(mem);
        assertArrayEquals(
            // expected
            new String[]{ "java", "-Xmx16g", "-Dhttp.proxyHost=localhost", "-Dhttp.proxyPort=9393", "-jar", "/mock/libdir/DemoJava.jar"}, 
            // actual
            CustomXmxFlags.addOrReplaceXmxFlag(gpConfig, jobContext, 
                    new String[] {"java", "-Xmx512m", "-Dhttp.proxyHost=localhost", "-Dhttp.proxyPort=9393", "-jar", "/mock/libdir/DemoJava.jar"}));
    }

    

    @Test
    public void setXmx_from_jobJavaXmx() {
        initContext(javaCmdLine, javaCmdArgs);
        
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_MEMORY)).thenReturn(null);
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_JAVA_XMX)).thenReturn(Memory.fromString("16 Gb"));
        final String[] expected={ "java", "-Xmx16g", "-jar", "/mock/libdir/DemoJava.jar"};
        final String[] actual=CustomXmxFlags.addOrReplaceXmxFlag(gpConfig, jobContext, new String[] {"java", "-Xmx512m", "-jar", "/mock/libdir/DemoJava.jar"});
        assertArrayEquals(expected, actual);
    }
    
    /**
     * Special-case, make sure -Xmx flags are added for <R2.5> modules, such as CART.
     * This particular test is similar to the config on GP@IndianaU:
     * 
     * R2.5=<R2.5_Rjava>
     * R2.5_Rjava=<run-rjava> "2.5" <java_flags> -cp <run_r_path> RunR
     * run-rjava=<wrapper-scripts>/run-rjava.sh
     * wrapper-scripts=/fq/path/to/wrapper_scripts
     * 
     * The '<java_flags>' substitution is included in the command line.
     * 
     */
    @Test
    public void setXmx_from_jobMemory_R_2_5_module_CART_At_IndianaU() {
        String rCmdLine="<R2.5> <libdir>cart.R cartCmdLine";
        String[] rCmdArgs={"/mock/wrapper_scripts/run-rjava.sh", "2.5", 
                "-Xmx512m", "-Dhttp.proxyHost=", "-Dhttp.proxyPort=", 
                "-cp", "/mock/rjava_classes", "RunR", "/mock/libdir/DemoModule/cart.R", "cartCmdLine"};
        initContext(rCmdLine, rCmdArgs); 
        
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_MEMORY)).thenReturn(Memory.fromString("16 Gb"));
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_JAVA_XMX)).thenReturn(null);
        final String[] expected= {"/mock/wrapper_scripts/run-rjava.sh", "2.5", "-Xmx16g", "-Dhttp.proxyHost=", "-Dhttp.proxyPort=", "-cp", "/mock/rjava_classes", "RunR", "/mock/libdir/DemoModule/cart.R", "cartCmdLine" };

        final String[] actual=CustomXmxFlags.addOrReplaceXmxFlag(gpConfig, jobContext, rCmdArgs);
        Assert.assertArrayEquals(expected, actual);
    }

    @Test
    public void setXmx_from_jobMemory_R_2_5_Rjava() {
        String rCmdLine="<R2.5_Rjava> <libdir>cart.R cartCmdLine";
        String[] rCmdArgs={"/mock/r_2.5_Rjava.sh", "-Xmx512m", "/mock/libdir/DemoModule/cart.R", "cartCmdLine"};
        initContext(rCmdLine, rCmdArgs); 
        
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_MEMORY)).thenReturn(Memory.fromString("16 Gb"));
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_JAVA_XMX)).thenReturn(null);
        final String[] expected= {"/mock/r_2.5_Rjava.sh", "-Xmx16g", "/mock/libdir/DemoModule/cart.R", "cartCmdLine" };

        final String[] actual=CustomXmxFlags.addOrReplaceXmxFlag(gpConfig, jobContext, rCmdArgs);
        Assert.assertArrayEquals(expected, actual);
    }

    @Test
    public void testIsJavaCmd() {
        assertEquals("isJavaCmd(<java> ...)", true, CustomXmxFlags.isJavaCmd("<java> <java_flags> -jar <libdir>main.jar <arg>"));
    }
    
    @Test
    public void testIsJavaCmd_java_with_version() {
        assertEquals("isJavaCmd(<java_1.6> ...)", true, CustomXmxFlags.isJavaCmd("<java_1.6> <java_flags> -jar <libdir>main.jar <arg>"));
    }

    @Test
    public void testIsJavaCmd_java_no_substitution() {
        assertEquals("isJavaCmd(java)", true, CustomXmxFlags.isJavaCmd("java"));
    }

    @Test
    public void testIsJavaCmd_R2_5_Rjava() {
        assertEquals(true, CustomXmxFlags.isJavaCmd("<R2.5_Rjava> <libdir>cart.R cartCmdLine"));;
    }

    @Test
    public void testIsJavaCmd_R() {
        assertEquals("isJavaCmd(<R> ...)", true, CustomXmxFlags.isJavaCmd("<R> <libdir>main.R <arg>"));
    }

    @Test
    public void testIsJavaCmd_R2_5() {
        assertEquals("isJavaCmd(<R2.5> ...)", true, CustomXmxFlags.isJavaCmd("<R2.5> <libdir>main.R <arg>"));
    }

    @Test
    public void testIsJavaCmd_R2_7() {
        assertEquals("isJavaCmd(<R2.7> ...)", true, CustomXmxFlags.isJavaCmd("<R2.7> <libdir>main.R <arg>"));
    }

    @Test
    public void testIsJavaCmd_R2_13() {
        assertEquals("isJavaCmd(<R2.13> ...)", true, CustomXmxFlags.isJavaCmd("<R2.13> <libdir>main.R <arg>"));
    }

    @Test
    public void testIsJavaCmd_R2_14() {
        // R2.14 uses Rscript instead of the java wrapper
        assertEquals("isJavaCmd(<R2.14> ...)", false, CustomXmxFlags.isJavaCmd("<R2.14> <libdir>main.R <arg>"));
    }

    @Test
    public void testIsJavaCmd_R3_1_Rjava() {
        assertEquals("isJavaCmd(<R3.1_Rjava> ...)", true, CustomXmxFlags.isJavaCmd("<R3.1_Rjava> <libdir>main.R <arg>"));
    }
    
    @Test
    public void testIsJavaCmd_pipeline() {
        final File golubZip=FileUtil.getDataFile("modules/Golub.Slonim.1999.Nature.all.aml.pipeline_v2_modules_only.zip");
        final TaskInfo taskInfo=TaskUtil.getTaskInfoFromZip(golubZip);
        final GpContext jobContext=new GpContext.Builder()
            .taskInfo(taskInfo)
            .build();
        assertEquals("isJavaCmd(taskInfo is pipeline)", false, CustomXmxFlags.isJavaCmd(jobContext));
    }
    
    @Test
    public void testIsJavaCmd_null() {
        assertEquals("isJavaCmd(null cmdLine String ...)", false, CustomXmxFlags.isJavaCmd((String)null));
    }
    
    @Test
    public void testIsJavaCmd_nullContext() {
        assertEquals("isJavaCmd(null jobContext ...)", false, CustomXmxFlags.isJavaCmd((GpContext)null));
    }

    /**
     * An example java module which already has an -Xmx flag,
     * replace it with the new value.
     */
    @Test
    public void replaceXmx_direct() {
        initContext(javaCmdLine, javaCmdArgs);
        assertArrayEquals(
            // expected
            new String[]{ "java", "-Xmx16g", "-jar", "/mock/libdir/DemoJava.jar"}, 
            // actual
            CustomXmxFlags.addOrReplaceXmxFlag(jobContext, mem, new String[] {"java", "-jar", "/mock/libdir/DemoJava.jar"}));
    }

    /**
     * Don't replace if arg is null
     */
    @Test
    public void replaceXmx_direct_ignoreNull() {
        initContext(javaCmdLine, javaCmdArgs);
        assertArrayEquals(
            // expected
            new String[]{ "java", "-jar", "/mock/libdir/DemoJava.jar"}, 
            // actual
            CustomXmxFlags.addOrReplaceXmxFlag(jobContext, (Memory)null, new String[] {"java", "-jar", "/mock/libdir/DemoJava.jar"}));
    }
    
    
    @Test
    public void testEmptyJavaCmdLine() {
        initContext(javaCmdLine, javaCmdArgs);
        final String[] cmdLineArgs={ };
        final String[] actual=CustomXmxFlags.addOrReplaceXmxFlag(jobContext, mem, cmdLineArgs);
        Assert.assertArrayEquals("empty java cmdLine", cmdLineArgs, actual);
    }
    
    @Test
    public void testNullTaskInfo() {
        final GpContext jobContext=GpContext.getServerContext();
        final String[] cmdLineArgs={"ant", "install-task" };
        final String[] actual=CustomXmxFlags.addOrReplaceXmxFlag(jobContext, mem, cmdLineArgs);
        assertArrayEquals("null taskInfo", cmdLineArgs, actual);
    }

    /** simulate java wrapper_script */
    public void javaWrapperScript_jobMemorySet() {
        initContext(javaCmdLine, javaCmdArgs);
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_MEMORY)).thenReturn(Memory.fromString("512 Mb"));
        final String[] expected={ 
                "/opt/genepattern/resources/wrapper-scripts/run-with-env.sh",
                "-c", 
                "env-custom-broad-centos5.sh", 
                "-u", 
                "Java",
                "java", 
                "-Xmx512m", 
                "-jar", 
                "/mock/libdir/DemoJava.jar" };
        
        assertEquals("default <java> wrapper script with no -Xmx flag", 
                Arrays.asList(expected), 
                Arrays.asList( CustomXmxFlags.addOrReplaceXmxFlag(gpConfig, jobContext, javaWrapperCmdArgs)) );
    }

    @Test
    public void javaWrapperScript_jobMemoryNotSet() {
        initContext(javaCmdLine, javaCmdArgs);
        //when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_MEMORY)).thenReturn(Memory.fromString("512 Mb"));
        final String[] expected={ 
                "/opt/genepattern/resources/wrapper-scripts/run-with-env.sh",
                "-c", 
                "env-custom-broad-centos5.sh", 
                "-u", 
                "Java",
                "java", 
                "-jar", 
                "/mock/libdir/DemoJava.jar" };
        
        assertEquals("default <java> wrapper script, no job.memory set", 
                Arrays.asList(expected), 
                Arrays.asList( CustomXmxFlags.addOrReplaceXmxFlag(gpConfig, jobContext, javaWrapperCmdArgs)) );
    }

    @Test
    public void javaWrapperScript_replaceXmx() {
        initContext(javaCmdLine, javaCmdArgs);
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_MEMORY)).thenReturn(Memory.fromString("4 Gb"));
        final String[] input={ 
                "/opt/run-java.sh", "-u", "Java", "java", 
                "-Xmx512m",
                "-jar", "/mock/libdir/DemoJava.jar" };
        final String[] expected={ 
                "/opt/run-java.sh", "-u", "Java", "java", 
                "-Xmx4g",
                "-jar", "/mock/libdir/DemoJava.jar" };
        
        assertEquals("replace -Xmx with job.memory", 
                Arrays.asList(expected), 
                Arrays.asList( CustomXmxFlags.addOrReplaceXmxFlag(gpConfig, jobContext, input)) );
    }
   
    
    /** simulate duplicate '<java_flags>' on command line */
    @Test
    @Ignore
    public void javaWrapperScript_duplicateXmx() {
        initContext(javaCmdLine, javaCmdArgs);
        when(gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_MEMORY)).thenReturn(Memory.fromString("2 Gb"));
        final String[] input={ 
                "/opt/run-java.sh", "-u", "Java", "java", 
                "-Xmx512m", "-Xmx2g",
                "-jar", "/mock/libdir/DemoJava.jar" };
        final String[] expected={ 
                "/opt/run-java.sh", "-u", "Java", "java", 
                "-Xmx2g",
                "-jar", "/mock/libdir/DemoJava.jar" };
        Assert.assertEquals("duplicate -Xmx flags", 
                Arrays.asList(expected), 
                Arrays.asList( CustomXmxFlags.addOrReplaceXmxFlag(gpConfig, jobContext, input)) );
    }

    @Test
    public void replaceXmx_null() {
        Assert.assertEquals("null string", null, CustomXmxFlags.replaceXmx(mem, null));
    }

    @Test
    public void replaceXmx_emptyString() {
        Assert.assertEquals("empty string", "", CustomXmxFlags.replaceXmx(mem, ""));
    }

    @Test
    public void replaceXmx_completeString() {
        Assert.assertEquals("-Xmx16g", CustomXmxFlags.replaceXmx(mem, "-Xmx1024m"));
    }
    
    @Test
    public void replaceXmx_at_beginning() {
        Assert.assertEquals("-Xmx16g -Dhttp.proxyHost=webcache.example.com -Dhttp.proxyPort=5555", 
                CustomXmxFlags.replaceXmx(mem, "-Xmx512m -Dhttp.proxyHost=webcache.example.com -Dhttp.proxyPort=5555"));
    }

    @Test
    public void replaceXmx_in_middle() {
        Assert.assertEquals("-Dhttp.proxyHost=webcache.example.com -Xmx16g -Dhttp.proxyPort=5555", 
                CustomXmxFlags.replaceXmx(mem, "-Dhttp.proxyHost=webcache.example.com -Xmx512m -Dhttp.proxyPort=5555"));
    }

    @Test
    public void replaceXmx_at_end() {
        Assert.assertEquals("-Dhttp.proxyHost=webcache.example.com -Dhttp.proxyPort=5555 -Xmx16g", 
                CustomXmxFlags.replaceXmx(mem, "-Dhttp.proxyHost=webcache.example.com -Dhttp.proxyPort=5555 -Xmx512m"));
    }

    @Test
    public void replaceXmx_skipInvalidSpec_empty() {
        final String arg="Don't replace -Xmx in a string";
        Assert.assertEquals(arg, 
                CustomXmxFlags.replaceXmx(mem, arg));
    }

    @Test
    public void replaceXmx_skipInvalidSpec_notANumber() {
        final String arg="This is an example of -Xmx=2345 happening to be in a string";
        Assert.assertEquals(arg, 
                CustomXmxFlags.replaceXmx(mem, arg));
    }
    
}
