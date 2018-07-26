package org.genepattern.server.executor.awsbatch;

import static org.genepattern.drm.JobRunner.PROP_DOCKER_IMAGE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.Value;
import org.genepattern.server.executor.awsbatch.testutil.Util;
import org.genepattern.webservice.TaskInfo;

import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.base.Strings;

/**
 * Test 'job.docker.image' customization.
 * 
 * Rules:
 *   when 'job.docker.image' is set in the manifest ...
 *     
 *     # job.docker.image set in the manifest file
 *     Example_v3, manifest:job.docker.image=genepattern/docker-example:3
 *     # custom 'job.docker.image' in the config file
 *     Example_v3.1, manifest:job.docker.image=genepattern/docker-example:3-custom
 *   
 *   for legacy modules which do not set 'job.docker.image' in the manifest ...
 *     # use built-in as a fallback
 *     ExampleDefault_v1, manifest (not set), config:job.docker.image (not set), config:job.docker.image.default (not set)
 *       genepattern/docker-default:1
 *     # use 'job.docker.image.default'
 *     ExampleDefault_v1, manifest (not set), config:job.docker.image (not set)
 *       config:job.docker.image.default=genepattern/docker-default:1-custom
 *     # use 'job.docker.image.lookup' by name:version
 *     ExampleLookup
 *     # use 'job.docker.image.lookup' by lsid:version
 *     ExampleLookup
 *     # use 'job.docker.image.lookup' by lsid_no_version, takes precedence over name_no_version
 *     ExampleLookup
 *     
 *     # job.docker.image.lookup by name_no_version
 *     ExampleLookupByName
 *     
 *       
 *     
 *     Example_v1, manifest (not set)
 *     Example_v2, manifest (not set)
 *     
 *     ExampleLookup, manifest (not set)
 *       v1,   genepattern/docker-example-lookup:1 (lsid:version)
 *       v2,   genepattern/docker-example-lookup:2 (name:version)
 *       v3,   genepattern/docker-example-lookup:3 (lsid_no_version)
 *       v4,   genepattern/docker-example-lookup:3 (lsid_no_version)
 *       v4.1, genepattern/docker-example-lookup:3 (lsid_no_version)
 *     ExampleLookupByName, manifest (not set)
 *       v3,   genepattern/docker-example-by-name:3 (name:version)
 *       v4,   genepattern/docker-example-by-name:4 (name_no_version)
 *       v4.1, genepattern/docker-example-lookup:4  (name_no_version)
 */
public class TestJobDockerImage {
    
    // test for ...
    //   legacy module which uses default image
    //   legacy module which uses custom image
    //   legacy module which uses default image, server 
    
    // Example module with these versions ...
    //    0.1  --> docker-example:0.1
    //    1.0  --> docker-example:1
    //    1.1  --> docker-example:1
    //    1.2  --> docker-example:1
    //    2    --> docker-example:2 (in manifest)
    //    3    --> docker-example:3 (in manifest)
    //    3.1  --> docker-example:latest (in manifest)
    
    public static final String TEST_USER="test_user";
    public static final String EXAMPLE_DEFAULT="urn:lsid:example.com:example.module.analysis:00001";
    public static final String EXAMPLE="urn:lsid:example.com:example.module.analysis:00002";
    public static final String EXAMPLE_LOOKUP="urn:lsid:example.com:example.module.analysis:00003";

    public static GpContext initJobContext(final String taskName, final String taskLsid) {
        return initJobContext(TEST_USER, taskName, taskLsid, null);
    }
    //public static GpContext initJobContext(final String taskName, final String taskLsid, final String dockerImage) {
    //    return initJobContext("test_user", taskName, taskLsid, null);
    //}
    public static GpContext initJobContext(final String userId, final String taskName, final String taskLsid, final String dockerImage) {
        final TaskInfo taskInfo=new TaskInfo();
        taskInfo.setName(taskName);
        taskInfo.giveTaskInfoAttributes().put("LSID", taskLsid);
        if (!Strings.isNullOrEmpty(dockerImage)) {
            taskInfo.giveTaskInfoAttributes().put("job.docker.image", dockerImage);
        }
        final GpContext jobContext=new GpContext.Builder()
            .userId(userId)
            .taskInfo(taskInfo)
        .build();
        return jobContext;
    }

    private static GpContext serverContext;
    // the example configuration
    private static GpConfig gpConfig;
    
    @BeforeClass
    public static void beforeClass() throws Throwable {
        serverContext=GpContext.getServerContext();
        // loaded from ./gp-jobrunner-awsbatch/src/test/resources/
        final File configFile=Util.getTestResource(TestJobDockerImage.class, "/config_test_job_docker_image.yaml");
        gpConfig=Util.initGpConfig(configFile);
        assertNotNull("sanity check after initializing gpConfig from file", gpConfig);
    }

    /** sanity check, make sure there is no 'job.docker.image' declared in the config file */
    @Test
    public void getValue_jobDockerImage() {
        assertEquals("gpConfig.getValue(serverContext, 'job.docker.image')",
            null,
            gpConfig.getValue(serverContext, PROP_DOCKER_IMAGE)
        );
    }

    /** sanity check, make sure that 'job.docker.image.default' is declared in the config file */
    @Test
    public void getValue_jobDockerImageDefault() {
        assertEquals("gpConfig.getValue(serverContext, 'job.docker.image.default')",
            new Value("genepattern/docker-default:1-custom"),
            gpConfig.getValue(serverContext, "job.docker.image.default")
        );
    }

    /**
     * Use 'job.docker.image' from the manifest file
     *   Example_v3, manifest
     * <pre>
         job.docker.image=genepattern/docker-example:3
     * </pre>
     */  
    @Test public void from_manifest() {
        final String dockerImage="genepattern/docker-example:3";
       assertEquals("getJobDockerImage", 
            dockerImage,
            AWSBatchJobRunner.getJobDockerImage(gpConfig, initJobContext(TEST_USER, "Example", EXAMPLE+":3", dockerImage)));
    }

    /**
     * custom 'job.docker.image' in the config file
     *   Example_v3.1, manifest:job.docker.image=genepattern/docker-example:3-custom
     */
    @Test public void from_manifest_custom() {
       assertEquals("getJobDockerImage", 
            // custom setting
            "genepattern/docker-example:3-custom",
            AWSBatchJobRunner.getJobDockerImage(gpConfig, initJobContext(TEST_USER, "Example", EXAMPLE+":3.1", "genepattern/docker-example:3")));
    }

    /**
     * use built-in as a fallback, when not set in manifest and not set in the config file.
     *   ExampleDefault_v1
     */
    @Test public void use_builtin_default() {
        // use the built-in default value when 'job.docker.image' and 'job.docker.image.default' are not set 
        //   ExampleNoConfig_v1 does not declare a job.docker.image
        final GpConfig gpConfig=new GpConfig.Builder().build();
       assertEquals("getJobDockerImage", 
            "genepattern/docker-java17:0.12",
            AWSBatchJobRunner.getJobDockerImage(gpConfig, initJobContext("ExampleDefault", EXAMPLE_DEFAULT+":1")));
    }
    
    /**
     * use 'job.docker.image.default', when not set in manifest and not otherwise
     * customized in the config file.
     *   ExampleDefault_v1
     */
    @Test public void use_jobDockerImageDefault() {
       assertEquals("getJobDockerImage", 
            "genepattern/docker-default:1-custom",
            AWSBatchJobRunner.getJobDockerImage(gpConfig, initJobContext("ExampleDefault", EXAMPLE_DEFAULT+":1")));
    }

    /**
     * use 'job.docker.image.lookup' by name:version
     */
    @Test public void lookup_by_name_version() {
       assertEquals("getJobDockerImage", 
            "genepattern/docker-example:1",
            AWSBatchJobRunner.getJobDockerImage(gpConfig, initJobContext("ExampleLookup", EXAMPLE_LOOKUP+":1")));
    }

    /**
     * use 'job.docker.image.lookup' by lsid:version
     */
    @Test public void lookup_by_lsid_version() {
       assertEquals("getJobDockerImage", 
            "genepattern/docker-example:2",
            AWSBatchJobRunner.getJobDockerImage(gpConfig, initJobContext("ExampleLookup", EXAMPLE_LOOKUP+":2")));
    }

    /**
     *  use 'job.docker.image.lookup' by lsid_no_version, takes precedence over name_no_version
     */
    @Test public void lookup_by_lsid_no_version() {
       assertEquals("getJobDockerImage", 
            "genepattern/docker-example:3",
            AWSBatchJobRunner.getJobDockerImage(gpConfig, initJobContext("ExampleLookup", EXAMPLE_LOOKUP+":3")));
    }

    /**
     * use 'job.docker.image.lookup' by name_no_version
     */
    @Test public void lookup_by_name_no_version() {
       assertEquals("getJobDockerImage", 
            "genepattern/docker-example:3",
            AWSBatchJobRunner.getJobDockerImage(gpConfig, initJobContext("ExampleLookupByName", EXAMPLE_LOOKUP)));
    }

}
