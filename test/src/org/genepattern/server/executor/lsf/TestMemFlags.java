package org.genepattern.server.executor.lsf;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.genepattern.drm.JobRunner;
import org.genepattern.junitutil.FileUtil;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.GpContextFactory;
import org.genepattern.server.config.GpServerProperties;
import org.junit.Assert;
import org.junit.Test;
import static org.hamcrest.core.Is.is;

/**
 * junit tests for initializing the Lsf Memory Flags from the GpConfig.
 * @author pcarr
 *
 */
public class TestMemFlags {
    private final GpConfig fromProps(final Properties customProps) {
        final File resourcesDir=FileUtil.getSourceDir(this.getClass());
        final GpServerProperties serverProperties=new GpServerProperties.Builder()
            .resourcesDir(resourcesDir)
            .addCustomProperties(customProps)
            .build();
        final GpConfig gpConfig=new GpConfig.Builder()
            .serverProperties(serverProperties)
            .build();
        return gpConfig;
    }
    
    private GpConfig initDefaultConfig() {
        final File resourcesDir=FileUtil.getSourceDir(this.getClass());
        final GpServerProperties serverProperties=new GpServerProperties.Builder()
            .resourcesDir(resourcesDir)
            .build();
        final GpConfig gpConfig=new GpConfig.Builder()
            .serverProperties(serverProperties)
            .build();
        return gpConfig;
    }

    @Test
    public void testDefaultValues() {
        final GpConfig gpConfig=initDefaultConfig();
        final GpContext gpContext=new GpContextFactory.Builder()
            .build();
       
        final List<String> memFlags=LsfCommand2.getMemFlags(gpConfig, gpContext);
        List<String> expected = Arrays.asList("-R", "rusage[mem=2]", "-M", "2");
        Assert.assertThat("memFlags", memFlags, is(expected));
    }
    
    @Test
    public void testFromJobRunnerFlag() {
        final Properties props=new Properties();
        props.put(JobRunner.PROP_MEMORY, "8gb");
        final GpConfig gpConfig=fromProps(props);
        final GpContext gpContext=GpContext.getServerContext();
        final List<String> memFlags=LsfCommand2.getMemFlags(gpConfig, gpContext);
        List<String> expected = Arrays.asList("-R", "rusage[mem=8]", "-M", "8");
        Assert.assertThat("memFlags", memFlags, is(expected)); 
    }

    /**
     * Must round up to an integer value.
     */
    @Test
    public void testRoundUpToGb() {
        final Properties props=new Properties();
        props.put(JobRunner.PROP_MEMORY, "512mb");
        final GpConfig gpConfig=fromProps(props);
        final GpContext gpContext=GpContext.getServerContext();
        final List<String> memFlags=LsfCommand2.getMemFlags(gpConfig, gpContext);
        List<String> expected = Arrays.asList("-R", "rusage[mem=1]", "-M", "1");
        Assert.assertThat("memFlags", memFlags, is(expected)); 
    }
    
    @Test
    public void testFromLsfMaxMemoryFlag() {
        final Properties props=new Properties();
        props.put(LsfProperties.Key.MAX_MEMORY.getKey(), "16");
        final GpConfig gpConfig=fromProps(props);
        final GpContext gpContext=GpContext.getServerContext();
        final List<String> memFlags=LsfCommand2.getMemFlags(gpConfig, gpContext);
        List<String> expected = Arrays.asList("-R", "rusage[mem=16]", "-M", "16");
        Assert.assertThat("memFlags", memFlags, is(expected)); 
    }
    
    /**
     * When both 'lsf.max.memory' and 'drm.memory' are set, use 'drm.memory'.
     */
    @Test
    public void testBothJobRunnerAndLsf() {
        final Properties props=new Properties();
        props.put(LsfProperties.Key.MAX_MEMORY.getKey(), "16");
        props.put(JobRunner.PROP_MEMORY, "512mb");
        final GpConfig gpConfig=fromProps(props);
        final GpContext gpContext=GpContext.getServerContext();
        final List<String> memFlags=LsfCommand2.getMemFlags(gpConfig, gpContext);
        List<String> expected = Arrays.asList("-R", "rusage[mem=1]", "-M", "1");
        Assert.assertThat("memFlags", memFlags, is(expected)); 
    }

}
