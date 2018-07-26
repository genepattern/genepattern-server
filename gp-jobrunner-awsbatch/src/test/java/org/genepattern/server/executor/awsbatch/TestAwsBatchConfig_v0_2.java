package org.genepattern.server.executor.awsbatch;

import static org.genepattern.drm.JobRunner.PROP_DOCKER_IMAGE;
import static org.genepattern.drm.JobRunner.PROP_MEMORY;
import static org.genepattern.drm.JobRunner.PROP_QUEUE;
import static org.genepattern.drm.JobRunner.PROP_WALLTIME;
import static org.genepattern.server.executor.awsbatch.AWSBatchJobRunner.PROP_JOB_AWSBATCH_JOB_DEF;

import static org.junit.Assert.*;

import java.io.File;

import org.genepattern.drm.Memory;
import org.genepattern.drm.Walltime;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.Value;
import org.genepattern.server.executor.awsbatch.testutil.Util;
import org.genepattern.webservice.TaskInfo;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test 'config_example_awsbatch-v0.2.yaml' file
 */
public class TestAwsBatchConfig_v0_2 {
    // GSEA base lsid
    public static final String GSEA_LSID="urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00072";

    public static GpContext initJobContext(final String taskName, final String taskLsid) {
        final TaskInfo taskInfo=new TaskInfo();
        taskInfo.setName(taskName);
        taskInfo.giveTaskInfoAttributes().put("LSID", taskLsid);
        final GpContext jobContext=new GpContext.Builder()
            .taskInfo(taskInfo)
        .build();
        return jobContext;
    }

    private static GpContext serverContext;
    private static File configFile;
    private static GpConfig gpConfig;

    @BeforeClass
    public static void beforeClass() throws Throwable {
        serverContext=GpContext.getServerContext();
        configFile=new File(Util.getAwsbatchConfDir(),"config_example_awsbatch-v0.2.yaml");
        gpConfig=Util.initGpConfig(configFile);
        assertNotNull("sanity check after initializing gpConfig from file", gpConfig);
    }

    @Test
    public void executorId_default() {
        assertEquals("default executorId", 
            "AWSBatch",
            gpConfig.getExecutorId(serverContext));
    }
    
    @Test
    public void jobMemory() {
        assertEquals("default job.memory",
            Memory.fromString("2 Gb"),
            gpConfig.getGPMemoryProperty(serverContext, PROP_MEMORY)
        );
    }
    
    @Test
    public void jobWalltime() throws Exception {
        assertEquals("default job.walltime",
            Walltime.fromString("02:00:00"),
            Walltime.fromString(
                gpConfig.getGPProperty(serverContext, PROP_WALLTIME))
        );
    }
    
    @Test
    public void jobQueue_default() {
        assertEquals("default job.queue", 
            "job-queue-default", 
            gpConfig.getGPProperty(serverContext, PROP_QUEUE));
    }

    // sanity check, make sure there is no 'job.docker.image' declared in the config file
    @Test
    public void check_getValue_dockerImage() {
        assertEquals("gpConfig.getValue(serverContext, 'job.docker.image')",
            null,
            gpConfig.getValue(serverContext, PROP_DOCKER_IMAGE)
        );
    }

    // sanity check, make sure that 'job.docker.image.default' is declared in the config file
    @Test
    public void check_getValue_dockerImageDefault() {
        assertEquals("gpConfig.getValue(serverContext, 'job.docker.image.default')",
            new Value("genepattern/docker-java17:0.12"),
            gpConfig.getValue(serverContext, "job.docker.image.default")
        );
    }

    @Test
    public void hierarchicalClustering_v7_17_from_lookup() {
        // urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00009:7.17
        assertEquals("getDockerImage, HCL_v7.17",
            "genepattern/docker-python36:0.5",
            AWSBatchJobRunner.getJobDockerImage(gpConfig, initJobContext("HierarchicalClustering", "urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00009:7.17"))
        );
    }

    @Test
    public void hierarchicalClustering_v6_from_lookup() {
        final String lsid="urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00009:6";
        assertEquals("custom HierarchicalClustering",
            "genepattern/docker-java17:0.12",
            AWSBatchJobRunner.getJobDockerImage(gpConfig, initJobContext("HierarchicalClustering", lsid))
        );
    }

    @Test
    public void hierarchicalClustering_v5_4_from_lookup() {
        // urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00009:7.17
        assertEquals("getDockerImage, HCL_v5.4",
            "genepattern/docker-java17:0.12",
            AWSBatchJobRunner.getJobDockerImage(gpConfig, initJobContext("HierarchicalClustering", "urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00009:5.4"))
        );
    }

    @Test
    public void envCustom_workaround() {
        assertEquals(
            "",
            gpConfig.getGPProperty(serverContext, "env-custom"));
    }

    @Test
    public void runWithEnv_workaround() {
        assertEquals(
            "bash <wrapper-scripts>/run-with-env.sh",
            gpConfig.getGPProperty(serverContext, "run-with-env"));
    }

    @Test
    public void jobQueue_custom() {
        final GpContext jobContext=initJobContext("MockBigMemoryModule", Util.MOCK_LSID_PREFIX+"00001:1");
        assertEquals(
            "job-queue-big-memory",
            gpConfig.getValue(jobContext, PROP_QUEUE).getValue());
    }

    @Test
    public void jobQueue_custom_gsea_v19() {
        final GpContext jobContext=initJobContext("GSEA", GSEA_LSID+":19.0.24");
        assertEquals(
            "job-queue-30gb-disk",
            gpConfig.getValue(jobContext, PROP_QUEUE).getValue());
    }

    @Test
    public void jobQueue_custom_gsea_v18() {
        final GpContext jobContext=initJobContext("GSEA", GSEA_LSID+":18");
        // inherited from 'GSEA' config
        assertEquals(
            "job-queue-30gb-disk",
            gpConfig.getValue(jobContext, PROP_QUEUE).getValue());
    }

    @Test
    public void jobDefn_default() {
        assertEquals(
            "Java17_Oracle_Generic:8",
            gpConfig.getValue(serverContext, PROP_JOB_AWSBATCH_JOB_DEF).getValue());
    }

    @Test
    public void jobDefn_custom_gsea_v19() {
        final GpContext jobContext=initJobContext("GSEA", GSEA_LSID+":19.0.24");
        assertEquals(
            "Java18_Oracle_Generic:8",
            gpConfig.getValue(jobContext, PROP_JOB_AWSBATCH_JOB_DEF).getValue());
    }

    @Test
    public void jobDefn_custom_gsea_v18() {
        final GpContext jobContext=initJobContext("GSEA", GSEA_LSID+":18");
        assertEquals(
            "Java17_Oracle_Generic:8",
            gpConfig.getValue(jobContext, PROP_JOB_AWSBATCH_JOB_DEF).getValue());
    }
    
}
