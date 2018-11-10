package org.genepattern.server.config;

import static org.genepattern.drm.JobRunner.PROP_DOCKER_IMAGE;
import static org.genepattern.drm.JobRunner.PROP_DOCKER_IMAGE_DEFAULT;
import static org.genepattern.server.config.TestDockerImage.assertDockerImageFromManifest;
import static org.genepattern.server.config.TestDockerImage.assertDockerImageNoManifest;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.genepattern.junitutil.ConfigUtil;
import org.genepattern.junitutil.Demo;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test 'job.docker.image.lookup' for legacy modules on a server with the default config file
 *     ./resources/config_default.yaml 
 */
public class TestDockerImageLookup {

    // load default configuration from ./resource/config_default.yaml
    private static GpConfig gpConfig;

    @BeforeClass
    public static void beforeClass() throws Throwable {
        final File configFile=new File("resources/config_default.yaml");
        gpConfig=ConfigUtil.initGpConfig(configFile);
        assertNotNull("sanity check after initializing gpConfig from file", gpConfig);
    }

    /** sanity check, 'job.docker.image' must not be declared in the config file */
    @Test
    public void check_dockerImage() {
        assertEquals("'job.docker.image' from config_default.yaml)",
            null,
            gpConfig.getValueFromConfig(Demo.serverContext, PROP_DOCKER_IMAGE)
        );
    }
    
    /** sanity check, 'job.docker.image.default' must be declared in the config file, with getValue */
    @Test public void check_dockerImageDefault() {
        assertEquals("get '"+PROP_DOCKER_IMAGE_DEFAULT+"'",
            new Value("genepattern/docker-java17:0.12"),
            gpConfig.getValue(Demo.serverContext, PROP_DOCKER_IMAGE_DEFAULT)
        );
    }
    
    @Test public void check_ABSOLUTE_v1_5() {
        assertDockerImageNoManifest("genepattern/docker-r-2-15:0.1", 
            gpConfig,
            "ABSOLUTE", 
            "urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00309:1.5"
        );
    }

    @Test public void check_ABSOLUTE_v1() {
        assertDockerImageNoManifest("genepattern/docker-r-2-15:0.1",
            gpConfig,
            "ABSOLUTE", 
            "urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00309:1"
        );
    }
    
    @Test public void check_DESeq2_v4_2() {
        assertDockerImageNoManifest("genepattern/docker-r-3-2:0.1", 
            gpConfig,
            "DESeq2", 
            "urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00362:0.4.2"
        );
    }

    @Test public void check_DESeq2_v4() {
        assertDockerImageNoManifest("genepattern/docker-r-3-2:0.1", 
            gpConfig,
            "DESeq2", 
            "urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00362:0.4"
        );
    }

    @Test
    public void check_HierarchicalClustering_v7_18_from_manifest() {
        final String name="HierarchicalClustering";
        final String lsid="urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00009:7.18";
        final String dockerImage="genepattern/docker-python36:0.6";
        assertDockerImageFromManifest(dockerImage, gpConfig, name, lsid);
    }

    @Test public void check_HierarchicalClustering_v7_17() {
        assertDockerImageNoManifest("genepattern/docker-python36:0.5",
            gpConfig,
            "HierarchicalClustering",
            "urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00009:7.17"
        );
    }

    @Test public void check_HierarchicalClustering_v6() {
        assertDockerImageNoManifest("genepattern/docker-java17:0.12",
            gpConfig,
            "HierarchicalClustering",
            "urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00009:6"
        );
    }

    @Test public void check_HierarchicalClusteringImage_v4() {
        assertDockerImageNoManifest("genepattern/docker-java17:0.12",
            gpConfig,
            "HierarchicalClusteringImage",
            "   urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00071:4"
        );
    }

    @Test public void check_ImputeMissingValuesKNN_v13() {
        assertDockerImageNoManifest("genepattern/docker-r-2-5:0.1",
            gpConfig,
            "ImputeMissingValues.KNN",
            "urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00054:13"
        );
    }

    /**
     * Test that job.docker.image from the module takes precence over built-in image lookup  
     * Note: this is for a hypothetical release, not an actual.
     */
    @Test public void example_ImputeMissingValuesKNN_v13_custom_from_manifest() {
        assertDockerImageFromManifest("genepattern/imputemissingvaluesknn:0.0.1", 
            gpConfig,
            "ImputeMissingValues.KNN", 
            "urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00054:13"
        );
    }

    /**
     * Test that job.docker.image from the module takes precence over built-in image lookup  
     * Note: this is for a hypothetical release, not an actual.
     */
    @Test public void example_ImputeMissingValuesKNN_v14_from_manifest() {
        assertDockerImageFromManifest("genepattern/imputemissingvaluesknn:0.1", 
            gpConfig,
            "ImputeMissingValues.KNN", 
            "urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00054:14"
        );
    }

    @Test public void check_KNN_v4() {
        assertDockerImageNoManifest("genepattern/docker-java17:0.12", 
            gpConfig,
            "KNN", 
            "urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00012:4"
        );
    }

    @Test public void check_NMFConsensus_v5() {
        assertDockerImageNoManifest("genepattern/docker-r-2-5:0.1", 
            gpConfig,
            "NMFConsensus", 
            "urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00057:5"
        );
    }
    
    @Test public void check_PreprocessDataset_v5_1() {
        assertDockerImageNoManifest("genepattern/docker-java17:0.12", 
            gpConfig,
            "PreprocessDataset", 
            "urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00020:5.1"
        );
    }

}
