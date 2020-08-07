package org.genepattern.server.config;

import static org.genepattern.drm.JobRunner.PROP_DOCKER_IMAGE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.genepattern.junitutil.ConfigUtil;
import org.genepattern.junitutil.Demo;
import org.genepattern.junitutil.FileUtil;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.Value;
import org.genepattern.webservice.TaskInfo;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.base.Strings;

/**
 * Test 'job.docker.image' configuration and customization options.
 * 
 * These tests use a GpConfig from a custom config file,
 *    ./test/src/org/genepattern/server/config/config_default.yaml 
 */
public class TestDockerImage {
    
    public static final String TEST_USER="test_user";
    public static final String TEST_USER_CUSTOM_EXEC="test_user_custom_exec";

    public static final String LSID_PREFIX="urn:lsid:example.com:example.module.analysis:";
    public static final String EXAMPLE_DEFAULT="urn:lsid:example.com:example.module.analysis:00001";
    public static final String EXAMPLE="urn:lsid:example.com:example.module.analysis:00002";
    public static final String EXAMPLE_LOOKUP="urn:lsid:example.com:example.module.analysis:00003";
    public static final String EXAMPLE_LOOKUP_BY_NAME="urn:lsid:example.com:example.module.analysis:00004";

    // the example configuration
    private static GpConfig gpConfig;

    @BeforeClass
    public static void beforeClass() throws Throwable {
        final File configFile=FileUtil.getSourceFile(TestDockerImage.class, "config_test_job_docker_image.yaml");
        gpConfig=ConfigUtil.initGpConfig(configFile);
        assertNotNull("sanity check after initializing gpConfig from file", gpConfig);
    }

    protected static GpContext initJobContext(final String userId, final TaskInfo taskInfo) {
        final GpContext jobContext=new GpContext.Builder()
            .userId(userId)
            .taskInfo(taskInfo)
        .build();
        return jobContext;
    }

    /**
     * for testing, create a TaskInfo which does not declare a 'job.docker.image' in the manifest file
     * @param taskName
     * @param taskLsid
     */
    protected static TaskInfo initTaskInfoNoManifest(final String taskName, final String taskLsid) {
        final TaskInfo taskInfo=new TaskInfo();
        taskInfo.setName(taskName);
        taskInfo.giveTaskInfoAttributes().put("LSID", taskLsid);
        return taskInfo;
    }

    /**
     * for testing, create a TaskInfo which declares a 'job.docker.image' in the manifest file
     * @param taskName
     * @param taskLsid
     * @param dockerImage
     */
    protected static TaskInfo initTaskInfo(final String taskName, final String taskLsid, final String dockerImage) {
        final TaskInfo taskInfo=new TaskInfo();
        taskInfo.setName(taskName);
        taskInfo.giveTaskInfoAttributes().put("LSID", taskLsid);
        if (!Strings.isNullOrEmpty(dockerImage)) {
            taskInfo.giveTaskInfoAttributes().put("job.docker.image", dockerImage);
        }
        return taskInfo;
    }

    /**
     * create a jobContext for a module which does not set the 'job.docker.image' in the manifest file
     * @param taskName
     * @param taskLsid
     */
    protected static GpContext initJobContextNoManifest(final String taskName, final String taskLsid) {
        final TaskInfo taskInfo=initTaskInfoNoManifest(taskName, taskLsid);
        return initJobContext(TEST_USER, taskInfo);
    }
    
    protected static GpContext initJobContext(final String taskName, final String taskLsid, final String dockerImage) {
        final TaskInfo taskInfo=initTaskInfo(taskName, taskLsid, dockerImage);
        return initJobContext(TEST_USER, taskInfo);
    }

    /**
     * Create a jobContext for the junit test.
     * When 'dockerImage' is not null, this is for a module which has a 'job.docker.image' set in the manifest file.
     * When null, it is for a module which does not declare 'job.docker.image' in the manifest.
     * 
     * @param userId
     * @param taskName
     * @param taskLsid
     * @param dockerImage
     */
    protected static GpContext initJobContext(final String userId, final String taskName, final String taskLsid, final String dockerImage) {
        final TaskInfo taskInfo=initTaskInfo(taskName, taskLsid, dockerImage);
        final GpContext jobContext=new GpContext.Builder()
            .userId(userId)
            .taskInfo(taskInfo)
        .build();
        return jobContext;
    }

    /**
     * assertDockerImage AND assertDockerImage_deprecated as one assertion
     * Just a sanity check that the GpConfig.getDockerImage gives the same value as DockerImage.getDockerImage
     */
    protected static void assertDockerImageAll(final String expectedDockerImage, final GpConfig gpConfig, final GpContext jobContext) {
        assertDockerImage(expectedDockerImage, gpConfig, jobContext);
        assertDockerImage_deprecated(expectedDockerImage, gpConfig, jobContext);
    }

    public static void assertDockerImage(final String expectedDockerImage, final GpConfig gpConfig, final GpContext jobContext) {
        final String lsid=jobContext.getLsid();
        final String version=lsid.substring(lsid.lastIndexOf(":")+1);
        assertEquals(
            "job.docker.image for '"+jobContext.getTaskName()+":"+version+"'",
            expectedDockerImage,
            gpConfig.getJobDockerImage(jobContext));
    }

    @SuppressWarnings("deprecation")
    protected static void assertDockerImage_deprecated(final String expectedDockerImage, final GpConfig gpConfig, final GpContext jobContext) {
        final String lsid=jobContext.getLsid();
        final String version=lsid.substring(lsid.lastIndexOf(":")+1);
        // validate the deprecated call
        assertEquals(
                "job.docker.image for '"+jobContext.getTaskName()+":"+version+"' (deprecated)",
                expectedDockerImage,
                org.genepattern.drm.DockerImage.getJobDockerImage(gpConfig, jobContext));
    }

    /**
     * test a module which sets 'job.docker.image' in the manifest file.
     * @param gpConfig
     * @param name
     * @param lsid
     * @param dockerImage the docker image that is declared in the manifest file
     */
    public static void assertDockerImageFromManifest(final String dockerImage, final GpConfig gpConfig, final String name, final String lsid) {
        final GpContext jobContext=initJobContext(name, lsid, dockerImage);
        assertDockerImage(dockerImage, gpConfig, jobContext);
    }

    /**
     * test a legacy module which does not set 'job.docker.image' in the manifest file.
     * assert that the gpConfig has the expected dockerImage.
     * 
     * @param expectedDockerImage
     * @param gpConfig the server configuration instance
     * @param name the module name
     * @param lsid the full module lsid
     */
    protected static void assertDockerImageNoManifest(final String expectedDockerImage, final GpConfig gpConfig, final String name, final String lsid) {
        final GpContext jobContext=TestDockerImage.initJobContextNoManifest(name, lsid);
        assertDockerImage(expectedDockerImage, gpConfig, jobContext);
    }

    /** sanity check, make sure there is no 'job.docker.image' declared in the config file */
    @Test
    public void getValue_jobDockerImage() {
        assertEquals("gpConfig.getValue(serverContext, 'job.docker.image')",
            null,
            gpConfig.getValueFromConfig(Demo.serverContext, PROP_DOCKER_IMAGE)
        );
    }

    /** sanity check, make sure that 'job.docker.image.default' is declared in the config file
     *  for the default executor, e.g.
     *  <pre>
        default.properties:
            executor: "AWSBatch"
            ...
            executors:
                AWSBatch:
                    ...
                    default.properties:
                        job.docker.image.default: {}
     *  </pre>
     */
    @Test
    public void getValue_jobDockerImageDefault_awsbatch() {
        assertEquals("gpConfig.getValue(serverContext, 'job.docker.image.default')",
            new Value("genepattern/docker-default:1-awsbatch-default"),
            gpConfig.getValue(Demo.serverContext, "job.docker.image.default")
        );
    }

    /** sanity check, make sure that 'job.docker.image.default' is declared in the config file for
     * the non-default executor 
     */
    @Test
    public void getValue_jobDockerImageDefault_customExecutor() {
        final GpContext jobContext=new GpContext.Builder()
            .userId(TEST_USER_CUSTOM_EXEC)
        .build();
        assertEquals("gpConfig.getValue(serverContext, 'job.docker.image.default')",
            new Value("genepattern/docker-default:1-custom"),
            gpConfig.getValue(jobContext, "job.docker.image.default")
        );
    }

    /**
     * use built-in as a fallback, when not set in manifest and not set in the config file.
     *   ExampleDefault_v1
     */
    @Test public void use_builtin_default() {
        assertDockerImageAll(
            "genepattern/docker-java17:0.12", 
            new GpConfig.Builder().build(), 
            initJobContextNoManifest("ExampleDefault", EXAMPLE_DEFAULT+":1")
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
        final String dockerImageFromManifest="genepattern/docker-example:3";
        final String expected=dockerImageFromManifest;
        assertDockerImage( 
            expected,
            gpConfig, 
            initJobContext(TEST_USER, "Example", EXAMPLE+":3", dockerImageFromManifest)
        );
    }

    /**
     * Override the 'job.docker.image' from the manifest file by setting it 
     * directly in the config file.
     *   Example_v3.1, manifest:job.docker.image=genepattern/docker-example:3-custom
     */
    @Test public void override_manifest_file() {
        final String dockerImageFromManifest="genepattern/docker-example:3";
        final String expected="genepattern/docker-example:3.1-custom";
        assertDockerImage(
            expected,
            gpConfig, 
            initJobContext(TEST_USER, "Example", EXAMPLE+":3.1", dockerImageFromManifest)
        );
    }

    /**
     * use 'job.docker.image.default', when not set in manifest.
     *   ExampleDefault_v1
     */
    @Test public void from_job_docker_image_default() {
        assertDockerImage(
            // expected docker image
            "genepattern/docker-default:1-awsbatch-default", 
            gpConfig,
            //   {taskName}, {baseLsid:version}
            initJobContextNoManifest("ExampleDefault", EXAMPLE_DEFAULT+":1")
        );
    }

    /**
     * 'job.docker.image.lookup' by {taskName:version}
     */
    @Test public void lookup_by_name_version() {
        assertDockerImage(
            "genepattern/docker-example:1",
            gpConfig,
            initJobContextNoManifest("ExampleLookup", EXAMPLE_LOOKUP+":1")
        );
    }

    /**
     * 'job.docker.image.lookup' by {lsid}
     */
    @Test public void lookup_by_lsid_version() {
       assertDockerImage(
           "genepattern/docker-example:2",
           gpConfig,
           initJobContextNoManifest("ExampleLookup", EXAMPLE_LOOKUP+":2")
       );
    }

    /**
     *  use 'job.docker.image.lookup' by lsid_no_version, takes precedence over name_no_version
     */
    @Test public void lookup_by_lsid_no_version() {
        assertDockerImage(
            "genepattern/docker-example:latest",
            gpConfig,
            initJobContextNoManifest("ExampleLookup", EXAMPLE_LOOKUP+":3")
        );
    }

    /**
     * use 'job.docker.image.lookup' by name_no_version
     */
    @Test public void lookup_by_name_no_version() {
        assertDockerImage(
            "genepattern/docker-example-by-name:latest",
            gpConfig,
            initJobContextNoManifest("ExampleLookupByName", EXAMPLE_LOOKUP_BY_NAME+":4")
        );
    }

    /** custom job.docker.image by {lsid:version} */
    @Test public void custom_per_module_by_lsid() {
        assertDockerImage(
            "genepattern/docker-example:2.1-custom",
            gpConfig, 
            initJobContextNoManifest("Example", EXAMPLE+":2.1")
        );
    }

    /** custom job.docker.image by {taskName} */
    @Test public void custom_dockerImageDefault_per_module_1() {
        assertDockerImage(
            "genepattern/docker-example:latest-custom",
            gpConfig,
            initJobContextNoManifest("Example", EXAMPLE+":4")
        );
    }

}
