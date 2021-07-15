/*******************************************************************************
 * Copyright (c) 2003-2021 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.executor.pipeline;

/**
 * Exception related to executing a pipeline or a job in a pipeline.
 * @author pcarr
 */
public class PipelineException extends Exception {
    public PipelineException(String message) {
        super(message);
    }

    public PipelineException(String message, Exception e) {
        super(message, e);
    }
    
    public PipelineException(String message, Throwable t) {
        super(message, t);
    }
}
