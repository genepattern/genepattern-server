package org.genepattern.server.job;

import org.genepattern.server.dm.GpFilePath;
import org.genepattern.webservice.JobInfo;

/**
 * Create an instance of this class and pass it into a JobInfoWalker in order to visit all jobs in a DAG.
 * For example, to visit all jobs of a completed pipeline in order to generate a summary report.
 * 
 * Based on the Visitor pattern, and loosely inspired by the Java NIO FileVisitor interface.
 * Think of a JobInfo as a File and child jobs as a Directory.
 * 
 * @author pcarr
 *
 */
public interface JobInfoVisitor {
    
    /**
     * This method is called before visiting any child jobs of the given jobInfo.
     * If this method returns false it indicates to the calling method that it should not
     * visit the child jobs.
     * 
     * @param jobInfo
     * @return
     */
    boolean preVisitChildren(JobInfo jobInfo);
    
    /**
     * This method is called before visiting any output files of the given jobInfo.
     * If this method returns false it indicates to the calling method that it should not
     * visit the result files.
     * 
     * @param jobInfo
     * @return
     */
    boolean preVisitOutputFiles(JobInfo jobInfo);
    
    /**
     * Visit a particular jobInfo in the tree.
     * 
     * @param root, for a pipeline, include the root job. For a standard job, the root is the job info.
     * @param parent, for a step in a pipeline, include the parent job. If the jobInfo is not a child job parent will be null.
     * @param jobInfo, the job which is being visited.
     */
    void visitJobInfo(JobInfo root, JobInfo parent, JobInfo jobInfo);
    
    /**
     * Visit a particular output file of the job.
     * @param parent, the parent jobInfo.
     * @param outputFile, one of the parent jobs output files.
     */
    void visitOutputFile(JobInfo parent, GpFilePath outputFile);
    
    /**
     * This method is called after all child jobs of the given job have been visited.
     * @param jobInfo
     */
    void postVisitChildren(JobInfo jobInfo);

    /**
     * This method is called after all output files of the given job have been visited.
     * @param jobInfo
     */
    void postVisitOutputFiles(JobInfo jobInfo);
    
    /**
     * This is called if the walker has been cancelled before it finished visiting
     * all jobs and result files.
     * If necessary do cleanup or signal an interrupted exception here.
     */
    void cancelled();
}
