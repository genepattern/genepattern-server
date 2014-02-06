package org.genepattern.junitutil;

import java.util.HashMap;
import java.util.Map;

import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.job.JobInfoLoader;
import org.genepattern.webservice.JobInfo;
import org.junit.Ignore;

/**
 * For junit tests, load canned job results from a Map, rather than from a DB.
 * @author pcarr
 *
 */
@Ignore
public class JobInfoLoaderFromMap implements JobInfoLoader {
    private Map<String,JobInfo> lookup=new HashMap<String,JobInfo>();

    public JobInfoLoaderFromMap() {
    }
    
    @Override
    public JobInfo getJobInfo(final Context userContext, final String jobId) throws Exception {
        JobInfo jobInfo=lookup.get(jobId);
        return jobInfo;
    }

}
