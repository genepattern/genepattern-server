/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.executor.serverthread;

import java.util.concurrent.Callable;

/**
 * A callable task, to run as a job by the ServerThreadExecutor.
 * 
 * Added to GP for the GP 3.4.0-b2 release for the PathSeq pipeline.
 * The pipeline has a series of for loops, which must be dynamically configured
 * at runtime. Rather than hard-code this behavior into the Pipeline Executor,
 * I created a new command executor, ServerThreadExecutor, which allows
 * control modules to be added to a pipeline.
 * 
 * A control module is a job which runs in the server runtime, and thus makes it possible
 * to do things like add a job to a pipeline at runtime.
 * 
 * To use this interface:
 * 1) create a module
 * 2) the first arg of the module must be a fully qualified classname to a class which implements this interface.
 * 3) [initial implementation] the class must be part of the core of GenePattern
 * 4) edit the config file, make sure the ServerThreadExecutor is defined, e.g.
 *     ServerThread:
        classname: org.genepattern.server.executor.serverthread.ServerThreadExecutor
 * 5) edit the config file, so that your newly created module uses the ServerThread executor, e.g.
 *     module.properties:
 *         myControlModule: 
 *             executor: ServerThread
 * 
 * @author pcarr
 */
public interface ServerTask extends Callable<String> {
    void setUserId(String userId);
    void setJobId(int jobId);
    void setArgs(String[] args);
}
