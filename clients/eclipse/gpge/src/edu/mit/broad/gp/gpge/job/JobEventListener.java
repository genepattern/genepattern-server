/*
 * Created on Jul 21, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package edu.mit.broad.gp.gpge.job;
import org.genepattern.webservice.AnalysisJob;
import org.genepattern.webservice.JobInfo;

/**
 * @author genepattern
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public interface JobEventListener {

    public void jobFinished(AnalysisJob job);
    public void jobStatusChange(JobInfo jobInfo);
    
}
