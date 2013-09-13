package org.genepattern.server.rest;

import org.genepattern.server.config.ServerConfiguration;

/**
 * Factory method for initializing an instance of the JobInputApi.
 * 
 * @author pcarr
 */
public class JobInputApiFactory {
    static public JobInputApi createJobInputApi(ServerConfiguration.Context context) {
        return new JobInputApiImpl();
    }
    
    static public JobInputApi createJobInputApi(ServerConfiguration.Context context, boolean initDefault) {
        return new JobInputApiImpl(initDefault);
    }
}
