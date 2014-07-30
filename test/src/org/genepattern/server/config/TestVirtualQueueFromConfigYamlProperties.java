package org.genepattern.server.config;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.util.Arrays;

import org.genepattern.drm.JobRunner;
import org.genepattern.junitutil.FileUtil;
import org.genepattern.server.auth.IGroupMembershipPlugin;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;
import org.junit.Before;
import org.junit.Test;

/**
 * junit test cases for setting a virtual queue with the 'executor.properties' idom.
 * See the 'virtual_queue_executor_properties_test.yaml' file for an example configuration.
 * 
 * @author pcarr
 *
 */
public class TestVirtualQueueFromConfigYamlProperties {
    //private File configFile;
    private JobConfigObj jobConfigObj;
    private IGroupMembershipPlugin groupInfo;
    private ConfigYamlProperties configYamlProps;
    private GpContext gpContext;
    private TaskInfo topHatInfo;

    @Before
    public void setUp() throws ConfigurationException {
        // use the example config file from resources directory
        File configFile=new File("resources/config_example_virtual_queue.yaml");
        this.jobConfigObj=ConfigFileParser.parse(configFile);
        this.groupInfo=mock(IGroupMembershipPlugin.class);
        this.configYamlProps=ConfigFileParser.initConfigYamlProperties(jobConfigObj, groupInfo);
        this.gpContext=mock(GpContext.class);
        this.topHatInfo=mock(TaskInfo.class);
        when(topHatInfo.getName()).thenReturn("TopHat");
        when(topHatInfo.giveTaskInfoAttributes()).thenReturn(new TaskInfoAttributes());
    }
    
    @Test
    public void virtualQueue_default() {
        assertEquals("expected 'job.queue'", 
                "genepattern", 
                configYamlProps.getProperty(gpContext, JobRunner.PROP_QUEUE));
        assertEquals("expected default 'job.virtualQueue'", 
                "genepattern_short",
                configYamlProps.getProperty(gpContext, JobRunner.PROP_VIRTUAL_QUEUE));
        assertEquals("expected default 'job.extraArgs'",
                null,
                configYamlProps.getValue(gpContext, "job.extraArgs"));
    }
    
    @Test
    public void virtualQueue_custom() {
        when(gpContext.getTaskInfo()).thenReturn(topHatInfo);
        assertEquals("expected 'executor.props'for TopHat", new Value("genepattern_long"), configYamlProps.getValue(gpContext, "executor.props"));
        assertEquals("when taskName is 'TopHat', expected getProperty('job.queue')",
                "genepattern",
                configYamlProps.getProperty(gpContext, "job.queue"));
        assertEquals("when taskName is 'TopHat', expected getValue('job.queue')",
                new Value("genepattern"),
                configYamlProps.getValue(gpContext, "job.queue"));
        assertEquals("when taskName is 'TopHat', expected getValue('job.extraArgs')",
                new Value(Arrays.asList("-g", "/genepattern/gpprod/long", "-m", "node1448 node1449 node1450 node1451 node1452 node1453 node1454 node1455")),
                configYamlProps.getValue(gpContext, "job.extraArgs"));
        assertEquals("when taskName is 'TopHat', expected getValue('job.virtualQueue')",
                new Value("genepattern_long"),
                configYamlProps.getValue(gpContext, "job.virtualQueue"));
    }
    
}
