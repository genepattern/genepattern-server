package org.genepattern.server.job;



/**
 * Interface for traversing all jobs in a DAG, for example from the root job for a completed pipeline.
 * 
 * @author pcarr
 *
 */
public interface JobInfoWalker {
    void walk(JobInfoVisitor v);
    void cancel();
}
