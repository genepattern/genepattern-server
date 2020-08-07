package org.genepattern.drm;

import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;

/**
 * @deprecated see GpConfig getDockerImage
 * 
 * @author pcarr
 */
public class DockerImage {

    /** @deprecated call {@link GpConfig#getJobDockerImage(GpContext)} instead  */
    public static final String getJobDockerImage(final GpConfig gpConfig, final GpContext jobContext) {
        return gpConfig.getGPProperty(jobContext, org.genepattern.drm.JobRunner.PROP_DOCKER_IMAGE);
    }

}
