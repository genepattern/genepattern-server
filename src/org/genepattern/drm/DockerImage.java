package org.genepattern.drm;

import static org.genepattern.drm.JobRunner.PROP_DOCKER_IMAGE;
import static org.genepattern.drm.JobRunner.PROP_DOCKER_IMAGE_LOOKUP;

import java.util.Map;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.Value;
import org.genepattern.util.LSID;

import com.google.common.base.Strings;

/**
 * Helper class for initializing the 'job.docker.image' for a job based on the following conditions.
 * <ul>
 *   <li>the 'job.docker.image' in the manifest file</li>
 *   <li>the 'job.docker.image' in the config_yaml file</li>
 *   <li>the 'job.docker.image.default' in the config_yaml file</li>
 *   <li>the 'job.docker.image.lookup' in the config_yaml file</li>
 *   <li>the hard-coded value in this source file, if not set anywhere else</li>
 * </ul>
 * 
 * <h3>Default</h3>
 * <p>By default use the 'job.docker.image' declared in the manifest file.</p>
 * 
 * <h3>Special case: No image in manifest</h3>
 * <p>There are two ways to set the docker image when there is no value in the manifest.</p>
 * <ol>
 *   <li>use the 'job.docker.image.lookup' table</li>
 *   <li>set the 'job.docker.image.default' property</li>
 * </ol>
 * <p>When there is no value in the manifest, set the 'job.default.docker.image' in the manifest
 *    file.
 * </p>
 * <p>The 'job.docker.image.lookup' can be used for older GenePattern modules which do not set the
 * docker image in the manfest file. Example:
 * </p>
 * <pre>
    job.docker.image.lookup: {
      # {taskName_no_version} : {dockerImage}
      "ExampleLookup": "genepattern/docker-example:1-lookup-name-no-version",

      # {taskName:version}    : {dockerImage}
      "ExampleLookup:1" : "genepattern/docker-example:1",
      
      # ExampleLookup
      # {lsid}                : {dockerImage}
      "urn:lsid:example.com:example.module.analysis:00003:2": "genepattern/docker-example:2",

      # ExampleLookup    
      # {lsid_no_version}     : {dockerImage}
      "urn:lsid:example.com:example.module.analysis:00003": "genepattern/docker-example:3",
      
      # Example 
      "Example:3.1": "genepattern/docker-example:3.1-from-lookup"
    }
 * </pre>
 * 
 * <h3>Special case: Override the value from the manifest</h3>
 * <p>If you need to use a different value than that which is set in the manifest file, 
 *    set the 'job.docker.image' in the config_yaml file. This will override the value,
 *    if any, that is set in the manifest file.
 * </p>
 * 
 * <h3>Precedence rules</h3>
 * <p>The 'job.docker.image' param takes precedence over 'job.docker.image.default'
 *    which takes precedence over 'job.docker.image.lookup'.
 * </p>
 * <p>In all cases the normal precedence rules for the config_yaml file apply. For example
 *    a custom value per user will override the default value.
 * </p>
 * 
 * @author pcarr
 *
 */
public class DockerImage {
    private static final Logger log = Logger.getLogger(DockerImage.class);

    public static final String getJobDockerImage(final GpConfig gpConfig, final GpContext jobContext) {
        final String dockerImage=gpConfig.getGPProperty(jobContext, PROP_DOCKER_IMAGE);
        if (!Strings.isNullOrEmpty(dockerImage)) {
            return dockerImage;
        }
        log.warn("'"+PROP_DOCKER_IMAGE_LOOKUP+"' not set, trying '"+PROP_DOCKER_IMAGE_LOOKUP+"'");
        final Value lookup=gpConfig.getValue(jobContext, PROP_DOCKER_IMAGE_LOOKUP);
        if (lookup != null) {
            if (!lookup.isMap()) {
                log.error("ignoring '"+PROP_DOCKER_IMAGE_LOOKUP+"', expecting a value of type Map");
            }
            else {
                final Map<?,?> map=lookup.getMap();
                final String lsid=jobContext.getLsid();
                final String taskName=jobContext.getTaskName();
                String lsidNoVersion=null;
                String taskNameVersion=null;

                try {
                    LSID lsidObj=new LSID(lsid);
                    lsidNoVersion=lsidObj.toStringNoVersion();
                    if (lsidObj.hasVersion()) {
                        taskNameVersion=taskName+":"+lsidObj.getVersion();
                    }
                }
                catch (Throwable t) {
                    log.error(t);
                }
                if (map.containsKey(lsid)) {
                    return map.get(lsid).toString();
                }
                else if (taskNameVersion != null && map.containsKey(taskNameVersion)) {
                    return map.get(taskNameVersion).toString();
                    
                }
                else if (lsidNoVersion != null && map.containsKey(lsidNoVersion)) {
                    return map.get(lsidNoVersion).toString();
                }
                else if (map.containsKey(taskName)) {
                    return map.get(taskName).toString();
                } 
            }
        }
        final String dockerImageDefault=gpConfig.getGPProperty(jobContext, JobRunner.PROP_DOCKER_IMAGE_DEFAULT, "genepattern/docker-java17:0.12");
        return dockerImageDefault;
    }

}
