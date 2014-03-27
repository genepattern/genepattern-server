package org.genepattern.server.genepattern;

import java.io.File;

import org.genepattern.drm.Memory;
import org.genepattern.junitutil.FileUtil;
import org.genepattern.junitutil.TaskUtil;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.GpContextFactory;
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
        final GpContext taskContext=new GpContextFactory.Builder()
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
        Assert.assertArrayEquals("No change for pipelines", expected, actual);
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
        final GpContext jobContext=new GpContextFactory.Builder()
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
    
}
