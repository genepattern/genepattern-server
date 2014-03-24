package org.genepattern.server.genepattern;

import java.io.File;

import org.genepattern.drm.Memory;
import org.genepattern.junitutil.FileUtil;
import org.genepattern.junitutil.TaskUtil;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.GpContextFactory;
import org.genepattern.server.genepattern.CustomXmxFlags;
import org.genepattern.webservice.TaskInfo;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * junit test cases for automatically setting java Xmx flag based on 'job.memory' property.
 * @author pcarr
 *
 */
public class TestCustomXmxFlags {
    private GpContext jobContext;

    private GpContext initJobContext() {
        final File zipfile=FileUtil.getDataFile("modules/ARACNE_v2.zip");
        final TaskInfo taskInfo=TaskUtil.getTaskInfoFromZip(zipfile);
        
        return new GpContextFactory.Builder()
            .taskInfo(taskInfo)
            .build();
    }
    
    @Before
    public void beforeTest() {
        this.jobContext=initJobContext();
    }
    
    /**
     * Test case for Aracne as configured on gpprod,
     * <pre>
module.properties:
    ARACNE:
        executor: LSF
        lsf.max.memory: 8
        java_flags: -Xmx8g
     * </pre>
     * 
     * <pre>
           <java> <java_flags> -cp <libdir>aracne-java.jar<path.separator><libdir>aracne-main.jar<path.separator><libdir>commons-cli.jar<path.separator><libdir>commons-logging-1.0.3.jar<path.separator><libdir>workbook-0.9.jar<path.separator><libdir>log4j-1.2.8.jar<path.separator><libdir>collections-generic-4.0.jar<path.separator><libdir>commons-math-1.0.jar<path.separator><libdir>gp-modules.jar 
               org.genepattern.modules.aracne.ARACNE 
               -i <dataset.file> 
               -o <output.file> 
               -h <hub.gene> 
               -s <hub.genes.file> 
               -l <transcription.factor.file> 
               -k <kernel.width> 
               -t <mi.threshold> 
               -p <p.value> 
               -e <dpi.tolerance> 
               -f <mean.filter> 
               <cv.filter> 
     * </pre>
     */
    @Test
    public void testAracneLegacy() {
        final Memory mem=Memory.fromString("16 Gb");
        final String[] cmdLineArgs= {
                "/broad/software/free/Linux/redhat_5_x86_64/pkgs/sun-java-jdk_1.6.0-21_x86_64/bin/java",
                "-Xmx1024m", 
                "-cp",
                "/xchip/gpprod/servers/genepattern/taskLib/ARACNE.2.854/aracne-java.jar:/xchip/gpprod/servers/genepattern/taskLib/ARACNE.2.854/aracne-main.jar:/xchip/gpprod/servers/genepattern/taskLib/ARACNE.2.854/commons-cli.jar:/xchip/gpprod/servers/genepattern/taskLib/ARACNE.2.854/commons-logging-1.0.3.jar:/xchip/gpprod/servers/genepattern/taskLib/ARACNE.2.854/workbook-0.9.jar:/xchip/gpprod/servers/genepattern/taskLib/ARACNE.2.854/log4j-1.2.8.jar:/xchip/gpprod/servers/genepattern/taskLib/ARACNE.2.854/collections-generic-4.0.jar:/xchip/gpprod/servers/genepattern/taskLib/ARACNE.2.854/commons-math-1.0.jar:/xchip/gpprod/servers/genepattern/taskLib/ARACNE.2.854/gp-modules.jar",
                "org.genepattern.modules.aracne.ARACNE",
                "-i", 
                "/xchip/gpprod-upload/servers/genepattern/users/KarolBaca/uploads/tmp/run381623986050853385.tmp/dataset.file/1/GSEAinput.gct",
                "-o",
                "p53_pval_m60_2.adj",
                "-h",
                "-s",
                "/xchip/gpprod-upload/servers/genepattern/users/KarolBaca/uploads/tmp/run5295682235411312210.tmp/hub.genes.file/2/p53_list_alias.txt",
                "-l",
                "-k",
                "-t",
                "0",
                "-p",
                "1e-60",
                "-e",
                "1",
                "-f",
                "0",
                "0"
        };
        final String[] actual=CustomXmxFlags.addOrReplaceXmxFlag(jobContext, mem, cmdLineArgs);
        Assert.assertEquals("", "-Xmx16g", actual[1]);
    }
    
    @Test
    public void testReplaceXmx_null() {
        Memory mem=Memory.fromString("16gb");
        Assert.assertEquals("null string", null, CustomXmxFlags.replaceXmx(mem, null));
    }

    @Test
    public void testReplaceXmx_emptyString() {
        Memory mem=Memory.fromString("16gb");
        Assert.assertEquals("empty string", "", CustomXmxFlags.replaceXmx(mem, ""));
    }

    @Test
    public void testReplaceXmx_completeString() {
        Memory mem=Memory.fromString("16gb");
        Assert.assertEquals("-Xmx16g", CustomXmxFlags.replaceXmx(mem, "-Xmx1024m"));
    }
    
    @Test
    public void testReplaceXmx_at_beginning() {
        Memory mem=Memory.fromString("16gb");
        Assert.assertEquals("-Xmx16g -Dhttp.proxyHost=http.proxyHost=webcache.example.com -Dhttp.proxyPort=5555", 
                CustomXmxFlags.replaceXmx(mem, "-Xmx512m -Dhttp.proxyHost=http.proxyHost=webcache.example.com -Dhttp.proxyPort=5555"));
    }

    @Test
    public void testReplaceXmx_in_middle() {
        Memory mem=Memory.fromString("16gb");
        Assert.assertEquals("-Dhttp.proxyHost=http.proxyHost=webcache.example.com -Xmx16g -Dhttp.proxyPort=5555", 
                CustomXmxFlags.replaceXmx(mem, "-Dhttp.proxyHost=http.proxyHost=webcache.example.com -Xmx512m -Dhttp.proxyPort=5555"));
    }

    @Test
    public void testReplaceXmx_at_end() {
        Memory mem=Memory.fromString("16gb");
        Assert.assertEquals("-Dhttp.proxyHost=http.proxyHost=webcache.example.com -Dhttp.proxyPort=5555 -Xmx16g", 
                CustomXmxFlags.replaceXmx(mem, "-Dhttp.proxyHost=http.proxyHost=webcache.example.com -Dhttp.proxyPort=5555 -Xmx512m"));
    }

}
