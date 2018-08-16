package org.genepattern.drm;

import static org.genepattern.drm.JobRunner.PROP_DOCKER_IMAGE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.genepattern.junitutil.ConfigUtil;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.Value;
import org.genepattern.webservice.TaskInfo;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.base.Strings;

/**
 * Test 'job.docker.image' customization.
 */
public class TestDockerImage {
    
    public static final String TEST_USER="test_user";
    public static final String TEST_USER_CUSTOM_EXEC="test_user_custom_exec";

    public static final String LSID_PREFIX="urn:lsid:example.com:example.module.analysis:";
    public static final String EXAMPLE_DEFAULT="urn:lsid:example.com:example.module.analysis:00001";
    public static final String EXAMPLE="urn:lsid:example.com:example.module.analysis:00002";
    public static final String EXAMPLE_LOOKUP="urn:lsid:example.com:example.module.analysis:00003";
    public static final String EXAMPLE_LOOKUP_BY_NAME="urn:lsid:example.com:example.module.analysis:00004";

    protected static GpContext initJobContext(final TaskInfo taskInfo) {
        return initJobContext(TEST_USER, taskInfo);
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
    private static GpContext initJobContextNoManifest(final String taskName, final String taskLsid) {
        final TaskInfo taskInfo=initTaskInfoNoManifest(taskName, taskLsid);
        return initJobContext(TEST_USER, taskInfo);
    }
    
    public static GpContext initJobContext(final String taskName, final String taskLsid, final String dockerImage) {
        final TaskInfo taskInfo=initTaskInfo(taskName, taskLsid, dockerImage);
        return initJobContext(TEST_USER, taskInfo);
    }

    /**
     * Create a jobContext for the unit test.
     * When 'dockerImage' is not null, this is for a module which has a 'job.docker.image' set in the manifest file.
     * When null, it is for a module which does not declare 'job.docker.image' in the manifest.
     * 
     * @param userId
     * @param taskName
     * @param taskLsid
     * @param dockerImage
     */
    public static GpContext initJobContext(final String userId, final String taskName, final String taskLsid, final String dockerImage) {
        final TaskInfo taskInfo=initTaskInfo(taskName, taskLsid, dockerImage);
        final GpContext jobContext=new GpContext.Builder()
            .userId(userId)
            .taskInfo(taskInfo)
        .build();
        return jobContext;
    }

    private void assertDockerImage(final String expectedDockerImage, final GpContext jobContext) {
        assertDockerImage(expectedDockerImage, gpConfig, jobContext);
    }

    private void assertDockerImage(final String expectedDockerImage, final GpConfig gpConfig, final GpContext jobContext) {
        final String lsid=jobContext.getLsid();
        final String version=lsid.substring(lsid.lastIndexOf(":")+1);
        assertEquals(
            "job.docker.image for '"+jobContext.getTaskName()+":"+version+"'",
            expectedDockerImage,
            DockerImage.getJobDockerImage(gpConfig, jobContext));
    }

    private static GpContext serverContext;
    // the example configuration
    private static GpConfig gpConfig;
    
    @BeforeClass
    public static void beforeClass() throws Throwable {
        serverContext=GpContext.getServerContext();
        final File configFile=new File("resources/config_test_job_docker_image.yaml");
        gpConfig=ConfigUtil.initGpConfig(configFile);
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
            gpConfig.getValue(serverContext, "job.docker.image.default")
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
        assertDockerImage(
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
            //   {taskName}, {baseLsid:version}
            initJobContextNoManifest("ExampleDefault", EXAMPLE_DEFAULT+":1")
        );
    }

    /**
     * use 'job.docker.image.lookup' by name:version
     */
    @Test public void lookup_by_name_version() {
        assertDockerImage(
            "genepattern/docker-example:1-awsbatch-lookup-name-version",
            initJobContextNoManifest("ExampleLookup", EXAMPLE_LOOKUP+":1")
        );
    }

    /**
     * use 'job.docker.image.lookup' by lsid:version
     */
    @Test public void lookup_by_lsid_version() {
       assertDockerImage(
           "genepattern/docker-example:2-awsbatch-lookup-lsid",
           initJobContextNoManifest("ExampleLookup", EXAMPLE_LOOKUP+":2")
       );
    }

    /**
     *  use 'job.docker.image.lookup' by lsid_no_version, takes precedence over name_no_version
     */
    @Test public void lookup_by_lsid_no_version() {
        assertDockerImage(
            "genepattern/docker-example:3-awsbatch-lookup-lsid-no-version",
            initJobContextNoManifest("ExampleLookup", EXAMPLE_LOOKUP+":3")
        );
    }

    /**
     * use 'job.docker.image.lookup' by name_no_version
     */
    @Test public void lookup_by_name_no_version() {
        assertDockerImage(
            "genepattern/docker-example:awsbatch-lookup-name-no-version",
            initJobContextNoManifest("ExampleLookupByName", EXAMPLE_LOOKUP_BY_NAME+":4")
        );
    }

    // set job.docker.image.default in module.properties -> {lsid}
    // set job.docker.image.default in module.properties -> {taskName}
    @Test public void custom_dockerImageDefault_per_module() {
        assertDockerImage(
            "genepattern/docker-example:2.1-custom",
            initJobContextNoManifest("Example", EXAMPLE+":2.1")
        );
    }

}
