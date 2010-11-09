package org.genepattern.server.executor.pipeline;

/**
 * Exception related to executing a pipeline or a job in a pipeline.
 * @author pcarr
 */
public class PipelineException extends Exception {
    public PipelineException(String message, Exception e) {
        super(message, e);
    }
    
    public PipelineException(String message, Throwable t) {
        super(message, t);
    }
}
